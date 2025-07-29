package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonDocumentRepository extends JpaRepository<HrPersonDocument, Long> {
    // Find by referenceId
    List<HrPersonDocument> findByPerson(HrPerson person);
}
