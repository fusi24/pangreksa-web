package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryPositionAllowanceRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryAllowanceRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryBaseLevelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PayrollService {
    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository;
    private final HrSalaryAllowanceRepository hrSalaryAllowanceRepository;
    private final HrSalaryPositionAllowanceRepository hrSalaryPositionAllowanceRepository;
    private final FwAppUserRepository appUserRepository;

    public PayrollService(HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository,
                          FwAppUserRepository appUserRepository,
                          HrSalaryAllowanceRepository hrSalaryAllowanceRepository,
                          HrSalaryPositionAllowanceRepository hrSalaryAllowancePackageRepository) {
        this.hrSalaryBaseLevelRepository = hrSalaryBaseLevelRepository;
        this.appUserRepository = appUserRepository;
        this.hrSalaryAllowanceRepository = hrSalaryAllowanceRepository;
        this.hrSalaryPositionAllowanceRepository = hrSalaryAllowancePackageRepository;
    }

    private FwAppUser appUser;

    public void setUser(AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());
        this.appUser = appUser;
    }

    private FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public List<HrSalaryBaseLevel> getAllSalaryBaseLevels(boolean includeInactive) {
        if (appUser == null) {
            throw new IllegalStateException("App user is not set. Please call setUser() before using this method.");
        }

        if (includeInactive)
            return hrSalaryBaseLevelRepository.findByCompanyOrderByLevelCodeAsc(appUser.getCompany());
        else
            return hrSalaryBaseLevelRepository.findByCompanyAndEndDateIsNullOrderByLevelCodeAsc(appUser.getCompany());
    }

    public List<HrSalaryAllowance> getAllSalaryAllowances(boolean includeInactive) {
        if (includeInactive)
            return hrSalaryAllowanceRepository.findByCompanyOrderByNameAsc(appUser.getCompany());
        else
            return hrSalaryAllowanceRepository.findByCompanyAndEndDateIsNullOrderByNameAsc(appUser.getCompany());
    }

    public List<HrSalaryPositionAllowance> getSalaryPositionAllowancesByPosition(HrPosition position, boolean includeInactive) {
        if (includeInactive)
            return hrSalaryPositionAllowanceRepository.findByPositionAndCompanyOrderByUpdatedAtAsc(position, appUser.getCompany());
        else
            return hrSalaryPositionAllowanceRepository.findByPositionAndCompanyAndEndDateIsNullOrderByUpdatedAtAsc(position, appUser.getCompany());
    }

    public HrSalaryBaseLevel saveSalaryBaseLevel(HrSalaryBaseLevel salaryBaseLevel, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if (salaryBaseLevel.getId() == null) {
            salaryBaseLevel.setCreatedBy(appUser);
            salaryBaseLevel.setUpdatedBy(appUser);
            salaryBaseLevel.setCreatedAt(LocalDateTime.now());
            salaryBaseLevel.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryBaseLevel.setUpdatedBy(appUser);
            salaryBaseLevel.setUpdatedAt(LocalDateTime.now());
        }

        return hrSalaryBaseLevelRepository.save(salaryBaseLevel);
    }

    public HrSalaryAllowance saveSalaryAllowance(HrSalaryAllowance salaryAllowance, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if (salaryAllowance.getId() == null) {
            salaryAllowance.setCreatedBy(appUser);
            salaryAllowance.setUpdatedBy(appUser);
            salaryAllowance.setCreatedAt(LocalDateTime.now());
            salaryAllowance.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryAllowance.setUpdatedBy(appUser);
            salaryAllowance.setUpdatedAt(LocalDateTime.now());
        }

        return hrSalaryAllowanceRepository.save(salaryAllowance);
    }

    public HrSalaryPositionAllowance saveSalaryPositionAllowance(HrSalaryPositionAllowance salaryPositionAllowance, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if (salaryPositionAllowance.getId() == null) {
            salaryPositionAllowance.setCreatedBy(appUser);
            salaryPositionAllowance.setUpdatedBy(appUser);
            salaryPositionAllowance.setCreatedAt(LocalDateTime.now());
            salaryPositionAllowance.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryPositionAllowance.setUpdatedBy(appUser);
            salaryPositionAllowance.setUpdatedAt(LocalDateTime.now());
        }

        return hrSalaryPositionAllowanceRepository.save(salaryPositionAllowance);
    }
}
