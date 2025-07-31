package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import com.fusi24.pangreksa.web.model.entity.HrPosition;
import com.fusi24.pangreksa.web.repo.HrCompanyRepository;
import com.fusi24.pangreksa.web.repo.HrOrgStructureRepository;
import com.fusi24.pangreksa.web.repo.HrPositionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {
    private final HrCompanyRepository hrCompanyRepository;
    private final HrOrgStructureRepository hrOrgStructureRepository;
    private final HrPositionRepository hrPositionRepository;
    private final String ENTITY_NAME = "Company";

    public CompanyService(HrCompanyRepository hrCompanyRepository, HrOrgStructureRepository hrOrgStructureRepository,HrPositionRepository hrPositionRepository) {
        this.hrCompanyRepository = hrCompanyRepository;
        this.hrOrgStructureRepository = hrOrgStructureRepository;
        this.hrPositionRepository = hrPositionRepository;
    }

    public List<HrCompany> getallCompanies() {
        return hrCompanyRepository.findAll();
    }

    public List<HrOrgStructure> getAllOrgStructuresInCompany(HrCompany company) {
        return hrOrgStructureRepository.findByCompany(company);
    }

    public List<HrPosition> getAllPositionsInOrganization(HrCompany company, HrOrgStructure orgStructure) {
        return hrPositionRepository.findByCompanyAndOrgStructure(company, orgStructure);
    }
}
