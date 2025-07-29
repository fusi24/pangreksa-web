package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonAddressRepository extends JpaRepository<HrPersonAddress, Long> {
    // Find by referenceId
    List<HrPersonAddress> findByPerson(HrPerson person);
}
