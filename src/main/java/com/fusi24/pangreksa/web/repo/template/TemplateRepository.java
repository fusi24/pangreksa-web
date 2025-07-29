package com.fusi24.pangreksa.web.repo.template;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<HrPerson, Long> {
    // Find by referenceId
    //List<HrPerson> findByReferenceId(Long referenceId);
}
