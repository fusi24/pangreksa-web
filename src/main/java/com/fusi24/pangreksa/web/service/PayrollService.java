package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public HrPayroll savePayroll(HrPayroll data, AppUserInfo userInfo) {
        FwAppUser appUser = this.findAppUserByUserId(userInfo.getUserId().toString());

        // Set createdBy and createdAt for new records
        if (data.getId() == null) {
            data.setCreatedBy(appUser);
            data.setCreatedAt(LocalDateTime.now());
        }

        // Always set updatedBy and updatedAt when saving (both new and existing)
        data.setUpdatedBy(appUser);
        data.setUpdatedAt(LocalDateTime.now());

        HrPayroll saved = hrPayrollRepository.save(data);

        // Trigger payroll calculation
        calculatePayroll(saved);

        return saved;
    }

    @Transactional
    public void deletePayroll(HrPayroll data) {
        hrPayrollRepository.delete(data);
    }

    @Transactional
    public HrPayrollCalculation calculatePayroll(HrPayroll payrollInput) {
        // 1. Fetch configs
        BigDecimal bpjsHealthRate = systemService.getConfigNumericValue("Persentase Tarif BPJS Kesehatan (%)").divide(BigDecimal.valueOf(100)); // 1% → 0.01
        BigDecimal bpjsJhtRate = systemService.getConfigNumericValue("Persentase Tarif BPJS JHT (%)").divide(BigDecimal.valueOf(100)); // 2% → 0.02
        BigDecimal bpjsJpRate = systemService.getConfigNumericValue("Persentase Tarif BPJS JP (%)").divide(BigDecimal.valueOf(100)); // 1% → 0.01

        BigDecimal bpjsHealthCap = systemService.getConfigNumericValue("Batas Upah BPJS Kesehatan");
        BigDecimal bpjsJpCap = systemService.getConfigNumericValue("Batas Upah BPJS JP");

        // Determine PTKP based on employee's tax status (assuming you have it in HrPerson)
        // status pajak dapet darimana? asumsi TKO semua
//        String taxStatus = payrollInput.getPerson().getTaxStatus(); // e.g., "TK/0"
        String taxStatus = "TK0";
        BigDecimal ptkp = switch (taxStatus) {
            case "TK/0" -> systemService.getConfigNumericValue("PTKP TK0");
            case "TK/1" -> systemService.getConfigNumericValue("PTKP TK1"); // ← Add this key if needed
            case "K/0" -> systemService.getConfigNumericValue("PTKP K0");
            // ... add more cases as needed
            default -> systemService.getConfigNumericValue("PTKP TK0"); // fallback
        };

        String roundingRule = systemService.getConfigStringValue("PEMBULATAN PKP"); // e.g., "FLOOR"

        FwAppUser personUser = appUserRepository.findByPersonId(payrollInput.getPerson().getId()).orElseThrow(() -> new RuntimeException("Person not found"));
        HrSalaryEmployeeLevel salaryEmployeeLevel = hrSalaryEmployeeLevelRepository.findByAppUserId(personUser.getId()).orElseThrow(() -> new RuntimeException("Salary Employee Level not found"));
        HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(payrollInput.getPerson().getId());
        List<HrSalaryPositionAllowance> allowances = hrSalaryPositionAllowanceRepository.findByPositionAndCompanyOrderByUpdatedAtAsc(personPosition.getPosition(), personPosition.getCompany());

        // 2. Calculate Gross
        BigDecimal baseSalary = salaryEmployeeLevel.getBaseLevel().getBaseSalary();
        BigDecimal fixedAllowance = BigDecimal.valueOf(allowances.stream().mapToDouble(p -> p.getAllowance().getAmount().doubleValue()).sum());
        BigDecimal variableAllowances = payrollInput.getVariableAllowances() != null ? payrollInput.getVariableAllowances() : BigDecimal.ZERO;
        BigDecimal overtimeAmount = payrollInput.getOvertimeAmount() != null ? payrollInput.getOvertimeAmount() : BigDecimal.ZERO;
        BigDecimal annualBonus = payrollInput.getAnnualBonus() != null ? payrollInput.getAnnualBonus() : BigDecimal.ZERO;

        BigDecimal grossSalary = baseSalary
                .add(fixedAllowance)
                .add(variableAllowances)
                .add(overtimeAmount)
                .add(annualBonus);

        // 3. Calculate BPJS Deductions (capped)
        BigDecimal bpjsHealthBase = grossSalary.compareTo(bpjsHealthCap) > 0 ? bpjsHealthCap : grossSalary;
        BigDecimal bpjsHealthDeduction = bpjsHealthBase.multiply(bpjsHealthRate);

        BigDecimal bpjsJhtDeduction = grossSalary.multiply(bpjsJhtRate); // no cap for JHT (as per common practice)

        BigDecimal bpjsJpBase = grossSalary.compareTo(bpjsJpCap) > 0 ? bpjsJpCap : grossSalary;
        BigDecimal bpjsJpDeduction = bpjsJpBase.multiply(bpjsJpRate);

        // 4. Annual Income Before Tax
        BigDecimal annualIncomeBeforeTax = grossSalary.multiply(BigDecimal.valueOf(12));

        // 5. Taxable Income Calculation
        BigDecimal annualTaxableIncome = annualIncomeBeforeTax.subtract(ptkp);
        if (annualTaxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            annualTaxableIncome = BigDecimal.ZERO;
        }

        // Apply rounding rule to monthly taxable income
        BigDecimal monthlyTaxableIncome = annualTaxableIncome.divide(BigDecimal.valueOf(12), 0, RoundingMode.DOWN); // default: FLOOR

        if ("CEILING".equalsIgnoreCase(roundingRule)) {
            monthlyTaxableIncome = annualTaxableIncome.divide(BigDecimal.valueOf(12), 0, RoundingMode.UP);
        } else if ("HALF_UP".equalsIgnoreCase(roundingRule)) {
            monthlyTaxableIncome = annualTaxableIncome.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
        }
        // else: FLOOR (default)

        // 6. Calculate PPh 21 using HrTaxBracket
        BigDecimal pph21Amount = calculatePPh21(monthlyTaxableIncome, annualTaxableIncome);

        // 7. Other Deductions
        BigDecimal otherDeductions = payrollInput.getOtherDeductions() != null ? payrollInput.getOtherDeductions() : BigDecimal.ZERO;
        BigDecimal previousThpPaid = payrollInput.getPreviousThpPaid() != null ? payrollInput.getPreviousThpPaid() : BigDecimal.ZERO;

        // 8. Net Take Home Pay
        BigDecimal netTakeHomePay = grossSalary
                .subtract(bpjsHealthDeduction)
                .subtract(bpjsJhtDeduction)
                .subtract(bpjsJpDeduction)
                .subtract(pph21Amount)
                .subtract(otherDeductions)
                .subtract(previousThpPaid);

        // 9. Build and Save Calculation Result

        HrPayrollCalculation current = hrPayrollCalculationRepository.findFirstByPayrollInputId(payrollInput.getId());

        HrPayrollCalculation calculation = HrPayrollCalculation.builder()
                .payrollInput(payrollInput)
                .grossSalary(grossSalary)
                .bpjsHealthDeduction(bpjsHealthDeduction)
                .bpjsJhtDeduction(bpjsJhtDeduction)
                .bpjsJpDeduction(bpjsJpDeduction)
                .annualIncomeBeforeTax(annualIncomeBeforeTax)
                .ptkpApplied(ptkp)
                .taxableIncome(monthlyTaxableIncome)
                .pph21Amount(pph21Amount)
                .netTakeHomePay(netTakeHomePay)
                .calculatedAt(LocalDateTime.now())
                .notes("Calculated automatically on save")
                .id(current == null ? null : current.getId())
                .build();

        return hrPayrollCalculationRepository.save(calculation);
    }

    private BigDecimal calculatePPh21(BigDecimal monthlyTaxableIncome, BigDecimal annualTaxableIncome) {
        // Fetch tax brackets ordered by min_income
        List<HrTaxBracket> brackets = hrTaxBracketRepository.findAllByOrderByMinIncomeAsc();

        BigDecimal annualTax = BigDecimal.ZERO;
        BigDecimal remaining = annualTaxableIncome;

        for (HrTaxBracket bracket : brackets) {
            BigDecimal min = bracket.getMinIncome();
            BigDecimal max = bracket.getMaxIncome() != null ? bracket.getMaxIncome() : new BigDecimal("999999999999");
            BigDecimal rate = bracket.getTaxRate().divide(BigDecimal.valueOf(100)); // if stored as 5, 15, etc.

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal bracketRange = max.subtract(min);
            BigDecimal taxableInBracket = remaining.compareTo(bracketRange) > 0 ? bracketRange : remaining;

            BigDecimal taxInBracket = taxableInBracket.multiply(rate);
            annualTax = annualTax.add(taxInBracket);
            remaining = remaining.subtract(taxableInBracket);
        }

        // Return monthly PPh21
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
                            cb.greaterThanOrEqualTo(root.get("payrollMonth"), startOfYear),
                            cb.lessThan(root.get("payrollMonth"), startOfNextYear)
                    )
            );
        }

        if (month != null) {
            LocalDate startOfMonth = month.withDayOfMonth(1);
            LocalDate startOfNextMonth = startOfMonth.plusMonths(1);
            spec = spec.and((root, query, cb) ->
                    cb.and(
                            cb.greaterThanOrEqualTo(root.get("payrollMonth"), startOfMonth),
                            cb.lessThan(root.get("payrollMonth"), startOfNextMonth)
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

            Join<HrPayroll, HrPerson> personJoin = root.join("person");
            return cb.or(
                    cb.like(cb.lower(personJoin.get("firstName")), lowerCaseSearchTerm),
                    cb.like(cb.lower(personJoin.get("lastName")), lowerCaseSearchTerm)
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
