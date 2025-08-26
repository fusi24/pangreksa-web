package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrSalaryAllowanceRepository extends JpaRepository<HrSalaryAllowance, Long> {
    List<HrSalaryAllowance> findByName(String name);
    List<HrSalaryAllowance> findByNameAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String name, LocalDate startDate, LocalDate endDate
    );
    List<HrSalaryAllowance> findByEndDateIsNullOrderByNameAsc();
    List<HrSalaryAllowance> findAllByOrderByNameAsc();
}
