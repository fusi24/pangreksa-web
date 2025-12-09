package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.repo.HrSalaryBaseLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SalaryBaseLevelService {

    private final HrSalaryBaseLevelRepository baseRepo;

    public SalaryBaseLevelService(HrSalaryBaseLevelRepository baseRepo) {
        this.baseRepo = baseRepo;
    }

    public HrSalaryBaseLevel getActiveVersion(HrCompany company) {
        return baseRepo.findActiveVersion(company);
    }

    /**
     * Generate kode untuk ADD saja:
     * FORMAT: LEVEL-001-2025
     */
    public String generateLevelCode(LocalDate startDate, HrCompany company) {
        int year = startDate.getYear();
        Integer maxSeq = baseRepo.findMaxSequenceByYear(company.getId(), year);
        if (maxSeq == null) maxSeq = 0;
        return "LEVEL-" + String.format("%03d", maxSeq + 1) + "-" + year;
    }

    /**
     * Close versi lama:
     * - set endDate
     * - set isActive = false
     */
    @Transactional
    public void closeOldVersion(HrSalaryBaseLevel oldVersion, LocalDate newStartDate, FwAppUser actor) {
        if (oldVersion == null) return;

        oldVersion.setEndDate(newStartDate.minusDays(1));
        oldVersion.setIsActive(false);
        oldVersion.setUpdatedBy(actor);

        baseRepo.save(oldVersion);
    }

    /**
     * Create versi baru:
     * - isActive diambil dari checkbox
     */
    @Transactional
    public HrSalaryBaseLevel createNewVersion(
            HrCompany company,
            BigDecimal amount,
            LocalDate startDate,
            LocalDate endDate,
            String levelCode,
            String reason,
            Boolean isActive,
            FwAppUser actor
    ) {
        HrSalaryBaseLevel newLevel = new HrSalaryBaseLevel();

        newLevel.setCompany(company);
        newLevel.setLevelCode(levelCode);
        newLevel.setBaseSalary(amount);
        newLevel.setStartDate(startDate);
        newLevel.setEndDate(endDate);
        newLevel.setReason(reason);
        newLevel.setIsActive(isActive != null ? isActive : Boolean.TRUE);

        newLevel.setCreatedBy(actor);
        newLevel.setUpdatedBy(actor);

        return baseRepo.save(newLevel);
    }

    @Transactional
    public void delete(HrSalaryBaseLevel level) {
        baseRepo.delete(level);
    }
}
