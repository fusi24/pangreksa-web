package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrAttendanceViolation;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HrAttendanceViolationRepository
        extends JpaRepository<HrAttendanceViolation, Long> {

    boolean existsByEmployeeAndAttendanceDateAndViolationType(
            HrPerson employee,
            LocalDate attendanceDate,
            String violationType
    );
}
