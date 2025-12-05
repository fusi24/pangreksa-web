package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrAttendanceRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
        if (!isWorkingDay(LocalDate.now())) {
            return false;
        }
        Optional<HrAttendance> today = attendanceRepo.findByAppUserIdAndAttendanceDate(
                currentUser.getId(), LocalDate.now()
        );
        return today.isEmpty();
    }

    public boolean shouldShowCheckOutPopup() {
        LocalDate today = LocalDate.now();

        // Must be a working day (i.e., user has an active schedule for today)
        if (!isWorkingDay(today)) {
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

    public HrAttendance saveAttendance(HrAttendance att, AppUserInfo modifier) {
        // Auto-set status based on check-in/out vs schedule
        setStatusBasedOnSchedule(att);
        return attendanceRepo.save(att);
    }

    public void deleteAttendance(HrAttendance attendance) {
        attendanceRepo.delete(attendance);
    }


    // ✅ Fixed: Accepts start/end and filters by attendanceDate
    public Page<HrAttendance> getAttendancePage(Pageable pageable, LocalDate start, LocalDate end, String searchTerm, HrCompany company, HrOrgStructure orgStructure) {
        if (currentUser == null) {
            throw new IllegalStateException("App user is not set. Please call setUser() before using this method.");
        }

        Specification<HrAttendance> spec = buildFilterSpec(start, end, searchTerm, company, orgStructure);
        return attendanceRepo.findAll(spec, pageable);
    }

    public long countAttendance(LocalDate start, LocalDate end, String searchTerm, HrCompany company, HrOrgStructure orgStructure) {
        Specification<HrAttendance> spec = buildFilterSpec(start, end, searchTerm, company, orgStructure);
        return attendanceRepo.count(spec);
    }

    // ✅ Fixed: Now uses start/end and adds company/department filters
    private Specification<HrAttendance> buildFilterSpec(LocalDate start, LocalDate end, String searchTerm, HrCompany company, HrOrgStructure orgStructure) {
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
        // No check-in → ALPHA (absent)
        if (att.getCheckIn() == null) {
            att.setStatus("ALPHA");
            return;
        }

        // Defer final status until check-out is recorded
        if (att.getCheckOut() == null) {
            // Optional: set a temporary status like "MASUK" or leave unchanged
            // For your requirement, we do nothing or reset to neutral
            // Example: att.setStatus(null); // or "MASUK"
            return; // DO NOT set final status yet
        }

        // Both check-in and check-out exist → evaluate full attendance

        // Convert UTC instants to Jakarta local times
        LocalTime actualIn = att.getCheckIn().atZone(JAKARTA_ZONE).toLocalTime();
        LocalTime actualOut = att.getCheckOut().atZone(JAKARTA_ZONE).toLocalTime();

        LocalTime scheduledIn = att.getWorkSchedule().getCheckIn();   // Jakarta time
        LocalTime scheduledOut = att.getWorkSchedule().getCheckOut(); // Jakarta time

        // Default: assume on time
        String status = "HADIR";

        // Check for late arrival
        if (actualIn.isAfter(scheduledIn.plusMinutes(15))) {
            status = "TERLAMBAT";
        }

        // Check for early departure
        if (actualOut.isBefore(scheduledOut.minusMinutes(30))) {
            status = "PULANG_CEPAT";
        }
        // Check for overtime (only if not already early leave)
        else if (actualOut.isAfter(scheduledOut.plusHours(1))) {
            status = "OVERTIME";
        }

        att.setStatus(status);
    }
}