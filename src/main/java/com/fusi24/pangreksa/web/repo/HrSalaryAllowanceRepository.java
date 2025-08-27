package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
