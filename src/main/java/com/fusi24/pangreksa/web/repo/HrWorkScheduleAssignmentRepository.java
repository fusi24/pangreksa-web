package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrWorkScheduleAssignment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HrWorkScheduleAssignmentRepository extends CrudRepository<HrWorkScheduleAssignment, Long>, JpaSpecificationExecutor<HrWorkScheduleAssignment> {

    Optional<HrWorkScheduleAssignment>
    findFirstByOrgStructureIdAndSchedule_EffectiveDateLessThanEqualAndSchedule_IsActiveTrue(
            Long orgStructureId,
            LocalDate date
    );
}