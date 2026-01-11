package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPayroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface HrPayrollRepository extends CrudRepository<HrPayroll, Long>, JpaSpecificationExecutor<HrPayroll> {

    @Override
    @EntityGraph(attributePaths = {"calculation"})
    Page<HrPayroll> findAll(Specification<HrPayroll> spec, Pageable pageable);

    boolean existsByPersonIdAndPayrollDate(Long personId, LocalDate payrollDate);
}
