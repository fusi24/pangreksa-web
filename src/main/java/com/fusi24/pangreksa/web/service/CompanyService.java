package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.repo.HrCompanyRepository;
import com.fusi24.pangreksa.web.repo.HrPersonRespository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {
    private final HrCompanyRepository hrCompanyRepository;

    public CompanyService(HrCompanyRepository hrCompanyRepository) {
        this.hrCompanyRepository = hrCompanyRepository;
    }

    public List<HrCompany> getallCompanies() {
        return hrCompanyRepository.findAll();
    }

}
