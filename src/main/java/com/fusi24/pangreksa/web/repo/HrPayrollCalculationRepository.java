package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPayrollCalculation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HrPayrollCalculationRepository extends CrudRepository<HrPayrollCalculation, Long> {

    public HrPayrollCalculation findFirstByPayrollInputId(Long payrollId);

    @Modifying
    @Query("DELETE FROM HrPayrollCalculation c WHERE c.payrollInput.id IN :payrollIds")
    void deleteByPayrollIds(@Param("payrollIds") List<Long> payrollIds);
}
