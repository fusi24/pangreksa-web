package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrSalaryBaseLevelRepository extends JpaRepository<HrSalaryBaseLevel, Long> {
    // Find all salary levels by level code
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByLevelCodeAndCompany(String levelCode, HrCompany company);

    // Optionally: find active salary level by code and date
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByLevelCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndCompany(
            String levelCode, LocalDate startDate, LocalDate endDate, HrCompany company
    );

    //Find all salary that end date is null order by level code asc
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByCompanyAndEndDateIsNullOrderByLevelCodeAsc(HrCompany company);
    //find all salary  order by level code asc
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByCompanyOrderByLevelCodeAsc(HrCompany company);
}
