package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface HrOrgStructureRepository extends CrudRepository<HrOrgStructure, Long>, JpaSpecificationExecutor<HrOrgStructure> {
    // Find by referenceId
    //List<HrOrgStructure> findByReferenceId(Long referenceId);
    @EntityGraph(attributePaths = {"company", "parent"})
    List<HrOrgStructure> findByCompany(HrCompany company);

    @EntityGraph(attributePaths = {"company", "parent"})
    @Query("SELECT o FROM HrOrgStructure o")
    List<HrOrgStructure> findAllWithAssociations();

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNameAndCompanyIdAndIdNot(String name, Long companyId, Long id);

    List<HrOrgStructure> findByParentId(Long parentId);
}
