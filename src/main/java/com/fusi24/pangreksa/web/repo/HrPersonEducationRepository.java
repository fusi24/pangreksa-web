package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonEducationRepository extends JpaRepository<HrPersonEducation, Long> {
    // Find by referenceId
    List<HrPersonEducation> findByPerson(HrPerson person);
}
