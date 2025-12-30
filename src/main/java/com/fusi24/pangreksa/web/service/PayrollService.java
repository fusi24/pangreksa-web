package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
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
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import java.math.RoundingMode;

@Service
public class PayrollService {
    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository;
    private final HrSalaryAllowanceRepository hrSalaryAllowanceRepository;
    private final HrSalaryPositionAllowanceRepository hrSalaryPositionAllowanceRepository;
    private final FwAppUserRepository appUserRepository;

    private final HrPayrollRepository hrPayrollRepository;
    private final HrPayrollCalculationRepository hrPayrollCalculationRepository;
    private final HrTaxBracketRepository hrTaxBracketRepository;
    private final HrSalaryEmployeeLevelRepository hrSalaryEmployeeLevelRepository;

    private final HrPersonPositionRepository hrPersonPositionRepository;
    private final HrPersonRespository hrPersonRepository;
    private final HrPersonPtkpRepository hrPersonPtkpRepository;
    private final HrLeaveApplicationRepository hrLeaveApplicationRepository;

    private final SystemService systemService;

    @Autowired
    public PayrollService(HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository,
                          FwAppUserRepository appUserRepository,
                          HrSalaryAllowanceRepository hrSalaryAllowanceRepository,
                          HrSalaryPositionAllowanceRepository hrSalaryAllowancePackageRepository,
                          HrPayrollRepository hrPayrollRepository,
                          SystemService systemService,
                          HrPayrollCalculationRepository hrPayrollCalculationRepository,
                          HrTaxBracketRepository hrTaxBracketRepository,
                          HrSalaryEmployeeLevelRepository hrSalaryEmployeeLevelRepository,
                          HrPersonPositionRepository hrPersonPositionRepository,
                          HrPersonRespository hrPersonRepository,
                          HrPersonPtkpRepository hrPersonPtkpRepository,
                          HrLeaveApplicationRepository hrLeaveApplicationRepository) {

        this.hrSalaryBaseLevelRepository = hrSalaryBaseLevelRepository;
        this.appUserRepository = appUserRepository;
        this.hrSalaryAllowanceRepository = hrSalaryAllowanceRepository;
        this.hrSalaryPositionAllowanceRepository = hrSalaryAllowancePackageRepository;

        this.hrPayrollRepository = hrPayrollRepository;
        this.systemService = systemService;
        this.hrPayrollCalculationRepository = hrPayrollCalculationRepository;
        this.hrTaxBracketRepository = hrTaxBracketRepository;
        this.hrSalaryEmployeeLevelRepository = hrSalaryEmployeeLevelRepository;

        this.hrPersonPositionRepository = hrPersonPositionRepository;
        this.hrPersonRepository = hrPersonRepository;
        this.hrPersonPtkpRepository = hrPersonPtkpRepository;
        this.hrLeaveApplicationRepository = hrLeaveApplicationRepository;

        log.warn("PayrollService constructed. instance={}, hrPersonRepositoryInjected={}",
                System.identityHashCode(this),
                (this.hrPersonRepository != null));

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

                baseSalary = salaryEmployeeLevel.getBaseLevel() == null
                        ? BigDecimal.ZERO
                        : salaryEmployeeLevel.getBaseLevel().getBaseSalary();

                HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(payrollInput.getPerson().getId());
                if (personPosition != null && personPosition.getPosition() != null) {
                    List<HrSalaryPositionAllowance> allowances = hrSalaryPositionAllowanceRepository
                            .findByPositionAndCompanyOrderByUpdatedAtAsc(personPosition.getPosition(), personPosition.getCompany());

                    fixedAllowance = BigDecimal.valueOf(
                            allowances.stream()
                                    .mapToDouble(p -> p.getAllowance() == null ? 0d : p.getAllowance().getAmount().doubleValue())
                                    .sum()
                    );
                }
            } catch (Exception ex) {
                // Jangan memblokir proses kalkulasi kalau master belum lengkap; gunakan default 0.
                log.warn("Skipping base salary/fixed allowance derivation: {}", ex.getMessage());
            }
        }

        // ==== Variable allowance (dari hr_payroll.allowances_value) ====
        BigDecimal variableAllowances = parseBigDecimalSafe(payrollInput.getAllowancesValue());

        BigDecimal totalAllowances = fixedAllowance.add(variableAllowances);

        // ==========================================================
        // ==== OVERTIME (INI BAGIAN E YANG ANDA TANYAKAN) ===========
        // payrollInput.getOvertimeHours() = menit 0..60
        // payrollInput.getOvertimeType() = STATIC / PERCENTAGE
        // payrollInput.getOvertimeValuePayment() = nominal (STATIC) atau persen (PERCENTAGE)
        // Total overtime = (base overtime value) * (menit/60)
        // ==========================================================

        BigDecimal overtimeMinutes = payrollInput.getOvertimeHours() == null
                ? BigDecimal.ZERO
                : payrollInput.getOvertimeHours();

        BigDecimal overtimeFactor = overtimeMinutes.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal overtimeAmountBase = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(payrollInput.getOvertimeType())) {
            BigDecimal pct = payrollInput.getOvertimeValuePayment() == null
                    ? BigDecimal.ZERO
                    : payrollInput.getOvertimeValuePayment();

            overtimeAmountBase = baseSalary.multiply(pct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            overtimeAmountBase = payrollInput.getOvertimeValuePayment() == null
                    ? BigDecimal.ZERO
                    : payrollInput.getOvertimeValuePayment();
        }

        BigDecimal totalOvertimes = overtimeAmountBase.multiply(overtimeFactor);

        // ==== Bonus + Deductions (snapshot) ====
        BigDecimal totalBonus = payrollInput.getAnnualBonus() == null ? BigDecimal.ZERO : payrollInput.getAnnualBonus();
        BigDecimal otherDeductions = payrollInput.getOtherDeductions() == null ? BigDecimal.ZERO : payrollInput.getOtherDeductions();

        // Gross = base + allowance + overtime + bonus
        BigDecimal grossSalary = baseSalary
                .add(totalAllowances)
                .add(totalOvertimes)
                .add(totalBonus);

        // Total taxable (pendekatan sekarang): gross - other deductions
        BigDecimal totalTaxable = grossSalary.subtract(otherDeductions);
        if (totalTaxable.compareTo(BigDecimal.ZERO) < 0) {
            totalTaxable = BigDecimal.ZERO;
        }

        // Net THP (schema agregat sekarang): gross - other deductions
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
        return hrPersonRepository.findAll();
    }

    public HrPayrollCalculation getCalculationByPayrollId(Long payrollId) {
        return hrPayrollCalculationRepository.findFirstByPayrollInputId(payrollId);
    }

    @Getter
    @Setter
    public static class AddPayrollRequest {
        private Integer year;
        private Integer month;

        private Integer paramAttendanceDays;
        private Integer overtimeMinutes;

        // "STATIC" / "PERCENTAGE"
        private String overtimePaymentType;

        private BigDecimal overtimeStaticNominal;
        private Integer overtimePercent;

        // "NO_ALLOWANCE" / "SELECT_ALLOWANCE" / "BENEFITS_PACKAGE"
        private String allowanceMode;

        private List<HrSalaryAllowance> selectedAllowances = new ArrayList<>();
    }

    public List<HrSalaryAllowance> getSelectableAllowancesForPayrollDate(LocalDate payrollDate) {
        List<HrSalaryAllowance> all = this.getAllSalaryAllowances(true);

        return all.stream()
                .filter(a -> {
                    if (a.getStartDate() != null && payrollDate.isBefore(a.getStartDate())) return false;
                    if (a.getEndDate() == null) return true;
                    return !payrollDate.isAfter(a.getEndDate());
                })
                .toList();
    }

    @Transactional
    public void createPayrollBulk(AddPayrollRequest req, AppUserInfo userInfo) {

        FwAppUser appUser = this.findAppUserByUserId(userInfo.getUserId().toString());

        // payroll date = YYYY-MM-01
        LocalDate payrollDate = LocalDate.of(req.getYear(), req.getMonth(), 1);

        // employees aktif
        List<HrPerson> employees = hrPersonRepository.findAll();
        if (employees == null || employees.isEmpty()) {
            return;
        }

        LocalDate yearStart = LocalDate.of(req.getYear(), 1, 1);
        LocalDate yearEnd = yearStart.plusYears(1);

        for (HrPerson person : employees) {

            // ===============================
            // A. Posisi aktif pegawai
            // ===============================
            HrPersonPosition personPosition =
                    hrPersonPositionRepository.findFirstByPersonId(person.getId());

            if (personPosition == null) {
                continue;
            }

            HrPosition position = personPosition.getPosition();
            HrOrgStructure department = position != null ? position.getOrgStructure() : null;

            // ===============================
            // B. PTKP aktif
            // ===============================
            HrPersonPtkp activePtkp = hrPersonPtkpRepository
                    .findActiveByPersonId(person.getId(), payrollDate)
                    .orElse(null);

            String ptkpCode = activePtkp == null ? "K/0" : activePtkp.getPtkpCode();
            BigDecimal ptkpAmount = activePtkp == null ? BigDecimal.ZERO : activePtkp.getPtkpAmount();

            // ===============================
            // C. Total cuti tahun berjalan (APPROVED)
            // ===============================
            BigDecimal totalLeaveYear = BigDecimal.valueOf(
                    hrLeaveApplicationRepository.sumLeaveDaysByPersonAndPeriodAndStatuses(
                            person.getId(),
                            yearStart,
                            yearEnd,
                            List.of(LeaveStatusEnum.APPROVED)
                    )
            );

            // ===============================
            // D. Allowance total (mode)
            // ===============================
            BigDecimal allowanceTotal = BigDecimal.ZERO;

            if ("SELECT_ALLOWANCE".equals(req.getAllowanceMode())) {
                if (req.getSelectedAllowances() != null && !req.getSelectedAllowances().isEmpty()) {
                    allowanceTotal = req.getSelectedAllowances().stream()
                            .map(a -> a.getAmount() == null ? BigDecimal.ZERO : a.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            } else if ("BENEFITS_PACKAGE".equals(req.getAllowanceMode())) {
                allowanceTotal = resolveBenefitsPackageAllowanceTotal(person, payrollDate, personPosition);
            }

            // ===============================
            // E. Build payroll record (hr_payroll)
            // ===============================
            HrPayroll payroll = HrPayroll.builder()
                    .person(person)

                    // snapshot hr_person
                    .firstName(person.getFirstName())
                    .middleName(person.getMiddleName())
                    .lastName(person.getLastName())
                    .pob(person.getPob())
                    .dob(person.getDob())
                    .gender(person.getGender() == null ? null : person.getGender().name())
                    .ktpNumber(person.getKtpNumber())

                    // snapshot position (DDL NOT NULL -> default "")
                    .position(position != null ? position.getName() : "")
                    .positionCode(position != null ? position.getCode() : "")

                    // snapshot department (DDL NOT NULL -> default "")
                    .department(department != null ? department.getName() : "")
                    .departmentCode(department != null ? department.getCode() : "")

                    // payroll core
                    .payrollDate(payrollDate)
                    .paramAttendanceDays(req.getParamAttendanceDays())

                    // allowance
                    .allowancesType(req.getAllowanceMode())
                    .allowancesValue(allowanceTotal.toPlainString())

                    // overtime (menit 0..60 disimpan di overtime_hours)
                    .overtimeHours(BigDecimal.valueOf(req.getOvertimeMinutes() == null ? 0L : req.getOvertimeMinutes().longValue()))
                    .overtimeType(req.getOvertimePaymentType())
                    .overtimeValuePayment(resolveOvertimeValue(req))

                    // komponen per orang (diisi via Add Komponen)
                    .annualBonus(BigDecimal.ZERO)
                    .otherDeductions(BigDecimal.ZERO)

                    // PTKP + total leave
                    .ptkpCode(ptkpCode)
                    .ptkpAmount(ptkpAmount)
                    .totalLeaveYear(totalLeaveYear)

                    .build();

            // audit (AuditableEntity -> builder tidak include superclass)
            payroll.setCreatedBy(appUser);
            payroll.setUpdatedBy(appUser);

            hrPayrollRepository.save(payroll);

            // sesuai arahan Anda: belum insert ke hr_payroll_calculations dulu
            // calculatePayroll(payroll);
        }

        log.warn("createPayrollBulk called. instance={}, hrPersonRepositoryNull={}",
                System.identityHashCode(this),
                (this.hrPersonRepository == null));

    }

    private BigDecimal resolveOvertimeValue(AddPayrollRequest req) {
        if ("PERCENTAGE".equals(req.getOvertimePaymentType())) {
            Integer pct = req.getOvertimePercent() == null ? 0 : req.getOvertimePercent();
            return new BigDecimal(pct);
        }
        return req.getOvertimeStaticNominal() == null ? BigDecimal.ZERO : req.getOvertimeStaticNominal();
    }

    private BigDecimal resolveBenefitsPackageAllowanceTotal(HrPerson person, LocalDate payrollDate, HrPersonPosition personPosition) {
        if (personPosition == null || personPosition.getPosition() == null) return BigDecimal.ZERO;

        try {
            List<HrSalaryPositionAllowance> packages = hrSalaryPositionAllowanceRepository
                    .findByPositionAndCompanyOrderByUpdatedAtAsc(personPosition.getPosition(), personPosition.getCompany());

            List<HrSalaryPositionAllowance> activePackages = packages.stream()
                    .filter(p -> {
                        if (p.getStartDate() != null && payrollDate.isBefore(p.getStartDate())) return false;
                        if (p.getEndDate() == null) return true;
                        return !payrollDate.isAfter(p.getEndDate());
                    })
                    .toList();

            BigDecimal total = BigDecimal.ZERO;
            for (HrSalaryPositionAllowance p : activePackages) {
                HrSalaryAllowance a = p.getAllowance();
                if (a != null && a.getAmount() != null) {
                    total = total.add(a.getAmount());
                }
            }
            return total;
        } catch (Exception ex) {
            log.warn("Benefits package resolve failed for personId {}: {}", person.getId(), ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

}
