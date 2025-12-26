package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrAttendanceRepository;
import com.fusi24.pangreksa.web.repo.HrLeaveApplicationRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private HrAttendanceRepository attendanceRepo;

    @Autowired
    private HrWorkScheduleService workScheduleService; // You'll implement this

    @Autowired
    private CalendarService calendarService; // For holidays

    @Autowired
    private FwAppUserRepository appUserRepository;

    @Autowired
    private HrLeaveApplicationRepository hrLeaveApplicationRepository;

    @Getter
    private FwAppUser currentUser;

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    public void setUser(AppUserInfo user) {
        this.currentUser = findAppUserByUserId(user.getUserId().toString());
    }

    private FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public HrAttendance getOrCreateTodayAttendance(FwAppUser user) {
        LocalDate today = LocalDate.now();
        return attendanceRepo.findByAppUserIdAndAttendanceDate(user.getId(), today)
                .orElseGet(() -> {
                    HrAttendance att = new HrAttendance();
                    att.setAppUser(user);
                    att.setPerson(user.getPerson()); // assuming AppUserInfo has getPerson()
                    HrWorkSchedule schedule = workScheduleService.getActiveScheduleForUser(user, today);
                    if (schedule == null) {
                        return null;
                    }
                    att.setWorkSchedule(schedule);
                    att.setAttendanceDate(today);
                    att.setStatus("ALPHA"); // temporary
                    return att;
                });
    }

    public boolean shouldShowCheckInPopup() {
        LocalDate today = LocalDate.now(JAKARTA_ZONE);

        if (!isWorkingDay(today)) {
            return false;
        }

        // Get current user's person
        HrPerson employee = currentUser.getPerson(); // adjust based on your model

        // If on approved leave today → skip check-in
        if (isOnApprovedLeave(today, employee)) {
            return false;
        }

        Optional<HrAttendance> todayAttendance = attendanceRepo.findByAppUserIdAndAttendanceDate(
                currentUser.getId(), today
        );
        return todayAttendance.isEmpty();
    }

    public boolean shouldShowCheckOutPopup() {
        LocalDate today = LocalDate.now(JAKARTA_ZONE);

        // Must be a working day (i.e., user has an active schedule for today)
        if (!isWorkingDay(today)) {
            return false;
        }

        // Get current user's person
        HrPerson employee = currentUser.getPerson(); // adjust based on your model

        // If on approved leave today → skip check-in
        if (isOnApprovedLeave(today, employee)) {
            return false;
        }

        // Fetch today's attendance record
        Optional<HrAttendance> todayAttendance = attendanceRepo.findByAppUserIdAndAttendanceDate(
                currentUser.getId(), today
        );

        // Must have checked in
        if (todayAttendance.isEmpty() || todayAttendance.get().getCheckIn() == null) {
            return false;
        }

        HrAttendance attendance = todayAttendance.get();

        // Already checked out → no popup
        if (attendance.getCheckOut() != null) {
            return false;
        }

        // Get the work schedule linked to this attendance
        HrWorkSchedule schedule = attendance.getWorkSchedule();
        if (schedule == null) {
            return false; // Safety check
        }

        LocalTime scheduledCheckOut = schedule.getCheckOut(); // e.g., 17:00
        LocalTime now = LocalTime.now();

        // Optional: Add a 15-minute grace period before scheduled checkout
        // So popup appears starting 15 mins before official end time
        LocalTime earliestPopupTime = scheduledCheckOut.minusMinutes(15);

        // Show popup if current time >= (scheduled checkout - grace)
        return !now.isBefore(earliestPopupTime);
    }

    private boolean isWorkingDay(LocalDate date) {
        if (calendarService.isHoliday(date)) return false;
        if (date.getDayOfWeek().getValue() > 5) return false; // Sat=6, Sun=7
        return workScheduleService.hasActiveScheduleForUser(currentUser, date);
    }

    private boolean isOnApprovedLeave(LocalDate date, HrPerson employee) {
        List<LeaveStatusEnum> approvedStatuses = List.of(LeaveStatusEnum.APPROVED);
        // You might also include "PENDING" if you want to block check-in during pending leave
        // But typically, only approved leaves count.

        return hrLeaveApplicationRepository.existsByEmployeeAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                employee, date, date, approvedStatuses
        );
    }

    public HrAttendance saveAttendance(HrAttendance att, AppUserInfo modifier) {
        // Auto-set status based on check-in/out vs schedule
        setStatusBasedOnSchedule(att);
        return attendanceRepo.save(att);
    }

    public void deleteAttendance(HrAttendance attendance) {
        attendanceRepo.delete(attendance);
    }


    // ✅ Fixed: Accepts start/end and filters by attendanceDate
    public Page<HrAttendance> getAttendancePage(Pageable pageable, LocalDate start, LocalDate end, String searchTerm, HrCompany company, HrOrgStructure orgStructure, HrPerson emp) {
        if (currentUser == null) {
            throw new IllegalStateException("App user is not set. Please call setUser() before using this method.");
        }

        Specification<HrAttendance> spec = buildFilterSpec(start, end, searchTerm, company, orgStructure, emp);
        return attendanceRepo.findAll(spec, pageable);
    }

    public long countAttendance(LocalDate start, LocalDate end, String searchTerm, HrCompany company, HrOrgStructure orgStructure, HrPerson emp) {
        Specification<HrAttendance> spec = buildFilterSpec(start, end, searchTerm, company, orgStructure, emp);
        return attendanceRepo.count(spec);
    }

    // ✅ Fixed: Now uses start/end and adds company/department filters
    private Specification<HrAttendance> buildFilterSpec(LocalDate start, LocalDate end, String searchTerm, HrCompany company, HrOrgStructure orgStructure, HrPerson emp) {
        Specification<HrAttendance> spec = buildBaseSearchSpec(searchTerm);

        // Filter by date range
        if (start != null || end != null) {
            spec = spec.and((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (start != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), start));
                }
                if (end != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), end));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            });
        }

        // Filter by company (via person -> employee -> department -> company)
        if (company != null) {
            spec = spec.and((root, query, cb) -> {
                Join<HrAttendance, HrPerson> personJoin = root.join("person");
                Join<HrPerson, HrPersonPosition> personPositionJoin = personJoin.join("personPositionJoin");
                return cb.equal(personPositionJoin.get("company"), company);
            });
        }

        // Filter by department
        if (orgStructure != null) {
            spec = spec.and((root, query, cb) -> {
                Join<HrAttendance, HrPerson> personJoin = root.join("person");
                Join<HrPerson, HrPersonPosition> personPositionJoin = personJoin.join("personPositionJoin");
                Join<HrPersonPosition, HrPosition> positionJoin = personPositionJoin.join("position");
                return cb.equal(positionJoin.get("orgStructure"), orgStructure);
            });
        }

        if(emp != null) {
            spec = spec.and((root, query, cb) -> {
                Join<HrAttendance, HrPerson> personJoin = root.join("person");
                return cb.equal(root.get("person"), emp);
            });
        }

        return spec;
    }

    private Specification<HrAttendance> buildBaseSearchSpec(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        String lowerCaseSearchTerm = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<HrAttendance, HrPerson> personJoin = root.join("person");
            return cb.or(
                    cb.like(cb.lower(personJoin.get("firstName")), lowerCaseSearchTerm),
                    cb.like(cb.lower(personJoin.get("lastName")), lowerCaseSearchTerm)
            );
        };
    }

    private void setStatusBasedOnSchedule(HrAttendance att) {
        if (att.getCheckIn() == null) {
            att.setStatus("ALPHA");
            return;
        }

        if (att.getCheckOut() == null) {
            // Still working – don't set final status
            return;
        }

        // Convert to Jakarta time
        ZonedDateTime actualInJakarta = att.getCheckIn().atZone(JAKARTA_ZONE);
        ZonedDateTime actualOutJakarta = att.getCheckOut().atZone(JAKARTA_ZONE);

        LocalDate checkInDate = actualInJakarta.toLocalDate();

        // Assume work schedule is defined for a standard day (e.g., 08:00–17:00)
        LocalTime scheduledInTime = att.getWorkSchedule().getCheckIn();   // e.g., 08:00
        LocalTime scheduledOutTime = att.getWorkSchedule().getCheckOut(); // e.g., 17:00

        // Build scheduled window on the check-in day
        LocalDateTime scheduledIn = LocalDateTime.of(checkInDate, scheduledInTime);
        LocalDateTime scheduledOut = LocalDateTime.of(checkInDate, scheduledOutTime);

        // Allow checkout up to (e.g.) 12 hours into the next day for night shifts
        // You can adjust this logic based on your policy
        if (actualOutJakarta.toLocalDate().isAfter(checkInDate)) {
            // If checked out next day, shift scheduledOut to next day too
            // But only if actualOut is after scheduledOut (i.e., didn't finish early)
            // Alternative: define max allowed checkout (e.g., +1 day)
            scheduledOut = scheduledOut.plusDays(1);
        }

        // Now compare full LocalDateTime (via ZonedDateTime for safety)
        ZonedDateTime scheduledInZoned = scheduledIn.atZone(JAKARTA_ZONE);
        ZonedDateTime scheduledOutZoned = scheduledOut.atZone(JAKARTA_ZONE);

        String status = "HADIR";

        // Late arrival: after scheduledIn + 15 mins
        if (actualInJakarta.isAfter(scheduledInZoned.plusMinutes(15))) {
            status = "TERLAMBAT";
        }

        // Early departure: before scheduledOut - 30 mins
        if (actualOutJakarta.isBefore(scheduledOutZoned.minusMinutes(30))) {
            status = "PULANG_CEPAT";
        }
        // Overtime: after scheduledOut + 1 hour (and not early)
        else if (actualOutJakarta.isAfter(scheduledOutZoned.plusHours(1))) {
            status = "OVERTIME";
        }

        att.setStatus(status);
    }
}