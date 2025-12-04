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
        return today.isEmpty() || today.get().getCheckOut() == null;
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
        HrWorkSchedule sched = att.getWorkSchedule();
        if (att.getCheckIn() == null) {
            att.setStatus("ALPHA");
            return;
        }

        LocalTime checkInTime = att.getCheckIn().toLocalTime();
        LocalTime scheduledIn = sched.getCheckIn(); // assuming this field exists

        if (checkInTime.isAfter(scheduledIn.plusMinutes(15))) {
            att.setStatus("TERLAMBAT");
        } else {
            att.setStatus("HADIR");
        }

        if (att.getCheckOut() != null) {
            LocalTime checkOutTime = att.getCheckOut().toLocalTime();
            LocalTime scheduledOut = sched.getCheckOut();

            if (checkOutTime.isBefore(scheduledOut.minusMinutes(30))) {
                att.setStatus("PULANG_CEPAT");
            } else if (checkOutTime.isAfter(scheduledOut.plusHours(1))) {
                att.setStatus("OVERTIME");
            }
        }
    }
}