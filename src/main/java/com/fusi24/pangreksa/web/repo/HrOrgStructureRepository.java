package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrOrgStructureRepository extends JpaRepository<HrOrgStructure, Long> {
    // Find by referenceId
    //List<HrOrgStructure> findByReferenceId(Long referenceId);
    @EntityGraph(attributePaths = {"company", "parent"})
    List<HrOrgStructure> findByCompany(HrCompany company);
}
