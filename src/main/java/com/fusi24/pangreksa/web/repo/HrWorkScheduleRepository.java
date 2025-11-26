package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrWorkSchedule;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface HrWorkScheduleRepository extends CrudRepository<HrWorkSchedule, Long>, JpaSpecificationExecutor<HrWorkSchedule> {

    List<HrWorkSchedule> findByStatus(String status);

    @Query("SELECT ws FROM HrWorkSchedule ws WHERE ws.status = :status ORDER BY ws.effectiveDate DESC")
    List<HrWorkSchedule> findByStatusOrderByEffectiveDateDesc(@Param("status") String status);

    // Optional: Find by assignment type
    @Query("SELECT DISTINCT ws FROM HrWorkSchedule ws JOIN ws.assignments a WHERE a.assignmentType = :assignmentType")
    List<HrWorkSchedule> findByAssignmentType(@Param("assignmentType") String assignmentType);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT ws FROM HrWorkSchedule ws " +
            "LEFT JOIN FETCH ws.assignments a " +
            "LEFT JOIN FETCH a.orgStructure " +
            "ORDER BY ws.name")
    List<HrWorkSchedule> findAllWithAssociations();
}