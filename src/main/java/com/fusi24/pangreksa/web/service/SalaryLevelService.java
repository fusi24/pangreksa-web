package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.dto.UserLevelRow;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.model.entity.HrSalaryEmployeeLevel;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryBaseLevelRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryEmployeeLevelRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryLevelService {

    @PersistenceContext
    private EntityManager em;

    private final HrSalaryEmployeeLevelRepository employeeLevelRepo;
    private final HrSalaryBaseLevelRepository baseLevelRepo;
    private final FwAppUserRepository appUserRepository;

    public SalaryLevelService(HrSalaryEmployeeLevelRepository employeeLevelRepo,
                              HrSalaryBaseLevelRepository baseLevelRepo, FwAppUserRepository appUserRepository) {
        this.employeeLevelRepo = employeeLevelRepo;
        this.baseLevelRepo = baseLevelRepo;
        this.appUserRepository = appUserRepository;
    }

    public List<UserLevelRow> findUserLevelRows(String keyword) {
        List<Object[]> raw = employeeLevelRepo.findUserLevelRows(
                (keyword == null || keyword.isBlank()) ? null : keyword);
        return raw.stream()
                .map(RowMapperUtil::map)
                .collect(Collectors.toList());
    }

    public List<HrSalaryBaseLevel> findActiveBaseLevels(LocalDate today, Long companyId) {
        return baseLevelRepo.findActive(today, companyId);
    }

    public FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    @Transactional
    public void upsertMappings(List<UserLevelRow> rows, AppUserInfo appUserInfo) {
        var appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        for (var row : rows) {
            Long userId = row.getUserId();
            Long baseLevelId = row.getSelectedBaseLevelId();

            if (userId == null || baseLevelId == null) continue;

            var appUserRef   = em.getReference(FwAppUser.class, userId);
            var baseLevelRef = em.getReference(HrSalaryBaseLevel.class, baseLevelId);

            var existing = employeeLevelRepo.findByAppUser_Id(userId).orElse(null);
            if (existing == null) {
                var e = new HrSalaryEmployeeLevel();
                e.setAppUser(appUserRef);
                e.setBaseLevel(baseLevelRef);
                e.setCreatedBy(appUser);
                e.setUpdatedBy(appUser);
                employeeLevelRepo.save(e);
            } else {
                existing.setBaseLevel(baseLevelRef);
                employeeLevelRepo.save(existing);
            }
        }
    }
}