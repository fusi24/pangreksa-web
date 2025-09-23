package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPayrollCalculation;
import org.springframework.data.repository.CrudRepository;

public interface HrPayrollCalculationRepository extends CrudRepository<HrPayrollCalculation, Long> {

    public HrPayrollCalculation findFirstByPayrollInputId(Long payrollId);

}
