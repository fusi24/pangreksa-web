package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import com.fusi24.pangreksa.web.repo.HrCompanyBranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyBranchService {

    private final HrCompanyBranchRepository branchRepo;

    public CompanyBranchService(HrCompanyBranchRepository branchRepo) {
        this.branchRepo = branchRepo;
    }

    public List<HrCompanyBranch> findAll() {
        return branchRepo.findAll();
    }

    public List<HrCompanyBranch> findByCompany(HrCompany company) {
        if (company == null || company.getId() == null) return List.of();
        return branchRepo.findByCompanyId(company.getId());
    }

    public boolean isDuplicateCode(HrCompany company, HrCompanyBranch current) {
        if (company == null || company.getId() == null) return false;
        if (current == null || current.getBranchCode() == null) return false;

        boolean exists = branchRepo.existsByCompanyIdAndBranchCodeIgnoreCase(
                company.getId(), current.getBranchCode()
        );

        // Kalau update record lama, dan code tidak berubah, kita anggap aman.
        // Cara simpel: kalau id null -> pasti create -> cek exists.
        // Kalau id not null -> tetap cek exists, tapi validasi detail dilakukan di View.
        return exists && current.getId() == null;
    }

    @Transactional
    public HrCompanyBranch save(HrCompanyBranch branch) {
        return branchRepo.save(branch);
    }

    @Transactional
    public void delete(HrCompanyBranch branch) {
        branchRepo.delete(branch);
    }
}
