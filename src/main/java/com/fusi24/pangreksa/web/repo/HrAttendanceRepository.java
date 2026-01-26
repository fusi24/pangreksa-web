package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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


    @EntityGraph(attributePaths = {"person", "workSchedule"})
    Page<HrAttendance> findAll(Specification<HrAttendance> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"workSchedule"})
    @Query("""
        SELECT a
        FROM HrAttendance a
        WHERE a.person.id = :personId
          AND a.attendanceDate >= :startDate
          AND a.attendanceDate < :endDate
          AND a.status = :status
    """)
    List<HrAttendance> findOvertimeAttendances(
            @Param("personId") Long personId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status
    );

    @Query("""
        SELECT a
        FROM HrAttendance a
        WHERE a.person.id = :personId
          AND a.attendanceDate >= :startDate
          AND a.attendanceDate < :endDate
          AND a.status = 'OVERTIME'
    """)
    @EntityGraph(attributePaths = {"workSchedule"})
    List<HrAttendance> findOvertimeByPersonAndPeriod(
            @Param("personId") Long personId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @EntityGraph(attributePaths = {"person", "workSchedule"})
    @Query("""
    SELECT a
    FROM HrAttendance a
    WHERE a.attendanceDate = :date
      AND a.checkIn IS NOT NULL
      AND a.checkOut IS NULL
""")
    List<HrAttendance> findIncompleteAttendanceByDate(@Param("date") LocalDate date);


    @Query("""
        select count(a)
        from HrAttendance a
        where a.person.id = :personId
          and a.attendanceDate >= :startOfMonth
          and a.attendanceDate < :startOfNextMonth
          and a.status != 'ALPHA'
    """)
    long countAttendanceByPersonAndPeriod(@Param("personId") Long personId,
                                          @Param("startOfMonth") LocalDate startOfMonth,
                                          @Param("startOfNextMonth") LocalDate startOfNextMonth);

}