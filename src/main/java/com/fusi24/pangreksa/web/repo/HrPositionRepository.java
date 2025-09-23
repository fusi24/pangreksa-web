package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import com.fusi24.pangreksa.web.model.entity.HrPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPositionRepository extends JpaRepository<HrPosition, Long> {
    // Find by referenceId
    //List<HrPosition> findByReferenceId(Long referenceId);
    @EntityGraph(attributePaths = {"company","orgStructure"})
    List<HrPosition> findByCompanyAndOrgStructure(HrCompany company, HrOrgStructure orgStructure);
}
