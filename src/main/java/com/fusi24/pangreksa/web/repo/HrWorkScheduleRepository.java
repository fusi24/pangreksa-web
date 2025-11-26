package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrWorkSchedule;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface HrWorkScheduleRepository extends CrudRepository<HrWorkSchedule, Long>, JpaSpecificationExecutor<HrWorkSchedule> {

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT ws FROM HrWorkSchedule ws " +
            "LEFT JOIN FETCH ws.assignments a " +
            "LEFT JOIN FETCH a.orgStructure " +
            "ORDER BY ws.name")
    List<HrWorkSchedule> findAllWithAssociations();
}