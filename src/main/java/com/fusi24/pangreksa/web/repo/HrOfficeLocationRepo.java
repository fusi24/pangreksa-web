package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrOfficeLocation;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface HrOfficeLocationRepo extends CrudRepository<HrOfficeLocation, Long>, JpaSpecificationExecutor<HrOfficeLocation> {

}
