package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrWorkScheduleAssignment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface HrWorkScheduleAssignmentRepository extends CrudRepository<HrWorkScheduleAssignment, Long>, JpaSpecificationExecutor<HrWorkScheduleAssignment> {

    Optional<HrWorkScheduleAssignment> findFirstByOrgStructureId(Long orgStructureId);
}