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

    public boolean hasUnfinishedAttendanceBeforeToday() {
        LocalDate today = LocalDate.now(JAKARTA_ZONE);

        return attendanceRepo.findByAppUserIdAndAttendanceDateBetween(
                currentUser.getId(),
                today.minusDays(30), // safe window
                today.minusDays(1)
        ).stream().anyMatch(att ->
                att.getCheckIn() != null && att.getCheckOut() == null
        );
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

        if (!isWorkingDay(today)) return false;

        HrPerson employee = currentUser.getPerson();

        if (isOnApprovedLeave(today, employee)) return false;

        Optional<HrAttendance> todayAttendance =
                attendanceRepo.findByAppUserIdAndAttendanceDate(
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

        HrWorkSchedule schedule = att.getWorkSchedule();
        boolean overtimeAuto = Boolean.TRUE.equals(schedule.getIsOvertimeAuto());

        // ===============================
        // 1️⃣ BELUM CHECKOUT
        // ===============================
        if (att.getCheckOut() == null) {

            LocalDateTime scheduledOut = LocalDateTime.of(
                    att.getAttendanceDate(),
                    schedule.getCheckOut()
            );

            LocalDateTime batasLupa = scheduledOut.plusHours(1);
            LocalDateTime now = LocalDateTime.now(JAKARTA_ZONE);

            if (now.isAfter(batasLupa)) {
                att.setStatus("LUPA_CLOCK_OUT");
            }
            return;
        }

        // ===============================
        // 2️⃣ CHECKOUT SUDAH LEWAT HARI
        // ===============================
        if (att.getCheckOut().toLocalDate().isAfter(att.getAttendanceDate())) {
            att.setStatus("LUPA_CLOCK_OUT");
            return;
        }

        // ===============================
        // 3️⃣ NORMAL CHECKOUT (HARI YANG SAMA)
        // ===============================
        ZonedDateTime actualIn = att.getCheckIn().atZone(JAKARTA_ZONE);
        ZonedDateTime actualOut = att.getCheckOut().atZone(JAKARTA_ZONE);

        LocalDateTime scheduledIn = LocalDateTime.of(
                att.getAttendanceDate(),
                schedule.getCheckIn()
        );

        LocalDateTime scheduledOut = LocalDateTime.of(
                att.getAttendanceDate(),
                schedule.getCheckOut()
        );

        ZonedDateTime scheduledInZ = scheduledIn.atZone(JAKARTA_ZONE);
        ZonedDateTime scheduledOutZ = scheduledOut.atZone(JAKARTA_ZONE);

        String status = "HADIR";

        if (actualIn.isAfter(scheduledInZ.plusMinutes(15))) {
            status = "TERLAMBAT";
        }

        if (actualOut.isBefore(scheduledOutZ.minusMinutes(30))) {
            status = "PULANG_CEPAT";
        }
        else if (overtimeAuto && actualOut.isAfter(scheduledOutZ.plusHours(1))) {
            status = "OVERTIME";
        }

        att.setStatus(status);
    }


}