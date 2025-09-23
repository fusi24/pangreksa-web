package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonContactRepository extends JpaRepository<HrPersonContact, Long> {
    // Find by referenceId
    List<HrPersonContact> findByPerson(HrPerson person);
}
