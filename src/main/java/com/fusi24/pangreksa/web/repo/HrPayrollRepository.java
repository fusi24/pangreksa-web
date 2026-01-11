package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPayroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HrPayrollRepository extends CrudRepository<HrPayroll, Long>, JpaSpecificationExecutor<HrPayroll> {

    @Override
    @EntityGraph(attributePaths = {"calculation"})
    Page<HrPayroll> findAll(Specification<HrPayroll> spec, Pageable pageable);

    boolean existsByPersonIdAndPayrollDate(Long personId, LocalDate payrollDate);

    @Modifying
    @Query("DELETE FROM HrPayroll p WHERE p.id IN :ids")
    void deleteByIds(@Param("ids") List<Long> ids);

}
