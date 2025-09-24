package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.model.entity.HrSalaryEmployeeLevel;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryBaseLevelRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryEmployeeLevelRepository;
import com.fusi24.pangreksa.web.repo.HrSalaryEmployeeLevelRepository.UserLevelProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SalaryLevelService {

    @PersistenceContext
    private EntityManager em;

    private final HrSalaryEmployeeLevelRepository employeeLevelRepo;
    private final HrSalaryBaseLevelRepository baseLevelRepo;
    private final FwAppUserRepository appUserRepository;

    public SalaryLevelService(HrSalaryEmployeeLevelRepository employeeLevelRepo,
                              HrSalaryBaseLevelRepository baseLevelRepo,
                              FwAppUserRepository appUserRepository) {
        this.employeeLevelRepo = employeeLevelRepo;
        this.baseLevelRepo = baseLevelRepo;
        this.appUserRepository = appUserRepository;
    }

    /** Ambil baris user + level (tanpa DTO & tanpa RowMapper), pakai projection langsung */
    public List<UserLevelProjection> findUserLevelRows(String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return employeeLevelRepo.findUserLevelRows(kw);
    }

    public List<HrSalaryBaseLevel> findActiveBaseLevels(LocalDate today, Long companyId) {
        return baseLevelRepo.findActive(today, companyId);
    }

    public FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    /** Terima map userId -> baseLevelId dari UI, lalu upsert mapping */
    @Transactional
    public void upsertMappings(Map<Long, Long> userToBaseLevelId, AppUserInfo actor) {
        var appUser = this.findAppUserByUserId(actor.getUserId().toString());

        for (Map.Entry<Long, Long> e : userToBaseLevelId.entrySet()) {
            Long userId = e.getKey();
            Long baseLevelId = e.getValue();
            if (userId == null || baseLevelId == null) continue;

            var appUserRef   = em.getReference(FwAppUser.class, userId);
            var baseLevelRef = em.getReference(HrSalaryBaseLevel.class, baseLevelId);

            var existing = employeeLevelRepo.findByAppUser_Id(userId).orElse(null);
            if (existing == null) {
                var m = new HrSalaryEmployeeLevel();
                m.setAppUser(appUserRef);
                m.setBaseLevel(baseLevelRef);
                m.setCreatedBy(appUser);
                m.setUpdatedBy(appUser);
                employeeLevelRepo.save(m);
            } else {
                existing.setBaseLevel(baseLevelRef);
                existing.setUpdatedBy(appUser);
                employeeLevelRepo.save(existing);
            }
        }
    }
}
