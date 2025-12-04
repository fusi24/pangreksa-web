package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrWorkSchedule;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface HrWorkScheduleRepository extends CrudRepository<HrWorkSchedule, Long>, JpaSpecificationExecutor<HrWorkSchedule> {

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT ws FROM HrWorkSchedule ws " +
            "LEFT JOIN FETCH ws.assignments a " +
            "LEFT JOIN FETCH a.orgStructure " +
            "ORDER BY ws.name")
    List<HrWorkSchedule> findAllWithAssociations();

    Optional<HrWorkSchedule> findFirstByAssignmentScope(String scope);
}