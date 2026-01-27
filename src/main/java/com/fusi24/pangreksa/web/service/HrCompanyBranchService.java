package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import com.fusi24.pangreksa.web.repo.HrCompanyBranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HrCompanyBranchService {

    private final HrCompanyBranchRepository repo;

    public HrCompanyBranchService(HrCompanyBranchRepository repo) {
        this.repo = repo;
    }

    public List<HrCompanyBranch> findAll() {
        return repo.findAll();
    }
}
