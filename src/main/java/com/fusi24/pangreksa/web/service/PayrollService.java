package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.repo.*;
import lombok.Getter;
import lombok.Setter;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollService {
    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private static final int SCALE_MONEY = 2;
    private static final BigDecimal BD_12 = BigDecimal.valueOf(12);
    private static final BigDecimal BD_100 = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository;
    private final HrSalaryAllowanceRepository hrSalaryAllowanceRepository;
    private final HrSalaryPositionAllowanceRepository hrSalaryPositionAllowanceRepository;
    private final FwAppUserRepository appUserRepository;

    private final HrPayrollRepository hrPayrollRepository;
    private final HrPayrollCalculationRepository hrPayrollCalculationRepository;
    private final HrPayrollComponentRepository hrPayrollComponentRepository;

    private final HrTaxBracketRepository hrTaxBracketRepository;
    private final HrSalaryEmployeeLevelRepository hrSalaryEmployeeLevelRepository;
    private final HrAttendanceRepository hrAttendanceRepository;
    private final FwSystemRepository fwSystemRepository;

    private final HrPersonPositionRepository hrPersonPositionRepository;
    private final HrPersonRespository hrPersonRepository;
    private final HrPersonPtkpRepository hrPersonPtkpRepository;
    private final HrLeaveApplicationRepository hrLeaveApplicationRepository;

    private final MasterPtkpRepository masterPtkpRepository;
    private final MasterTerRepository masterTerRepository;
    private final MasterTerTarifRepository masterTerTarifRepository;

    private final SystemService systemService;

    private FwAppUser appUser;

    @Autowired
    public PayrollService(HrSalaryBaseLevelRepository hrSalaryBaseLevelRepository,
                          FwAppUserRepository appUserRepository,
                          HrSalaryAllowanceRepository hrSalaryAllowanceRepository,
                          HrSalaryPositionAllowanceRepository hrSalaryPositionAllowanceRepository,
                          HrPayrollRepository hrPayrollRepository,
                          HrPayrollCalculationRepository hrPayrollCalculationRepository,
                          HrPayrollComponentRepository hrPayrollComponentRepository,
                          HrTaxBracketRepository hrTaxBracketRepository,
                          HrSalaryEmployeeLevelRepository hrSalaryEmployeeLevelRepository,
                          HrAttendanceRepository hrAttendanceRepository,
                          FwSystemRepository fwSystemRepository,
                          HrPersonPositionRepository hrPersonPositionRepository,
                          HrPersonRespository hrPersonRepository,
                          HrPersonPtkpRepository hrPersonPtkpRepository,
                          HrLeaveApplicationRepository hrLeaveApplicationRepository, MasterPtkpRepository masterPtkpRepository, MasterTerRepository masterTerRepository, MasterTerTarifRepository masterTerTarifRepository,
                          SystemService systemService) {

        this.hrSalaryBaseLevelRepository = hrSalaryBaseLevelRepository;
        this.appUserRepository = appUserRepository;
        this.hrSalaryAllowanceRepository = hrSalaryAllowanceRepository;
        this.hrSalaryPositionAllowanceRepository = hrSalaryPositionAllowanceRepository;
        this.hrPayrollRepository = hrPayrollRepository;
        this.hrPayrollCalculationRepository = hrPayrollCalculationRepository;
        this.hrPayrollComponentRepository = hrPayrollComponentRepository;
        this.hrTaxBracketRepository = hrTaxBracketRepository;
        this.hrSalaryEmployeeLevelRepository = hrSalaryEmployeeLevelRepository;
        this.hrAttendanceRepository = hrAttendanceRepository;
        this.fwSystemRepository = fwSystemRepository;
        this.hrPersonPositionRepository = hrPersonPositionRepository;
        this.hrPersonRepository = hrPersonRepository;
        this.hrPersonPtkpRepository = hrPersonPtkpRepository;
        this.hrLeaveApplicationRepository = hrLeaveApplicationRepository;
        this.masterPtkpRepository = masterPtkpRepository;
        this.masterTerRepository = masterTerRepository;
        this.masterTerTarifRepository = masterTerTarifRepository;
        this.systemService = systemService;
    }

    public void setUser(AppUserInfo appUserInfo) {
        this.appUser = findAppUserByUserId(appUserInfo.getUserId().toString());
    }

    private FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public void deleteSalaryAllowance(HrSalaryAllowance data) {
        hrSalaryAllowanceRepository.delete(data);
    }

    public List<HrSalaryBaseLevel> getAllSalaryBaseLevels(boolean includeInactive) {
        ensureUserSet();

        if (includeInactive) {
            return hrSalaryBaseLevelRepository.findByCompanyOrderByLevelCodeAsc(appUser.getCompany());
        }
        return hrSalaryBaseLevelRepository.findByCompanyAndIsActiveTrueOrderByLevelCodeAsc(appUser.getCompany());
    }

    public List<HrSalaryAllowance> getAllSalaryAllowances(boolean includeInactive) {
        ensureUserSet();

        if (includeInactive) {
            return hrSalaryAllowanceRepository.findByCompanyOrderByNameAsc(appUser.getCompany());
        }
        return hrSalaryAllowanceRepository.findByCompanyAndEndDateIsNullOrderByNameAsc(appUser.getCompany());
    }

    public List<HrSalaryPositionAllowance> getSalaryPositionAllowancesByPosition(HrPosition position, boolean includeInactive) {
        ensureUserSet();

        if (includeInactive) {
            return hrSalaryPositionAllowanceRepository.findByPositionAndCompanyOrderByUpdatedAtAsc(position, appUser.getCompany());
        }
        return hrSalaryPositionAllowanceRepository.findByPositionAndCompanyAndEndDateIsNullOrderByUpdatedAtAsc(position, appUser.getCompany());
    }

    public HrSalaryBaseLevel saveSalaryBaseLevel(HrSalaryBaseLevel salaryBaseLevel, AppUserInfo appUserInfo) {
        FwAppUser currentUser = findAppUserByUserId(appUserInfo.getUserId().toString());

        if (salaryBaseLevel.getId() == null) {
            salaryBaseLevel.setCreatedBy(currentUser);
            salaryBaseLevel.setUpdatedBy(currentUser);
            salaryBaseLevel.setCreatedAt(LocalDateTime.now());
            salaryBaseLevel.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryBaseLevel.setUpdatedBy(currentUser);
            salaryBaseLevel.setUpdatedAt(LocalDateTime.now());
        }

        return hrSalaryBaseLevelRepository.save(salaryBaseLevel);
    }

    public HrSalaryAllowance saveSalaryAllowance(HrSalaryAllowance salaryAllowance, AppUserInfo appUserInfo) {
        FwAppUser currentUser = findAppUserByUserId(appUserInfo.getUserId().toString());

        if (salaryAllowance.getId() == null) {
            if (salaryAllowance.getCompany() == null) {
                salaryAllowance.setCompany(currentUser.getCompany());
            }
            salaryAllowance.setCreatedBy(currentUser);
            salaryAllowance.setUpdatedBy(currentUser);
            salaryAllowance.setCreatedAt(LocalDateTime.now());
            salaryAllowance.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryAllowance.setUpdatedBy(currentUser);
            salaryAllowance.setUpdatedAt(LocalDateTime.now());
            if (salaryAllowance.getCompany() == null) {
                salaryAllowance.setCompany(currentUser.getCompany());
            }
        }

        return hrSalaryAllowanceRepository.save(salaryAllowance);
    }

    public HrSalaryPositionAllowance saveSalaryPositionAllowance(HrSalaryPositionAllowance salaryPositionAllowance, AppUserInfo appUserInfo) {
        FwAppUser currentUser = findAppUserByUserId(appUserInfo.getUserId().toString());

        if (salaryPositionAllowance.getId() == null) {
            salaryPositionAllowance.setCreatedBy(currentUser);
            salaryPositionAllowance.setUpdatedBy(currentUser);
            salaryPositionAllowance.setCreatedAt(LocalDateTime.now());
            salaryPositionAllowance.setUpdatedAt(LocalDateTime.now());
        } else {
            salaryPositionAllowance.setUpdatedBy(currentUser);
            salaryPositionAllowance.setUpdatedAt(LocalDateTime.now());
        }

        return hrSalaryPositionAllowanceRepository.save(salaryPositionAllowance);
    }

    @Transactional
    public void deletePayroll(HrPayroll data) {
        hrPayrollRepository.delete(data);
    }

    public Page<HrPayroll> getPayrollPage(Pageable pageable, Integer year, LocalDate month, String searchTerm) {
        ensureUserSet();
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
            return (root, query, cb) -> cb.conjunction();
        }

        String lower = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), lower),
                cb.like(cb.lower(root.get("lastName")), lower),
                cb.like(cb.lower(root.get("employeeNumber")), lower)
        );
    }

    public List<HrPerson> getActiveEmployees() {
        ensureUserSet();
        return hrPersonRepository.findAll();
    }

    public HrPayrollCalculation getCalculationByPayrollId(Long payrollId) {
        return hrPayrollCalculationRepository.findFirstByPayrollId(payrollId);
    }

    @Getter
    @Setter
    public static class AddPayrollRequest {
        private Integer year;
        private Integer month;
        private Integer paramAttendanceDays;
        private Integer overtimeMinutes;
        private String overtimePaymentType; // masih dipakai untuk input proses
        private BigDecimal overtimeStaticNominal;
        private Integer overtimePercent;
        private String allowanceMode; // NO ALLOWANCE / SELECT ALLOWANCE / BENEFITS PACKAGE
        private List<HrSalaryAllowance> selectedAllowances = new ArrayList<>();
    }

    public List<HrSalaryAllowance> getSelectableAllowancesForPayrollDate(LocalDate payrollDate) {
        return getAllSalaryAllowances(true).stream()
                .filter(a -> {
                    if (a.getStartDate() != null && payrollDate.isBefore(a.getStartDate())) return false;
                    return a.getEndDate() == null || !payrollDate.isAfter(a.getEndDate());
                })
                .toList();
    }

    @Transactional
    public void createPayrollBulk(AddPayrollRequest req, AppUserInfo userInfo) {
        FwAppUser currentUser = findAppUserByUserId(userInfo.getUserId().toString());

        LocalDate payrollDate = LocalDate.of(req.getYear(), req.getMonth(), 1);
        LocalDate monthStart = payrollDate.withDayOfMonth(1);
        LocalDate monthEndExclusive = monthStart.plusMonths(1);

        List<HrPerson> employees = hrPersonRepository.findAll();
        if (employees == null || employees.isEmpty()) {
            return;
        }

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (HrPerson person : employees) {
            try {
                List<HrPersonPtkp> activePtkps = hrPersonPtkpRepository
                        .findActiveListByPersonId(person.getId(), payrollDate);

                HrPersonPtkp activePtkp = activePtkps.isEmpty() ? null : activePtkps.get(0);

                if (activePtkp == null
                        || activePtkp.getPtkpCode() == null
                        || activePtkp.getPtkpCode().isBlank()) {
                    skipped++;
                    log.warn("SKIP PAYROLL → personId={} | PTKP not valid for payrollDate={}",
                            person.getId(), payrollDate);
                    continue;
                }

                if (hrPayrollRepository.existsByPersonIdAndPayrollDate(person.getId(), payrollDate)) {
                    skipped++;
                    continue;
                }

                HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(person.getId());
                if (personPosition == null) {
                    skipped++;
                    log.warn("SKIP PAYROLL → personId={} | No active position", person.getId());
                    continue;
                }

                HrPayroll payroll = buildPayrollHeader(
                        person,
                        personPosition,
                        payrollDate,
                        req,
                        currentUser,
                        monthStart,
                        monthEndExclusive
                );

                HrPayroll savedPayroll = hrPayrollRepository.save(payroll);

                generatePayroll(
                        savedPayroll,
                        personPosition,
                        req,
                        monthStart,
                        monthEndExclusive,
                        BigDecimal.ZERO,
                        nvl(savedPayroll.getOtherDeductions())
                );

                success++;

            } catch (Exception e) {
                failed++;
                log.error("FAILED PAYROLL → personId={} | error={}", person.getId(), e.getMessage(), e);
            }
        }

        log.info("PAYROLL RESULT → success={} | skipped={} | failed={}", success, skipped, failed);

        log.info("PAYROLL RESULT → success={} | skipped={} | failed={}", success, skipped, failed);

    }

    @Transactional
    public void recalculateSinglePayroll(Long payrollId,
                                         AddPayrollRequest req,
                                         BigDecimal totalBonus,
                                         BigDecimal totalOtherDeductions,
                                         BigDecimal ignoredTotalTaxable,
                                         AppUserInfo userInfo) {

        FwAppUser currentUser = findAppUserByUserId(userInfo.getUserId().toString());

        HrPayroll payroll = hrPayrollRepository.findById(payrollId)
                .orElseThrow(() -> new IllegalStateException("Payroll not found: " + payrollId));

        HrPerson person = payroll.getPerson();
        if (person == null) {
            throw new IllegalStateException("Payroll person is null for payrollId=" + payrollId);
        }

        HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(person.getId());
        if (personPosition == null) {
            throw new IllegalStateException("No active position found for personId=" + person.getId());
        }

        LocalDate monthStart = payroll.getPayrollDate().withDayOfMonth(1);
        LocalDate monthEndExclusive = monthStart.plusMonths(1);

        payroll.setParamAttendanceDays(req.getParamAttendanceDays());
        payroll.setOvertimeHours(toMoney(req.getOvertimeMinutes()));
        payroll.setBonusAmount(nvl(totalBonus));
        payroll.setOtherDeductions(nvl(totalOtherDeductions));
        payroll.setUpdatedBy(currentUser);
        payroll.setUpdatedAt(LocalDateTime.now());
        payroll.setSumAttendance(resolveSumAttendance(person.getId(), monthStart, monthEndExclusive));

        HrPayroll savedPayroll = hrPayrollRepository.save(payroll);

        HrPayrollCalculation currentCalc = hrPayrollCalculationRepository.findFirstByPayrollId(savedPayroll.getId());
        if (currentCalc != null) {
            hrPayrollComponentRepository.deleteByPayrollCalculationId(currentCalc.getId());
            hrPayrollCalculationRepository.delete(currentCalc);
        }

        generatePayroll(savedPayroll, personPosition, req, monthStart, monthEndExclusive, nvl(totalBonus), nvl(totalOtherDeductions));
    }

    @Transactional
    public void deletePayrolls(List<Long> payrollIds) {
        if (payrollIds == null || payrollIds.isEmpty()) return;

        hrPayrollCalculationRepository.deleteByPayrollIdIn(payrollIds);
        hrPayrollRepository.deleteByIds(payrollIds);
    }

    @Transactional
    public HrPayrollCalculation calculatePayrollForMonth(HrPayroll payrollInput,
                                                         LocalDate startOfMonth,
                                                         LocalDate startOfNextMonth) {
        HrPerson person = payrollInput.getPerson();
        if (person == null) {
            throw new IllegalArgumentException("Payroll person must not be null");
        }

        HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(person.getId());
        if (personPosition == null) {
            throw new IllegalStateException("No active position found for personId=" + person.getId());
        }

        AddPayrollRequest req = new AddPayrollRequest();
        req.setYear(payrollInput.getPayrollDate().getYear());
        req.setMonth(payrollInput.getPayrollDate().getMonthValue());
        req.setParamAttendanceDays(payrollInput.getParamAttendanceDays());
        req.setOvertimeMinutes(payrollInput.getOvertimeHours() == null ? 0 : payrollInput.getOvertimeHours().intValue());
        req.setAllowanceMode("BENEFITS PACKAGE");

        return generatePayroll(
                payrollInput,
                personPosition,
                req,
                startOfMonth,
                startOfNextMonth,
                nvl(payrollInput.getBonusAmount()),
                nvl(payrollInput.getOtherDeductions())
        );
    }

    @Transactional
    public HrPayrollCalculation calculatePayrollForMonth(HrPayroll payrollInput,
                                                         LocalDate startOfMonth,
                                                         LocalDate startOfNextMonth,
                                                         BigDecimal totalBonus,
                                                         BigDecimal totalOtherDeductions,
                                                         BigDecimal ignoredTotalTaxable) {
        HrPerson person = payrollInput.getPerson();
        if (person == null) {
            throw new IllegalArgumentException("Payroll person must not be null");
        }

        HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(person.getId());
        if (personPosition == null) {
            throw new IllegalStateException("No active position found for personId=" + person.getId());
        }

        AddPayrollRequest req = new AddPayrollRequest();
        req.setYear(payrollInput.getPayrollDate().getYear());
        req.setMonth(payrollInput.getPayrollDate().getMonthValue());
        req.setParamAttendanceDays(payrollInput.getParamAttendanceDays());
        req.setOvertimeMinutes(payrollInput.getOvertimeHours() == null ? 0 : payrollInput.getOvertimeHours().intValue());
        req.setAllowanceMode("BENEFITS PACKAGE");

        return generatePayroll(
                payrollInput,
                personPosition,
                req,
                startOfMonth,
                startOfNextMonth,
                nvl(totalBonus),
                nvl(totalOtherDeductions)
        );
    }

    @Transactional
    public HrPayrollCalculation generatePayroll(HrPayroll payrollInput,
                                                HrPersonPosition personPosition,
                                                AddPayrollRequest req,
                                                LocalDate startOfMonth,
                                                LocalDate endOfMonthExclusive,
                                                BigDecimal bonusAmount,
                                                BigDecimal otherDeductions) {

        if (payrollInput == null || payrollInput.getPerson() == null) {
            throw new IllegalArgumentException("Payroll input/person must not be null");
        }

        HrPerson person = payrollInput.getPerson();
        Long personId = person.getId();

        BigDecimal baseSalary = nvl(payrollInput.getBaseSalary());
        if (baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            baseSalary = resolveGrossSalary(personId);
            payrollInput.setBaseSalary(baseSalary);
        }

        payrollInput.setBonusAmount(nvl(bonusAmount));
        payrollInput.setOtherDeductions(nvl(otherDeductions));

        int sumAttendance = resolveSumAttendance(personId, startOfMonth, endOfMonthExclusive);
        payrollInput.setSumAttendance(sumAttendance);

        AllowanceResult allowanceResult = resolveAllowanceResult(req, personPosition, payrollInput.getPayrollDate());
        BigDecimal fixedAllowanceTotal = allowanceResult.fixedTotal;
        BigDecimal variableAllowanceTotal = allowanceResult.variableTotal;

        AttendanceDeductionResult attendanceDeduction = resolveAttendanceDeduction(
                variableAllowanceTotal,
                payrollInput.getParamAttendanceDays(),
                personId,
                startOfMonth,
                endOfMonthExclusive
        );

        BigDecimal overtimeAmount = resolveOvertimeAmount(payrollInput, req, startOfMonth, endOfMonthExclusive);
        payrollInput.setOvertimeAmount(overtimeAmount);

        // BPJS base mengikuti kebijakan: base salary + fixed allowance
        BigDecimal bpjsBase = baseSalary
                .add(fixedAllowanceTotal);

        BpjsResult bpjs = resolveBpjsResult(bpjsBase);

        List<HrPayrollComponent> previewComponents = buildPayrollComponents(
                null,
                baseSalary,
                allowanceResult,
                new AttendanceDeductionResult(ZERO, ZERO, 0, 0),
                bpjs,
                overtimeAmount,
                nvl(bonusAmount),
                ZERO
        );

        BigDecimal bpjsJkkCompany = sumComponentAmountByCodes(previewComponents, Set.of("BPJS_JKK_COMPANY"));
        BigDecimal bpjsJkCompany = sumComponentAmountByCodes(previewComponents, Set.of("BPJS_JK_COMPANY"));
        BigDecimal bpjsJknCompany = sumComponentAmountByCodes(previewComponents, Set.of("BPJS_JKN_COMPANY"));
        BigDecimal thrAmount = sumComponentAmountByCodes(previewComponents, Set.of("THR"));

        BigDecimal penghasilanTeraturAmount = baseSalary
                .add(fixedAllowanceTotal)
                .add(variableAllowanceTotal)
                .add(bpjsJkkCompany)
                .add(bpjsJkCompany)
                .add(bpjsJknCompany);

        BigDecimal terDppAmount = penghasilanTeraturAmount
                .add(overtimeAmount)
                .add(nvl(bonusAmount))
                .add(thrAmount);

        String terCategory = resolveJenisTer(payrollInput.getPtkpCode());
        BigDecimal terRatePercent = resolveTarifTer(terCategory, terDppAmount);
        BigDecimal pph21Deduction = calculatePph21Ter(terDppAmount, terRatePercent);

        BigDecimal insuranceAmount = bpjs.companyTkTotal
                .add(bpjs.companyJkn)
                .add(bpjs.employeeTkTotal)
                .add(bpjs.employeeJkn);

        BigDecimal grossSalary = baseSalary
                .add(fixedAllowanceTotal)
                .add(variableAllowanceTotal)
                .add(overtimeAmount)
                .add(nvl(bonusAmount))
                .add(bpjs.companyTkTotal)
                .add(bpjs.companyJkn)
                .add(bpjs.employeeJht)
                .add(bpjs.employeeJp)
                .add(bpjs.employeeJkk)
                .add(bpjs.employeeJk)
                .add(bpjs.employeeJkn);

        BigDecimal totalDeduction = attendanceDeduction.absenceDeduction
                .add(attendanceDeduction.lateDeduction)
                .add(bpjs.employeeJht)
                .add(bpjs.employeeJp)
                .add(bpjs.employeeJkk)
                .add(bpjs.employeeJk)
                .add(bpjs.employeeJkn)
                .add(pph21Deduction)
                .add(nvl(otherDeductions));

        BigDecimal netTakeHomePay = grossSalary
                .subtract(insuranceAmount)
                .subtract(pph21Deduction);

        if (netTakeHomePay.signum() < 0) {
            netTakeHomePay = ZERO;
        }

        HrPayrollCalculation current = hrPayrollCalculationRepository.findFirstByPayrollId(payrollInput.getId());

        HrPayrollCalculation calc = HrPayrollCalculation.builder()
                .id(current == null ? null : current.getId())
                .payroll(payrollInput)
                .baseSalary(scale(baseSalary))
                .fixedAllowanceTotal(scale(fixedAllowanceTotal))
                .variableAllowanceTotal(scale(variableAllowanceTotal))
                .overtimeAmount(scale(overtimeAmount))
                .bonusAmount(scale(nvl(bonusAmount)))
                .grossSalary(scale(grossSalary))
                .absenceDeduction(scale(attendanceDeduction.absenceDeduction))
                .lateDeduction(scale(attendanceDeduction.lateDeduction))
                .bpjsJhtDeduction(scale(bpjs.employeeJht))
                .bpjsJpDeduction(scale(bpjs.employeeJp))
                .bpjsJknDeduction(scale(bpjs.employeeJkn))
                .bpjsJhtCompany(scale(bpjs.companyJht))
                .bpjsJpCompany(scale(bpjs.companyJp))
                .bpjsJknCompany(scale(bpjs.companyJkn))
                .pph21Deduction(scale(pph21Deduction))
                .totalDeduction(scale(totalDeduction))
                .netTakeHomePay(scale(netTakeHomePay))
                .calculatedAt(LocalDateTime.now())
                .notes("Calculated")
                .penghasilanTeraturAmount(scale(penghasilanTeraturAmount))
                .terDppAmount(scale(terDppAmount))
                .terCategory(terCategory)
                .terRatePercent(scale(terRatePercent))
                .build();

        HrPayrollCalculation savedCalc = hrPayrollCalculationRepository.save(calc);

        hrPayrollComponentRepository.deleteByPayrollCalculationId(savedCalc.getId());
        hrPayrollComponentRepository.saveAll(
                buildPayrollComponents(
                        savedCalc,
                        baseSalary,
                        allowanceResult,
                        attendanceDeduction,
                        bpjs,
                        overtimeAmount,
                        nvl(bonusAmount),
                        pph21Deduction
                )
        );
        hrPayrollRepository.save(payrollInput);

        return savedCalc;
    }

    private HrPayroll buildPayrollHeader(HrPerson person,
                                         HrPersonPosition personPosition,
                                         LocalDate payrollDate,
                                         AddPayrollRequest req,
                                         FwAppUser currentUser,
                                         LocalDate startOfMonth,
                                         LocalDate endOfMonthExclusive) {

        HrPosition position = personPosition.getPosition();
        HrOrgStructure department = position != null ? position.getOrgStructure() : null;

        HrPersonPtkp activePtkp = hrPersonPtkpRepository
                .findActiveByPersonId(person.getId(), payrollDate)
                .orElse(null);

        String ptkpCode = activePtkp == null ? "NOT_SET" : activePtkp.getPtkpCode();
        BigDecimal ptkpYear = activePtkp == null ? ZERO : nvl(activePtkp.getPtkpAmount());
        BigDecimal ptkpMonth = activePtkp == null ? ZERO : ptkpYear.divide(BD_12, 2, RoundingMode.HALF_UP);

        BigDecimal baseSalary = resolveGrossSalary(person.getId());

        HrPayroll payroll = HrPayroll.builder()
                .person(person)
                .employeeNumber(resolveEmployeeNumber(person))
                .firstName(person.getFirstName())
                .middleName(person.getMiddleName())
                .lastName(person.getLastName())
                .position(position != null ? nvlString(position.getName()) : "")
                .positionCode(position != null ? nvlString(position.getCode()) : "")
                .department(department != null ? nvlString(department.getName()) : "")
                .departmentCode(department != null ? nvlString(department.getCode()) : "")
                .pob(person.getPob())
                .dob(person.getDob())
                .gender(person.getGender() == null ? null : person.getGender().name())
                .ktpNumber(person.getKtpNumber())
                .statusEmployee(resolveStatusEmployee(person))
                .joinDate(resolveJoinDate(person, personPosition))
                .payrollDate(payrollDate)
                .paramAttendanceDays(req.getParamAttendanceDays() == null ? 0 : req.getParamAttendanceDays())
                .baseSalary(scale(baseSalary))
                .overtimeHours(toMoney(req.getOvertimeMinutes()))
                .overtimeAmount(ZERO)
                .bonusAmount(ZERO)
                .otherDeductions(ZERO)
                .ptkpCode(ptkpCode)
                .ptkpAmount(scale(ptkpMonth))
                .sumAttendance(resolveSumAttendance(person.getId(), startOfMonth, endOfMonthExclusive))
                .build();

        payroll.setCreatedBy(currentUser);
        payroll.setUpdatedBy(currentUser);
        payroll.setCreatedAt(LocalDateTime.now());
        payroll.setUpdatedAt(LocalDateTime.now());

        return payroll;
    }

    private AllowanceResult resolveAllowanceResult(AddPayrollRequest req,
                                                   HrPersonPosition personPosition,
                                                   LocalDate payrollDate) {

        List<HrSalaryAllowance> selected = new ArrayList<>();

        String mode = req == null ? "" : nvlString(req.getAllowanceMode()).trim();

        if ("SELECT ALLOWANCE".equalsIgnoreCase(mode)) {
            if (req.getSelectedAllowances() != null) {
                selected.addAll(req.getSelectedAllowances().stream().filter(Objects::nonNull).toList());
            }
        } else if ("BENEFITS PACKAGE".equalsIgnoreCase(mode)) {
            if (personPosition != null && personPosition.getPosition() != null) {
                List<HrSalaryPositionAllowance> packages = hrSalaryPositionAllowanceRepository
                        .findByPositionAndCompanyOrderByUpdatedAtAsc(personPosition.getPosition(), personPosition.getCompany());

                for (HrSalaryPositionAllowance p : packages) {
                    if (p == null || p.getAllowance() == null) continue;
                    if (p.getStartDate() != null && payrollDate.isBefore(p.getStartDate())) continue;
                    if (p.getEndDate() != null && payrollDate.isAfter(p.getEndDate())) continue;
                    selected.add(p.getAllowance());
                }
            }
        }

        BigDecimal fixed = ZERO;
        BigDecimal variable = ZERO;
        List<HrSalaryAllowance> fixedItems = new ArrayList<>();
        List<HrSalaryAllowance> variableItems = new ArrayList<>();

        for (HrSalaryAllowance a : selected) {
            BigDecimal amount = scale(nvl(a.getAmount()));
            String type = nvlString(a.getAllowanceType());

            if ("FIXED".equalsIgnoreCase(type)) {
                fixed = fixed.add(amount);
                fixedItems.add(a);
            } else {
                variable = variable.add(amount);
                variableItems.add(a);
            }
        }

        return new AllowanceResult(scale(fixed), scale(variable), fixedItems, variableItems);
    }

    private AttendanceDeductionResult resolveAttendanceDeduction(BigDecimal variableAllowanceTotal,
                                                                 Integer paramAttendanceDays,
                                                                 Long personId,
                                                                 LocalDate startOfMonth,
                                                                 LocalDate endOfMonthExclusive) {

        BigDecimal variable = nvl(variableAllowanceTotal);
        if (variable.compareTo(BigDecimal.ZERO) <= 0) {
            return new AttendanceDeductionResult(ZERO, ZERO, 0, 0);
        }

        int paramDays = paramAttendanceDays == null ? 0 : paramAttendanceDays;
        if (paramDays <= 0) {
            return new AttendanceDeductionResult(ZERO, ZERO, 0, 0);
        }

        int alphaCount = resolveCountByStatuses(personId, startOfMonth, endOfMonthExclusive, List.of("ALPHA"));
        int lateCount = resolveCountByStatuses(personId, startOfMonth, endOfMonthExclusive, List.of("TERLAMBAT", "TERLAMBAT_DI_LUAR_LOKASI"));

        BigDecimal perDay = variable.divide(BigDecimal.valueOf(paramDays), 6, RoundingMode.HALF_UP);
        BigDecimal absenceDeduction = perDay.multiply(BigDecimal.valueOf(alphaCount));

        BigDecimal lateFactor = BigDecimal.valueOf(0.25); // 1 terlambat = 0.25 hari
        BigDecimal lateEquivalentDay = BigDecimal.valueOf(lateCount).multiply(lateFactor);
        BigDecimal lateDeduction = perDay.multiply(lateEquivalentDay);

        BigDecimal totalAttendanceDeduction = absenceDeduction.add(lateDeduction);
        if (totalAttendanceDeduction.compareTo(variable) > 0) {
            BigDecimal ratio = variable.divide(totalAttendanceDeduction, 6, RoundingMode.HALF_UP);
            absenceDeduction = absenceDeduction.multiply(ratio);
            lateDeduction = lateDeduction.multiply(ratio);
        }

        return new AttendanceDeductionResult(scale(absenceDeduction), scale(lateDeduction), alphaCount, lateCount);
    }

    private BigDecimal resolveOvertimeAmount(HrPayroll payrollInput,
                                             AddPayrollRequest req,
                                             LocalDate startOfMonth,
                                             LocalDate endOfMonthExclusive) {

        if (payrollInput == null || payrollInput.getPerson() == null) return ZERO;

        Integer overtimeMinutesParam = req == null ? null : req.getOvertimeMinutes();
        if (overtimeMinutesParam == null || overtimeMinutesParam <= 0) {
            return ZERO;
        }

        List<HrAttendance> overtimeAttendances = hrAttendanceRepository.findOvertimeByPersonAndPeriod(
                payrollInput.getPerson().getId(),
                startOfMonth,
                endOfMonthExclusive
        );

        if (overtimeAttendances == null || overtimeAttendances.isEmpty()) {
            return ZERO;
        }

        long totalMinutes = 0L;
        for (HrAttendance a : overtimeAttendances) {
            if (a.getCheckOut() == null) continue;
            if (a.getWorkSchedule() == null) continue;
            if (a.getWorkSchedule().getCheckOut() == null) continue;
            if (a.getAttendanceDate() == null) continue;

            LocalTime scheduleOut = a.getWorkSchedule().getCheckOut();
            LocalDateTime scheduledOut = LocalDateTime.of(a.getAttendanceDate(), scheduleOut);

            if (a.getCheckOut().isAfter(scheduledOut)) {
                totalMinutes += Duration.between(scheduledOut, a.getCheckOut()).toMinutes();
            }
        }

        if (totalMinutes <= 0) return ZERO;

        BigDecimal multiplier = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(overtimeMinutesParam), 0, RoundingMode.DOWN);

        if (multiplier.compareTo(BigDecimal.ZERO) <= 0) return ZERO;

        BigDecimal unitAmount;
        String paymentType = req.getOvertimePaymentType() == null ? "STATIC" : req.getOvertimePaymentType();

        if ("PERCENTAGE".equalsIgnoreCase(paymentType)) {
            int pct = req.getOvertimePercent() == null ? 0 : req.getOvertimePercent();
            unitAmount = payrollInput.getBaseSalary()
                    .multiply(BigDecimal.valueOf(pct))
                    .divide(BD_100, 2, RoundingMode.HALF_UP);
        } else {
            unitAmount = nvl(req.getOvertimeStaticNominal());
        }

        return scale(unitAmount.multiply(multiplier));
    }

    private BpjsResult resolveBpjsResult(BigDecimal baseForBpjs) {
        BigDecimal gross = nvl(baseForBpjs);
        if (gross.signum() <= 0) {
            return BpjsResult.zero();
        }

        Map<Integer, FwSystem> cfg = fwSystemRepository
                .findBySortOrderIn(Set.of(100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 113, 114))
                .stream()
                .collect(Collectors.toMap(FwSystem::getSortOrder, x -> x, (a, b) -> a));

        BigDecimal companyJhtRate = getDecimalVal(cfg.get(100));
        BigDecimal employeeJhtRate = getDecimalVal(cfg.get(101));

        BigDecimal companyJpRate = getDecimalVal(cfg.get(102));
        BigDecimal employeeJpRate = getDecimalVal(cfg.get(103));

        BigDecimal companyJkkRate = getDecimalVal(cfg.get(104));
        BigDecimal employeeJkkRate = getDecimalVal(cfg.get(106));

        BigDecimal companyJkRate = getDecimalVal(cfg.get(107));
        BigDecimal employeeJkRate = getDecimalVal(cfg.get(108));

        BigDecimal companyJknRate = getDecimalVal(cfg.get(109));
        BigDecimal employeeJknRate = getDecimalVal(cfg.get(110));

        BigDecimal capJkn = parseDecimal(cfg.get(113) == null ? null : cfg.get(113).getStringVal());
        BigDecimal capJp = parseDecimal(cfg.get(114) == null ? null : cfg.get(114).getStringVal());

        BigDecimal baseJht = gross;
        BigDecimal baseJp = (capJp == null || capJp.signum() <= 0) ? gross : gross.min(capJp);
        BigDecimal baseJkn = (capJkn == null || capJkn.signum() <= 0) ? gross : gross.min(capJkn);

        // JKK & JK/JKM pakai gross/base salary seperti JHT
        BigDecimal baseJkk = gross;
        BigDecimal baseJk = gross;

        BigDecimal employeeJht = percentOf(baseJht, employeeJhtRate);
        BigDecimal employeeJp = percentOf(baseJp, employeeJpRate);
        BigDecimal employeeJkk = percentOf(baseJkk, employeeJkkRate);
        BigDecimal employeeJk = percentOf(baseJk, employeeJkRate);
        BigDecimal employeeJkn = percentOf(baseJkn, employeeJknRate);

        BigDecimal companyJht = percentOf(baseJht, companyJhtRate);
        BigDecimal companyJp = percentOf(baseJp, companyJpRate);
        BigDecimal companyJkk = percentOf(baseJkk, companyJkkRate);
        BigDecimal companyJk = percentOf(baseJk, companyJkRate);
        BigDecimal companyJkn = percentOf(baseJkn, companyJknRate);

        return new BpjsResult(
                scale(employeeJht),
                scale(employeeJp),
                scale(employeeJkk),
                scale(employeeJk),
                scale(employeeJkn),
                scale(companyJht),
                scale(companyJp),
                scale(companyJkk),
                scale(companyJk),
                scale(companyJkn)
        );
    }

    private List<HrPayrollComponent> buildPayrollComponents(HrPayrollCalculation calc,
                                                            BigDecimal baseSalary,
                                                            AllowanceResult allowanceResult,
                                                            AttendanceDeductionResult attendanceDeduction,
                                                            BpjsResult bpjs,
                                                            BigDecimal overtimeAmount,
                                                            BigDecimal bonusAmount,
                                                            BigDecimal pph21Deduction) {

        List<HrPayrollComponent> components = new ArrayList<>();

        int sort = 1;

        components.add(component(calc, "EARNING", "BASE", "GAPOK", "Gaji Pokok", baseSalary, sort++));

        for (HrSalaryAllowance a : allowanceResult.fixedItems) {
            components.add(component(
                    calc,
                    "EARNING",
                    "FIXED_ALLOWANCE",
                    safeCode(a.getName()),
                    a.getName(),
                    scale(nvl(a.getAmount())),
                    sort++
            ));
        }

        for (HrSalaryAllowance a : allowanceResult.variableItems) {
            components.add(component(
                    calc,
                    "EARNING",
                    "VARIABLE_ALLOWANCE",
                    safeCode(a.getName()),
                    a.getName(),
                    scale(nvl(a.getAmount())),
                    sort++
            ));
        }

        if (nvl(overtimeAmount).compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "OVERTIME", "OVERTIME", "Lembur", scale(overtimeAmount), sort++));
        }

        if (nvl(bonusAmount).compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "BONUS", "BONUS", "Bonus", scale(bonusAmount), sort++));
        }

        if (bpjs.companyJht.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "BPJS_COMPANY", "BPJS_JHT_COMPANY", "BPJS TK JHT (Perusahaan)", bpjs.companyJht, sort++));
        }

        if (bpjs.companyJp.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "BPJS_COMPANY", "BPJS_JP_COMPANY", "BPJS TK JP (Perusahaan)", bpjs.companyJp, sort++));
        }

        if (bpjs.companyJkk.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "BPJS_COMPANY", "BPJS_JKK_COMPANY", "BPJS TK JKK (Perusahaan)", bpjs.companyJkk, sort++));
        }

        if (bpjs.companyJk.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "BPJS_COMPANY", "BPJS_JK_COMPANY", "BPJS TK JK (Perusahaan)", bpjs.companyJk, sort++));
        }

        if (bpjs.companyJkn.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "EARNING", "BPJS_COMPANY", "BPJS_JKN_COMPANY", "BPJS JKN (Perusahaan)", bpjs.companyJkn, sort++));
        }

        if (attendanceDeduction.absenceDeduction.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "ATTENDANCE", "ABSENCE", "Absen", attendanceDeduction.absenceDeduction, sort++));
        }

        if (attendanceDeduction.lateDeduction.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "ATTENDANCE", "LATE", "Terlambat", attendanceDeduction.lateDeduction, sort++));
        }

        if (bpjs.employeeJht.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "BPJS", "BPJS_JHT", "BPJS TK JHT", bpjs.employeeJht, sort++));
        }

        if (bpjs.employeeJp.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "BPJS", "BPJS_JP", "BPJS TK JP", bpjs.employeeJp, sort++));
        }

        if (bpjs.employeeJkk.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "BPJS", "BPJS_JKK", "BPJS TK JKK", bpjs.employeeJkk, sort++));
        }

        if (bpjs.employeeJk.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "BPJS", "BPJS_JK", "BPJS TK JK", bpjs.employeeJk, sort++));
        }

        if (bpjs.employeeJkn.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "BPJS", "BPJS_JKN", "BPJS JKN", bpjs.employeeJkn, sort++));
        }

        if (nvl(pph21Deduction).compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(calc, "DEDUCTION", "TAX", "PPH21", "PPh 21", scale(pph21Deduction), sort++));
        }

        return components;
    }

    private HrPayrollComponent component(HrPayrollCalculation calc,
                                         String type,
                                         String group,
                                         String code,
                                         String name,
                                         BigDecimal amount,
                                         int sortOrder) {
        return HrPayrollComponent.builder()
                .payrollCalculation(calc)
                .componentType(type)
                .componentGroup(group)
                .componentCode(code)
                .componentName(name)
                .amount(scale(nvl(amount)))
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private BigDecimal resolveGrossSalary(Long personId) {
        try {
            FwAppUser fw = appUserRepository.findByPersonId(personId).orElse(null);
            if (fw == null) return ZERO;

            HrSalaryEmployeeLevel sel = hrSalaryEmployeeLevelRepository.findByAppUserId(fw.getId()).orElse(null);
            if (sel == null || sel.getBaseLevel() == null || sel.getBaseLevel().getBaseSalary() == null) {
                return ZERO;
            }

            return scale(sel.getBaseLevel().getBaseSalary());
        } catch (Exception ex) {
            log.warn("resolveGrossSalary failed for personId {}: {}", personId, ex.getMessage());
            return ZERO;
        }
    }

    private int resolveSumAttendance(Long personId, LocalDate startOfMonth, LocalDate endOfMonthExclusive) {
        if (personId == null) return 0;
        try {
            long c = hrAttendanceRepository.countAttendanceByPersonAndPeriod(personId, startOfMonth, endOfMonthExclusive);
            return (int) Math.max(0, c);
        } catch (Exception ex) {
            log.warn("resolveSumAttendance failed personId {}: {}", personId, ex.getMessage());
            return 0;
        }
    }

    private int resolveCountByStatuses(Long personId, LocalDate startOfMonth, LocalDate endOfMonthExclusive, List<String> statuses) {
        if (personId == null || statuses == null || statuses.isEmpty()) return 0;
        try {
            long c = hrAttendanceRepository.countByPersonAndPeriodAndStatuses(personId, startOfMonth, endOfMonthExclusive, statuses);
            return (int) Math.max(0, c);
        } catch (Exception ex) {
            log.warn("resolveCountByStatuses failed personId {} statuses {}: {}", personId, statuses, ex.getMessage());
            return 0;
        }
    }

    private BigDecimal getDecimalVal(FwSystem s) {
        if (s == null || s.getDecimalVal() == null) return ZERO;
        return scale(s.getDecimalVal());
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal ratePercent) {
        if (amount == null || ratePercent == null) return ZERO;
        if (amount.signum() <= 0 || ratePercent.signum() <= 0) return ZERO;

        return scale(amount.multiply(ratePercent).divide(BD_100, 2, RoundingMode.HALF_UP));
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        s = s.replace(" ", "");
        s = s.replace(",", "");
        s = s.replace(".", "");

        try {
            return new BigDecimal(s);
        } catch (Exception ex) {
            return null;
        }
    }
    private String resolveEmployeeNumber(HrPerson person) {
        if (person == null || person.getNip() == null) {
            return "";
        }
        return person.getNip().trim();
    }

    private String resolveStatusEmployee(HrPerson person) {
        try {
            return person.getStatusEmployee();
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate resolveJoinDate(HrPerson person, HrPersonPosition personPosition) {
        if (personPosition != null) {
            return personPosition.getStartDate();
        }
        return null;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    private BigDecimal scale(BigDecimal v) {
        return (v == null ? ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toMoney(Integer val) {
        return val == null ? ZERO : BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    private String nvlString(String s) {
        return s == null ? "" : s;
    }

    private String safeCode(String s) {
        return nvlString(s).trim().toUpperCase().replace(" ", "_");
    }

    private void ensureUserSet() {
        if (appUser == null) {
            throw new IllegalStateException("App user is not set. Please call setUser() before using this method.");
        }
    }

    private static class AllowanceResult {
        private final BigDecimal fixedTotal;
        private final BigDecimal variableTotal;
        private final List<HrSalaryAllowance> fixedItems;
        private final List<HrSalaryAllowance> variableItems;

        private AllowanceResult(BigDecimal fixedTotal,
                                BigDecimal variableTotal,
                                List<HrSalaryAllowance> fixedItems,
                                List<HrSalaryAllowance> variableItems) {
            this.fixedTotal = fixedTotal;
            this.variableTotal = variableTotal;
            this.fixedItems = fixedItems;
            this.variableItems = variableItems;
        }
    }

    private static class AttendanceDeductionResult {
        private final BigDecimal absenceDeduction;
        private final BigDecimal lateDeduction;
        private final int alphaCount;
        private final int lateCount;

        private AttendanceDeductionResult(BigDecimal absenceDeduction,
                                          BigDecimal lateDeduction,
                                          int alphaCount,
                                          int lateCount) {
            this.absenceDeduction = absenceDeduction;
            this.lateDeduction = lateDeduction;
            this.alphaCount = alphaCount;
            this.lateCount = lateCount;
        }
    }

    private static class BpjsResult {
        private final BigDecimal employeeJht;
        private final BigDecimal employeeJp;
        private final BigDecimal employeeJkk;
        private final BigDecimal employeeJk;
        private final BigDecimal employeeJkn;

        private final BigDecimal companyJht;
        private final BigDecimal companyJp;
        private final BigDecimal companyJkk;
        private final BigDecimal companyJk;
        private final BigDecimal companyJkn;

        private final BigDecimal companyTkTotal;
        private final BigDecimal employeeTkTotal;

        private BpjsResult(BigDecimal employeeJht,
                           BigDecimal employeeJp,
                           BigDecimal employeeJkk,
                           BigDecimal employeeJk,
                           BigDecimal employeeJkn,
                           BigDecimal companyJht,
                           BigDecimal companyJp,
                           BigDecimal companyJkk,
                           BigDecimal companyJk,
                           BigDecimal companyJkn) {
            this.employeeJht = employeeJht;
            this.employeeJp = employeeJp;
            this.employeeJkk = employeeJkk;
            this.employeeJk = employeeJk;
            this.employeeJkn = employeeJkn;

            this.companyJht = companyJht;
            this.companyJp = companyJp;
            this.companyJkk = companyJkk;
            this.companyJk = companyJk;
            this.companyJkn = companyJkn;

            this.companyTkTotal = companyJht.add(companyJp).add(companyJkk).add(companyJk);
            this.employeeTkTotal = employeeJht.add(employeeJp).add(employeeJkk).add(employeeJk);
        }

        private static BpjsResult zero() {
            return new BpjsResult(
                    ZERO, ZERO, ZERO, ZERO, ZERO,
                    ZERO, ZERO, ZERO, ZERO, ZERO
            );
        }
    }

    public List<HrPayrollComponent> getPayrollComponentsByCalculationId(Long calculationId) {
        return hrPayrollComponentRepository.findByPayrollCalculationId(calculationId);
    }

    private BigDecimal sumComponentAmountByCodes(List<HrPayrollComponent> components, Set<String> codes) {
        if (components == null || components.isEmpty()) {
            return ZERO;
        }

        return components.stream()
                .filter(c -> c.getComponentCode() != null)
                .filter(c -> codes.contains(c.getComponentCode().toUpperCase()))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolveJenisTer(String ptkpCode) {
        if (ptkpCode == null
                || ptkpCode.isBlank()
                || "NOT_SET".equalsIgnoreCase(ptkpCode.trim())) {

            throw new IllegalStateException("PTKP code is invalid for TER calculation: " + ptkpCode);
        }

        MasterPtkp masterPtkp = masterPtkpRepository
                .findFirstByKodePtkpAndAktifTrue(ptkpCode.trim())
                .orElseThrow(() -> new IllegalStateException(
                        "Master PTKP not found for kode_ptkp=" + ptkpCode));

        MasterTer masterTer = masterTerRepository
                .findFirstByMasterPtkpIdAndAktifTrue(masterPtkp.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Master TER not found for master_ptkp_id=" + masterPtkp.getId()));

        return masterTer.getJenisTer();
    }

    private BigDecimal resolveTarifTer(String jenisTer, BigDecimal dppTerAmount) {
        MasterTerTarif tarif = masterTerTarifRepository.findEffectiveTarif(jenisTer, nvl(dppTerAmount))
                .orElseThrow(() -> new IllegalStateException(
                        "Master TER tarif not found for jenisTer=" + jenisTer + ", bruto=" + nvl(dppTerAmount)
                ));

        return scale(nvl(tarif.getTarifPersen()));
    }

    private BigDecimal calculatePph21Ter(BigDecimal dppTerAmount, BigDecimal tarifPersen) {
        if (nvl(dppTerAmount).signum() <= 0 || nvl(tarifPersen).signum() <= 0) {
            return ZERO;
        }

        return scale(
                nvl(dppTerAmount)
                        .multiply(nvl(tarifPersen))
                        .divide(BD_100, 2, RoundingMode.HALF_UP)
        );
    }

}