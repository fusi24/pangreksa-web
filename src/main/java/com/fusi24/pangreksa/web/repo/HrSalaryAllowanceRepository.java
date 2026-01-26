package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrSalaryAllowanceRepository extends JpaRepository<HrSalaryAllowance, Long> {
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryAllowance> findByNameAndCompany(String name, HrCompany company);
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryAllowance> findByNameAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndCompany(
            String name, LocalDate startDate, LocalDate endDate, HrCompany company
    );
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryAllowance> findByCompanyAndEndDateIsNullOrderByNameAsc(HrCompany company);
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryAllowance> findByCompanyOrderByNameAsc(HrCompany company);

    @EntityGraph(attributePaths = {"company"})
    @Query("""
    SELECT a
    FROM HrSalaryAllowance a
    WHERE a.isAttendanceBased = true
      AND a.startDate <= :date
      AND (a.endDate IS NULL OR a.endDate >= :date)
""")
    List<HrSalaryAllowance> findActiveAttendanceBasedAllowances(@Param("date") LocalDate date);

}
