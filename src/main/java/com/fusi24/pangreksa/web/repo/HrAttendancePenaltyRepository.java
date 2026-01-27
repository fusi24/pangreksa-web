package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrAttendancePenalty;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrAttendancePenaltyRepository
        extends JpaRepository<HrAttendancePenalty, Long> {

    boolean existsByEmployeeAndReference_Id(HrPerson employee, Long referenceId);
}
