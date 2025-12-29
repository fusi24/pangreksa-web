package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import com.fusi24.pangreksa.web.repo.HrSalaryAllowanceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PayrollService {
    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository;
    private final HrSalaryAllowanceRepository hrSalaryAllowanceRepository;
    private final HrSalaryPositionAllowanceRepository hrSalaryPositionAllowanceRepository;
    private final FwAppUserRepository appUserRepository;

    @Autowired
    private HrPayrollRepository hrPayrollRepository;

    @Autowired
    private SystemService systemService;

    @Autowired
    private HrPayrollCalculationRepository hrPayrollCalculationRepository;

    @Autowired
    private HrTaxBracketRepository hrTaxBracketRepository;

    @Autowired
    private HrSalaryEmployeeLevelRepository hrSalaryEmployeeLevelRepository;

    @Autowired
    private HrPersonPositionRepository hrPersonPositionRepository;

    @Autowired
    private HrPersonRespository hrPersonRespository;

    public PayrollService(HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository,
                          FwAppUserRepository appUserRepository,
                          HrSalaryAllowanceRepository hrSalaryAllowanceRepository,
                          HrSalaryPositionAllowanceRepository hrSalaryAllowancePackageRepository) {
        this.hrSalaryBaseLevelRepository = hrSalaryBaseLevelRepository;
        this.appUserRepository = appUserRepository;
        this.hrSalaryAllowanceRepository = hrSalaryAllowanceRepository;
        this.hrSalaryPositionAllowanceRepository = hrSalaryAllowancePackageRepository;
    }

    public void deleteSalaryAllowance(HrSalaryAllowance data) {
        hrSalaryAllowanceRepository.delete(data);
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

        if (includeInactive) {
            return hrSalaryBaseLevelRepository.findByCompanyOrderByLevelCodeAsc(appUser.getCompany());
        }

        return hrSalaryBaseLevelRepository.findByCompanyAndIsActiveTrueOrderByLevelCodeAsc(appUser.getCompany());
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
            if (salaryAllowance.getCompany() == null) {
                salaryAllowance.setCompany(appUser.getCompany());
            }

            salaryAllowance.setCreatedBy(appUser);
            salaryAllowance.setUpdatedBy(appUser);
            salaryAllowance.setCreatedAt(LocalDateTime.now());
            salaryAllowance.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryAllowance.setUpdatedBy(appUser);
            salaryAllowance.setUpdatedAt(LocalDateTime.now());
            if (salaryAllowance.getCompany() == null) {
                salaryAllowance.setCompany(appUser.getCompany());
            }
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

    @Transactional
    public HrPayroll savePayroll(HrPayroll data, AppUserInfo userInfo) {
        FwAppUser appUser = this.findAppUserByUserId(userInfo.getUserId().toString());

        if (data.getId() == null) {
            data.setCreatedBy(appUser);
            data.setCreatedAt(LocalDateTime.now());
        }

        data.setUpdatedBy(appUser);
        data.setUpdatedAt(LocalDateTime.now());

        HrPayroll saved = hrPayrollRepository.save(data);

        calculatePayroll(saved);

        return saved;
    }

    @Transactional
    public void deletePayroll(HrPayroll data) {
        hrPayrollRepository.delete(data);
    }

    @Transactional
    public HrPayrollCalculation calculatePayroll(HrPayroll payrollInput) {
        // Entity HrPayrollCalculation pada schema saat ini hanya menyimpan agregat (gross/allowance/overtime/bonus/deduct/net).
        // Maka perhitungan di sini disesuaikan agar sejalan dengan kolom yang tersedia.

        if (payrollInput == null) {
            throw new IllegalArgumentException("Payroll input must not be null");
        }

        // ==== Derive base salary + fixed allowances from existing master data (jika tersedia) ====
        BigDecimal baseSalary = BigDecimal.ZERO;
        BigDecimal fixedAllowance = BigDecimal.ZERO;

        if (payrollInput.getPerson() != null) {
            try {
                FwAppUser personUser = appUserRepository.findByPersonId(payrollInput.getPerson().getId())
                        .orElseThrow(() -> new RuntimeException("Person user not found"));

                HrSalaryEmployeeLevel salaryEmployeeLevel = hrSalaryEmployeeLevelRepository.findByAppUserId(personUser.getId())
                        .orElseThrow(() -> new RuntimeException("Salary Employee Level not found"));

                baseSalary = salaryEmployeeLevel.getBaseLevel() == null ? BigDecimal.ZERO : salaryEmployeeLevel.getBaseLevel().getBaseSalary();

                HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(payrollInput.getPerson().getId());
                if (personPosition != null && personPosition.getPosition() != null) {
                    List<HrSalaryPositionAllowance> allowances = hrSalaryPositionAllowanceRepository
                            .findByPositionAndCompanyOrderByUpdatedAtAsc(personPosition.getPosition(), personPosition.getCompany());

                    fixedAllowance = BigDecimal.valueOf(
                            allowances.stream().mapToDouble(p -> p.getAllowance().getAmount().doubleValue()).sum()
                    );
                }
            } catch (Exception ex) {
                // Jangan memblokir proses kalkulasi kalau master belum lengkap; gunakan default 0.
                log.warn("Skipping base salary/fixed allowance derivation: {}", ex.getMessage());
            }
        }

        // ==== Variable allowance / overtime / bonus / deductions (dari payroll input snapshot) ====
        BigDecimal variableAllowances = parseBigDecimalSafe(payrollInput.getAllowancesValue());
        BigDecimal overtimeAmount = payrollInput.getOvertimeValuePayment() == null ? BigDecimal.ZERO : payrollInput.getOvertimeValuePayment();
        BigDecimal totalBonus = payrollInput.getAnnualBonus() == null ? BigDecimal.ZERO : payrollInput.getAnnualBonus();
        BigDecimal otherDeductions = payrollInput.getOtherDeductions() == null ? BigDecimal.ZERO : payrollInput.getOtherDeductions();

        BigDecimal totalAllowances = fixedAllowance.add(variableAllowances);
        BigDecimal totalOvertimes = overtimeAmount;

        // Gross = base + allowance + overtime + bonus
        BigDecimal grossSalary = baseSalary
                .add(totalAllowances)
                .add(totalOvertimes)
                .add(totalBonus);

        // Total taxable: pada schema saat ini disimpan sebagai agregat; gunakan gross - other deductions sebagai pendekatan.
        BigDecimal totalTaxable = grossSalary.subtract(otherDeductions);
        if (totalTaxable.compareTo(BigDecimal.ZERO) < 0) {
            totalTaxable = BigDecimal.ZERO;
        }

        // Net THP (schema saat ini tidak menyimpan komponen pajak/BPJS; gunakan gross - other deductions)
        BigDecimal netTakeHomePay = grossSalary.subtract(otherDeductions);

        HrPayrollCalculation current = hrPayrollCalculationRepository.findFirstByPayrollInputId(payrollInput.getId());

        HrPayrollCalculation calculation = HrPayrollCalculation.builder()
                .id(current == null ? null : current.getId())
                .payrollInput(payrollInput)
                .grossSalary(grossSalary)
                .totalAllowances(totalAllowances)
                .totalOvertimes(totalOvertimes)
                .totalBonus(totalBonus)
                .totalOtherDeductions(otherDeductions)
                .totalTaxable(totalTaxable)
                .netTakeHomePay(netTakeHomePay)
                .calculatedAt(LocalDateTime.now())
                .notes("Calculated automatically on save")
                .build();

        return hrPayrollCalculationRepository.save(calculation);
    }

    private BigDecimal parseBigDecimalSafe(String raw) {
        if (raw == null) return BigDecimal.ZERO;
        String s = raw.trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        try {
            s = s.replace(",", "");
            return new BigDecimal(s);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculatePPh21(BigDecimal monthlyTaxableIncome, BigDecimal annualTaxableIncome) {
        List<HrTaxBracket> brackets = hrTaxBracketRepository.findAllByOrderByMinIncomeAsc();

        BigDecimal annualTax = BigDecimal.ZERO;
        BigDecimal remaining = annualTaxableIncome;

        for (HrTaxBracket bracket : brackets) {
            BigDecimal min = bracket.getMinIncome();
            BigDecimal max = bracket.getMaxIncome() != null ? bracket.getMaxIncome() : new BigDecimal("999999999999");
            BigDecimal rate = bracket.getTaxRate().divide(BigDecimal.valueOf(100));

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal bracketRange = max.subtract(min);
            BigDecimal taxableInBracket = remaining.compareTo(bracketRange) > 0 ? bracketRange : remaining;

            BigDecimal taxInBracket = taxableInBracket.multiply(rate);
            annualTax = annualTax.add(taxInBracket);
            remaining = remaining.subtract(taxableInBracket);
        }

        return annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    public Page<HrPayroll> getPayrollPage(Pageable pageable, Integer year, LocalDate month, String searchTerm) {
        if (appUser == null) {
            throw new IllegalStateException("App user is not set. Please call setUser() before using this method.");
        }

        Specification<HrPayroll> spec = buildFilterSpec(year, month, searchTerm);

        return hrPayrollRepository.findAll(spec, pageable);
    }

    public long countPayroll(Integer year, LocalDate month, String searchTerm) {
        Specification<HrPayroll> spec = buildFilterSpec(year, month, searchTerm);
        return hrPayrollRepository.count(spec);
    }

    private Specification<HrPayroll> buildFilterSpec(Integer year, LocalDate month, String searchTerm) {
        Specification<HrPayroll> spec = buildBaseSearchSpec(searchTerm);

        if (year != null) {
            LocalDate startOfYear = LocalDate.of(year, 1, 1);
            LocalDate startOfNextYear = startOfYear.plusYears(1);
            spec = spec.and((root, query, cb) ->
                    cb.and(
                            cb.greaterThanOrEqualTo(root.get("payrollDate"), startOfYear),
                            cb.lessThan(root.get("payrollDate"), startOfNextYear)
                    )
            );
        }

        if (month != null) {
            LocalDate startOfMonth = month.withDayOfMonth(1);
            LocalDate startOfNextMonth = startOfMonth.plusMonths(1);
            spec = spec.and((root, query, cb) ->
                    cb.and(
                            cb.greaterThanOrEqualTo(root.get("payrollDate"), startOfMonth),
                            cb.lessThan(root.get("payrollDate"), startOfNextMonth)
                    )
            );
        }

        return spec;
    }

    private Specification<HrPayroll> buildBaseSearchSpec(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return (root, query, cb) -> {
                return  cb.conjunction();
            };
        }

        String lowerCaseSearchTerm = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, cb) -> {

            // Search berdasarkan snapshot nama di hr_payroll (first_name/last_name)
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), lowerCaseSearchTerm),
                    cb.like(cb.lower(root.get("lastName")), lowerCaseSearchTerm)
            );
        };
    }

    public List<HrPerson> getActiveEmployees() {
        if (appUser == null) {
            throw new IllegalStateException("App user not set");
        }
        return hrPersonRespository.findAll();
    }

    public HrPayrollCalculation getCalculationByPayrollId(Long payrollId) {
        return hrPayrollCalculationRepository.findFirstByPayrollInputId(payrollId);
    }
}
