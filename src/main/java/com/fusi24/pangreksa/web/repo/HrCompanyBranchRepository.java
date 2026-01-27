package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrCompanyBranchRepository extends JpaRepository<HrCompanyBranch, Long> {

    @EntityGraph(attributePaths = {"company"})
    List<HrCompanyBranch> findAll();

    @EntityGraph(attributePaths = {"company"})
    List<HrCompanyBranch> findByCompanyId(Long companyId);

    boolean existsByCompanyIdAndBranchCodeIgnoreCase(Long companyId, String branchCode);

    List<HrCompanyBranch> findByCompanyOrderByBranchNameAsc(HrCompany company);

}
