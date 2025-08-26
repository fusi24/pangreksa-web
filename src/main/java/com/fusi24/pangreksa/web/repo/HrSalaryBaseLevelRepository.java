package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrSalaryBaseLevelRepository extends JpaRepository<HrSalaryBaseLevel, Long> {
    // Find all salary levels by level code
    List<HrSalaryBaseLevel> findByLevelCode(String levelCode);

    // Optionally: find active salary level by code and date
    List<HrSalaryBaseLevel> findByLevelCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String levelCode, LocalDate startDate, LocalDate endDate
    );

    //Find all salary that end date is null order by level code asc
    List<HrSalaryBaseLevel> findByEndDateIsNullOrderByLevelCodeAsc();
    //find all salary  order by level code asc
    List<HrSalaryBaseLevel> findAllByOrderByLevelCodeAsc();
}
