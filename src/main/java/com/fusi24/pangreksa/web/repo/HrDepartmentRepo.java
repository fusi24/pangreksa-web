package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrDepartment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface HrDepartmentRepo extends CrudRepository<HrDepartment, Long>, JpaSpecificationExecutor<HrDepartment> {
}
