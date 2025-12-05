package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrAttendanceRepository extends CrudRepository<HrAttendance, Long>, JpaSpecificationExecutor<HrAttendance> {
    @EntityGraph(attributePaths = {"person", "workSchedule"})
    Optional<HrAttendance> findByAppUserIdAndAttendanceDate(Long appUserId, LocalDate date);

    @EntityGraph(attributePaths = "person")
    List<HrAttendance> findByAppUserIdAndAttendanceDateBetween(Long appUserId, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = "person")
    List<HrAttendance> findByAttendanceDate(LocalDate date);

    @EntityGraph(attributePaths = "person")
    List<HrAttendance> findByStatus(String status);


    @EntityGraph(attributePaths = "person")
    Page<HrAttendance> findAll(Specification<HrAttendance> spec, Pageable pageable);
}