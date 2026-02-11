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
import com.fusi24.pangreksa.web.repo.HrAttendanceRepository;
import com.fusi24.pangreksa.web.repo.FwSystemRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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
    private final HrAttendanceRepository hrAttendanceRepository;
    private final FwSystemRepository fwSystemRepository;

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
                          HrLeaveApplicationRepository hrLeaveApplicationRepository,
                          HrAttendanceRepository hrAttendanceRepository,
                          FwSystemRepository fwSystemRepository) {

        this.hrSalaryBaseLevelRepository = hrSalaryBaseLevelRepository;
        this.appUserRepository = appUserRepository;
        this.hrSalaryAllowanceRepository = hrSalaryAllowanceRepository;
        this.hrSalaryPositionAllowanceRepository = hrSalaryAllowancePackageRepository;
        this.hrAttendanceRepository = hrAttendanceRepository;
        this.fwSystemRepository = fwSystemRepository;

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
    public void deletePayroll(HrPayroll data) {
        hrPayrollRepository.delete(data);
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

    // ==============================
    // GRID DATA (pagination)
    // ==============================
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
            return (root, query, cb) -> cb.conjunction();
        }

        String lowerCaseSearchTerm = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), lowerCaseSearchTerm),
                cb.like(cb.lower(root.get("lastName")), lowerCaseSearchTerm)
        );
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

    // ==============================
    // REQUEST MODEL
    // ==============================
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

        // "NO ALLOWANCE" / "SELECT ALLOWANCE" / "BENEFITS PACKAGE"
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

    // ==============================
    // BULK CREATE (anti duplicate)
    // ==============================
    @Transactional
    public void createPayrollBulk(AddPayrollRequest req, AppUserInfo userInfo) {
        FwAppUser appUser = this.findAppUserByUserId(userInfo.getUserId().toString());

        LocalDate payrollDate = LocalDate.of(req.getYear(), req.getMonth(), 1);
        LocalDate startOfMonth = payrollDate;
        LocalDate startOfNextMonth = payrollDate.plusMonths(1);

        // NOTE: sesuai kondisi kamu sebelumnya, person yang ikut diproses adalah yang punya posisi aktif.
        // Jika kamu ingin semua hr_person tanpa kecuali, ganti ke hrPersonRepository.findAll().
        List<HrPerson> employees = hrPersonRepository.findAll();
        if (employees == null || employees.isEmpty()) return;

        LocalDate yearStart = LocalDate.of(req.getYear(), 1, 1);
        LocalDate yearEnd = yearStart.plusYears(1);

        for (HrPerson person : employees) {

            // Anti duplicate per bulan
            if (hrPayrollRepository.existsByPersonIdAndPayrollDate(person.getId(), payrollDate)) {
                continue; // skip kalau sudah ada payroll bulan tsb
            }

            HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(person.getId());
            if (personPosition == null) {
                // ini menjelaskan kenapa jumlah payroll lebih sedikit daripada hr_person:
                // person yang tidak punya posisi aktif / mapping position tidak dibuatkan payroll.
                continue;
            }

            HrPosition position = personPosition.getPosition();
            HrOrgStructure department = position != null ? position.getOrgStructure() : null;

            // PTKP aktif → simpan BULANAN (dibagi 12)
            HrPersonPtkp activePtkp = hrPersonPtkpRepository
                    .findActiveByPersonId(person.getId(), payrollDate)
                    .orElse(null);

            String ptkpCode = activePtkp == null ? "NOT_SET" : activePtkp.getPtkpCode();

            BigDecimal ptkpYear = activePtkp == null ? BigDecimal.ZERO : nvl(activePtkp.getPtkpAmount());
            BigDecimal ptkpMonth = (activePtkp == null)
                    ? BigDecimal.ZERO
                    : ptkpYear.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            // Total cuti tahun berjalan (APPROVED)
            BigDecimal totalLeaveYear = BigDecimal.valueOf(
                    hrLeaveApplicationRepository.sumLeaveDaysByPersonAndPeriodAndStatuses(
                            person.getId(),
                            yearStart,
                            yearEnd,
                            List.of(LeaveStatusEnum.APPROVED)
                    )
            );

            // Allowance value text (delimiter |)
            String allowancesValueText = buildAllowanceValueTextForPerson(req, person, payrollDate, personPosition);


            // Overtime value payment text: "pct | amount"
            String overtimeValuePaymentText = buildOvertimeValuePaymentText(req, person);

            HrPayroll payroll = HrPayroll.builder()
                    .person(person)

                    .firstName(person.getFirstName())
                    .middleName(person.getMiddleName())
                    .lastName(person.getLastName())
                    .pob(person.getPob())
                    .dob(person.getDob())
                    .gender(person.getGender() == null ? null : person.getGender().name())
                    .ktpNumber(person.getKtpNumber())

                    .position(position != null ? position.getName() : "")
                    .positionCode(position != null ? position.getCode() : "")

                    .department(department != null ? department.getName() : "")
                    .departmentCode(department != null ? department.getCode() : "")

                    .payrollDate(payrollDate)
                    .paramAttendanceDays(req.getParamAttendanceDays())

                    .allowancesType(req.getAllowanceMode())
                    .allowancesValue(allowancesValueText)

                    // overtime_hours = menit 0..60
                    .overtimeHours(BigDecimal.valueOf(req.getOvertimeMinutes() == null ? 0L : req.getOvertimeMinutes().longValue()))
                    .overtimeType(req.getOvertimePaymentType())
                    .overtimeValuePayment(overtimeValuePaymentText) // <-- butuh kolom text di DB

                    // default 0, nanti via Recalculate/Add Component
                    .annualBonus(BigDecimal.ZERO)
                    .otherDeductions(BigDecimal.ZERO)

                    .ptkpCode(ptkpCode)
                    .ptkpAmount(ptkpMonth) // <-- BULANAN
                    .totalLeaveYear(totalLeaveYear)
                    .build();

            payroll.setCreatedBy(appUser);
            payroll.setUpdatedBy(appUser);

            int sumAttendance = resolveSumAttendance(payroll.getPerson().getId(), startOfMonth, startOfNextMonth);
            payroll.setSumAttendance(sumAttendance);

            HrPayroll saved = hrPayrollRepository.save(payroll);

            // Sekarang langsung create calculations agar grid muncul datanya
            calculatePayrollForMonth(saved, startOfMonth, startOfNextMonth);
        }
    }

    // ==============================
    // SINGLE RECALCULATE (update)
    // ==============================
    @Transactional
    public void recalculateSinglePayroll(Long payrollId,
                                         AddPayrollRequest req,
                                         BigDecimal totalBonus,
                                         BigDecimal totalOtherDeductions,
                                         BigDecimal totalTaxable,
                                         AppUserInfo userInfo) {

        FwAppUser appUser = this.findAppUserByUserId(userInfo.getUserId().toString());

        HrPayroll payroll = hrPayrollRepository.findById(payrollId)
                .orElseThrow(() -> new IllegalStateException("Payroll not found: " + payrollId));

        LocalDate payrollDate = payroll.getPayrollDate();
        LocalDate startOfMonth = payrollDate.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        // Update request-driven fields
        payroll.setParamAttendanceDays(req.getParamAttendanceDays());

        int sumAttendance = resolveSumAttendance(payroll.getPerson().getId(), startOfMonth, startOfNextMonth);
        payroll.setSumAttendance(sumAttendance);

        payroll.setAllowancesType(req.getAllowanceMode());

        HrPersonPosition personPosition = hrPersonPositionRepository.findFirstByPersonId(payroll.getPerson().getId());
        String allowancesValueText = buildAllowanceValueTextForPerson(req, payroll.getPerson(), payroll.getPayrollDate(), personPosition);

        payroll.setAllowancesValue(allowancesValueText);

        payroll.setOvertimeHours(BigDecimal.valueOf(req.getOvertimeMinutes() == null ? 0L : req.getOvertimeMinutes().longValue()));
        payroll.setOvertimeType(req.getOvertimePaymentType());
        payroll.setOvertimeValuePayment(buildOvertimeValuePaymentText(req, payroll.getPerson()));

        payroll.setUpdatedBy(appUser);
        hrPayrollRepository.save(payroll);

        // Update calculation (bonus/deduct/taxable) then recompute net
        calculatePayrollForMonth(payroll, startOfMonth, startOfNextMonth, totalBonus, totalOtherDeductions, totalTaxable);
    }

    // ==============================
    // BULK DELETE
    // ==============================
    @Transactional
    public void deletePayrolls(List<Long> payrollIds) {
        if (payrollIds == null || payrollIds.isEmpty()) return;

        hrPayrollCalculationRepository.deleteByPayrollIds(payrollIds);
        hrPayrollRepository.deleteByIds(payrollIds);
    }

    // ==============================
    // CORE CALCULATION (MONTH-SCOPED)
    // ==============================
    @Transactional
    public HrPayrollCalculation calculatePayrollForMonth(HrPayroll payrollInput,
                                                         LocalDate startOfMonth,
                                                         LocalDate startOfNextMonth) {
        return calculatePayrollForMonth(payrollInput, startOfMonth, startOfNextMonth,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional
    public HrPayrollCalculation calculatePayrollForMonth(HrPayroll payrollInput,
                                                         LocalDate startOfMonth,
                                                         LocalDate startOfNextMonth,
                                                         BigDecimal totalBonus,
                                                         BigDecimal totalOtherDeductions,
                                                         BigDecimal totalTaxable) {

        if (payrollInput == null || payrollInput.getPerson() == null) {
            throw new IllegalArgumentException("Payroll input/person must not be null");
        }

        BigDecimal grossSalary = resolveGrossSalary(payrollInput.getPerson().getId());
        BigDecimal ptkpMonth   = nvl(payrollInput.getPtkpAmount());

        Integer sumAttendance = payrollInput.getSumAttendance(); // sudah diisi saat create/recalc
        Integer paramAttendanceDays = payrollInput.getParamAttendanceDays();
        BigDecimal realGrossSalary = computeRealGrossDeduction(grossSalary, sumAttendance, paramAttendanceDays);

        BigDecimal computedTaxable;
        if (ptkpMonth.compareTo(BigDecimal.ZERO) <= 0) {
            // PTKP expired / tidak ada → anggap tidak kena pajak
            computedTaxable = BigDecimal.ZERO;
        } else {
            computedTaxable = grossSalary.subtract(ptkpMonth);
            if (computedTaxable.compareTo(BigDecimal.ZERO) < 0) {
                computedTaxable = BigDecimal.ZERO;
            }
        }

        BigDecimal totalAllowances = parseAllowanceTotal(payrollInput);                // poin 2A
        BigDecimal totalOvertimes = calculateOvertimeTotal(payrollInput, startOfMonth, startOfNextMonth); // poin 2B

        BigDecimal bonus = nvl(totalBonus);
        BigDecimal otherDed = nvl(totalOtherDeductions);

        // potongan jaminan kesehatan (rupiah)
        BigDecimal healthDeduction = resolveHealthInsuranceDeduction(grossSalary);

        // THP final (sesuai struktur perhitungan kamu)
        BigDecimal netTakeHomePay = grossSalary
                .add(totalAllowances)
                .add(totalOvertimes)
                .add(bonus)
                .subtract(otherDed)
                .subtract(computedTaxable)
                .subtract(healthDeduction);

        BigDecimal enhancedNetTakeHomePay = netTakeHomePay.subtract(realGrossSalary);

        HrPayrollCalculation current = hrPayrollCalculationRepository.findFirstByPayrollInputId(payrollInput.getId());

        HrPayrollCalculation calculation = HrPayrollCalculation.builder()
                .id(current == null ? null : current.getId())
                .payrollInput(payrollInput)
                .grossSalary(grossSalary)
                .totalAllowances(totalAllowances)
                .totalOvertimes(totalOvertimes)
                .totalBonus(bonus)
                .totalOtherDeductions(otherDed)

                // simpan hasil gross-ptkp ke DB
                .totalTaxable(computedTaxable)

                // simpan health deduction ke DB
                .healthDeduction(healthDeduction)

                .realGrossSalary(realGrossSalary) // <-- NEW
                .netTakeHomePay(enhancedNetTakeHomePay) // <-- NEW

                .calculatedAt(LocalDateTime.now())
                .notes("Calculated")
                .build();

        return hrPayrollCalculationRepository.save(calculation);
    }

    // ==============================
    // HELPERS
    // ==============================
    private BigDecimal resolveGrossSalary(Long personId) {
        // join mapping: fw_appuser.person_id -> hr_salary_employee_level.id_fwuser -> hr_salary_base_level.base_salary
        try {
            FwAppUser fw = appUserRepository.findByPersonId(personId).orElse(null);
            if (fw == null) return BigDecimal.ZERO;

            HrSalaryEmployeeLevel sel = hrSalaryEmployeeLevelRepository.findByAppUserId(fw.getId()).orElse(null);
            if (sel == null || sel.getBaseLevel() == null) return BigDecimal.ZERO;

            return nvl(sel.getBaseLevel().getBaseSalary());
        } catch (Exception ex) {
            log.warn("Resolve gross salary failed for personId {}: {}", personId, ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private String buildAllowanceValueText(AddPayrollRequest req) {
        if (req == null || req.getAllowanceMode() == null) return "0";

        if ("NO ALLOWANCE".equals(req.getAllowanceMode())) {
            return "0";
        }

        if ("SELECT ALLOWANCE".equals(req.getAllowanceMode())) {
            if (req.getSelectedAllowances() == null || req.getSelectedAllowances().isEmpty()) return "0";

            // format: "NAME:AMOUNT|NAME:AMOUNT|..."
            StringBuilder sb = new StringBuilder();
            for (HrSalaryAllowance a : req.getSelectedAllowances()) {
                if (a == null) continue;
                String nm = a.getName() == null ? "" : a.getName();
                BigDecimal amt = a.getAmount() == null ? BigDecimal.ZERO : a.getAmount();
                if (!sb.isEmpty()) sb.append("|");
                sb.append(nm).append(":").append(amt.toPlainString());
            }
            return sb.isEmpty() ? "0" : sb.toString();
        }

        if ("BENEFITS PACKAGE".equals(req.getAllowanceMode())) {
            // untuk bulk: belum ada person context di req,
            // jadi allowanceValueText untuk package akan diset saat per-person, bukan dari req langsung.
            // Di createPayrollBulk kita sudah set allowancesValue via buildAllowanceValueText(req),
            // namun untuk package yang benar perlu per person.
            // Karena itu: kita override di createPayrollBulk jika mode BENEFITS PACKAGE.
            return "0";
        }

        return "0";
    }

    private BigDecimal parseAllowanceTotal(HrPayroll payroll) {
        if (payroll == null) return BigDecimal.ZERO;

        String mode = payroll.getAllowancesType();
        String raw = payroll.getAllowancesValue();

        if (mode == null || "NO ALLOWANCE".equalsIgnoreCase(mode)) return BigDecimal.ZERO;
        return sumAllowanceValueText(raw);
    }

    private String buildOvertimeValuePaymentText(AddPayrollRequest req, HrPerson person) {
        // format:
        // STATIC: "0 | nominal"
        // PERCENTAGE: "pct | pctAmount" (pctAmount = pct% * grossSalary)
        if (req == null) return "0 | 0";

        String type = req.getOvertimePaymentType() == null ? "STATIC" : req.getOvertimePaymentType();

        if ("PERCENTAGE".equalsIgnoreCase(type)) {
            int pct = req.getOvertimePercent() == null ? 0 : req.getOvertimePercent();
            BigDecimal gross = person == null ? BigDecimal.ZERO : resolveGrossSalary(person.getId());
            BigDecimal pctAmount = gross.multiply(BigDecimal.valueOf(pct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            return pct + " | " + pctAmount.toPlainString();
        }

        BigDecimal nominal = req.getOvertimeStaticNominal() == null ? BigDecimal.ZERO : req.getOvertimeStaticNominal();
        return "0 | " + nominal.toPlainString();
    }

    private BigDecimal calculateOvertimeTotal(HrPayroll payroll,
                                              LocalDate startOfMonth,
                                              LocalDate startOfNextMonth) {
        // 1) Ambil attendance OVERTIME hanya bulan itu (poin 2)
        // 2) hitung total overtime minutes = sum(max(0, checkOut - scheduleCheckOut))
        // 3) overtime_hours payroll = parameter kelipatan menit (0..60)
        //    factor = totalMinutes / overtime_hours (dibulatkan ke bawah agar bulat)
        // 4) overtime_value_payment (text "pct | amount") ambil amount nya saja lalu * factor

        if (payroll == null || payroll.getPerson() == null) return BigDecimal.ZERO;

        Long personId = payroll.getPerson().getId();
        List<HrAttendance> overtimeAtt = hrAttendanceRepository.findOvertimeByPersonAndPeriod(
                personId, startOfMonth, startOfNextMonth
        );

        if (overtimeAtt == null || overtimeAtt.isEmpty()) return BigDecimal.ZERO;

        long totalMinutes = 0L;
        for (HrAttendance a : overtimeAtt) {
            if (a.getCheckOut() == null) continue;
            if (a.getWorkSchedule() == null) continue;
            if (a.getWorkSchedule().getCheckOut() == null) continue;

            LocalTime scheduleOut = a.getWorkSchedule().getCheckOut();
            LocalDate attDate = a.getAttendanceDate();
            if (attDate == null) continue;

            LocalDateTime scheduleOutDt = LocalDateTime.of(attDate, scheduleOut);
            LocalDateTime checkOutDt = a.getCheckOut();

            if (checkOutDt.isAfter(scheduleOutDt)) {
                totalMinutes += Duration.between(scheduleOutDt, checkOutDt).toMinutes();
            }
        }

        if (totalMinutes <= 0) return BigDecimal.ZERO;

        BigDecimal paramMinutes = payroll.getOvertimeHours() == null ? BigDecimal.ZERO : payroll.getOvertimeHours();
        if (paramMinutes.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        // faktor harus bulat, tidak boleh koma
        BigDecimal factorBd = BigDecimal.valueOf(totalMinutes)
                .divide(paramMinutes, 0, RoundingMode.DOWN);

        if (factorBd.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal overtimeUnitAmount = parseOvertimeAmountOnly(payroll.getOvertimeValuePayment());
        return overtimeUnitAmount.multiply(factorBd);
    }

    private BigDecimal parseOvertimeAmountOnly(String overtimeValuePaymentText) {
        // input: "10 | 1000000" -> return 1000000
        if (overtimeValuePaymentText == null) return BigDecimal.ZERO;
        String raw = overtimeValuePaymentText.trim();
        if (raw.isEmpty()) return BigDecimal.ZERO;

        String[] parts = raw.split("\\|");
        if (parts.length < 2) {
            return parseBigDecimalSafe(raw);
        }
        return parseBigDecimalSafe(parts[1].trim());
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

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class AllowancePack {
        private final String allowanceValueText;
        private final BigDecimal allowanceTotal;

        private AllowancePack(String allowanceValueText, BigDecimal allowanceTotal) {
            this.allowanceValueText = allowanceValueText;
            this.allowanceTotal = allowanceTotal;
        }
    }

    private AllowancePack buildAllowanceValue(HrPerson person, LocalDate payrollDate, HrPersonPosition personPosition, AddPayrollRequest req) {

        String mode = nvlString(req.getAllowanceMode()).trim();

        // NO ALLOWANCE
        if ("NO ALLOWANCE".equalsIgnoreCase(mode) || mode.isBlank()) {
            return new AllowancePack("0", BigDecimal.ZERO);
        }

        // SELECT ALLOWANCE
        if ("SELECT ALLOWANCE".equalsIgnoreCase(mode)) {
            if (req.getSelectedAllowances() == null || req.getSelectedAllowances().isEmpty()) {
                return new AllowancePack("0", BigDecimal.ZERO);
            }

            StringBuilder sb = new StringBuilder();
            BigDecimal total = BigDecimal.ZERO;

            for (HrSalaryAllowance a : req.getSelectedAllowances()) {
                if (a == null) continue;

                String name = a.getName() == null ? "" : a.getName().trim();
                BigDecimal amount = a.getAmount() == null ? BigDecimal.ZERO : a.getAmount();

                if (!name.isBlank()) {
                    if (!sb.isEmpty()) sb.append("|");
                    sb.append(name).append(":").append(amount.toPlainString());
                }

                total = total.add(amount);
            }

            if (sb.isEmpty()) {
                return new AllowancePack("0", BigDecimal.ZERO);
            }

            return new AllowancePack(sb.toString(), total);
        }

        // BENEFITS PACKAGE
        if ("BENEFITS PACKAGE".equalsIgnoreCase(mode)) {
            List<HrSalaryPositionAllowance> packages = hrSalaryPositionAllowanceRepository
                    .findByPositionAndCompanyOrderByUpdatedAtAsc(personPosition.getPosition(), personPosition.getCompany());

            BigDecimal total = BigDecimal.ZERO;
            StringBuilder sb = new StringBuilder();

            for (HrSalaryPositionAllowance p : packages) {
                if (p == null) continue;

                // filter active by start/end date
                if (p.getStartDate() != null && payrollDate.isBefore(p.getStartDate())) continue;
                if (p.getEndDate() != null && payrollDate.isAfter(p.getEndDate())) continue;

                HrSalaryAllowance a = p.getAllowance();
                if (a == null) continue;

                String name = a.getName() == null ? "" : a.getName().trim();
                BigDecimal amount = a.getAmount() == null ? BigDecimal.ZERO : a.getAmount();

                if (!name.isBlank()) {
                    if (!sb.isEmpty()) sb.append("|");
                    sb.append(name).append(":").append(amount.toPlainString());
                }

                total = total.add(amount);
            }

            if (sb.isEmpty()) {
                return new AllowancePack("0", BigDecimal.ZERO);
            }

            return new AllowancePack(sb.toString(), total);
        }

        // fallback
        return new AllowancePack("0", BigDecimal.ZERO);
    }

    private HrPayrollCalculation buildCalculation(HrPayroll payroll, BigDecimal totalAllowances, LocalDate monthStart, LocalDate monthEnd) {

        // ===============================
        // 1) Gross salary dari mapping salary level
        // ===============================
        BigDecimal grossSalary = resolveGrossSalary(payroll.getPerson());

        // ===============================
        // 2) Total overtimes dari attendance + schedule
        // ===============================
        BigDecimal totalOvertimes = resolveTotalOvertimesFromAttendance(payroll, monthStart, monthEnd);

        // ===============================
        // 3) Bonus & deductions (awal 0, nanti via Add Komponen)
        // ===============================
        BigDecimal totalBonus = payroll.getAnnualBonus() == null ? BigDecimal.ZERO : payroll.getAnnualBonus();
        BigDecimal totalOtherDeductions = payroll.getOtherDeductions() == null ? BigDecimal.ZERO : payroll.getOtherDeductions();

        // ===============================
        // 4) total_taxable (sementara 0 dulu sesuai kebutuhan saat ini)
        // kalau Anda mau, nanti kita sambungkan ke bracket + PTKP
        // ===============================
        BigDecimal totalTaxable = BigDecimal.ZERO;

        // ===============================
        // 5) Net THP sesuai formula Anda
        // net = gross + allowance + overtime + bonus - other_deduct - total_taxable
        // ===============================
        BigDecimal netTakeHomePay = grossSalary
                .add(nvl(totalAllowances))
                .add(nvl(totalOvertimes))
                .add(nvl(totalBonus))
                .subtract(nvl(totalOtherDeductions))
                .subtract(nvl(totalTaxable));

        return HrPayrollCalculation.builder()
                .payrollInput(payroll)
                .grossSalary(grossSalary)
                .totalAllowances(nvl(totalAllowances))
                .totalOvertimes(nvl(totalOvertimes))
                .totalBonus(nvl(totalBonus))
                .totalOtherDeductions(nvl(totalOtherDeductions))
                .totalTaxable(nvl(totalTaxable))
                .netTakeHomePay(netTakeHomePay)
                .calculatedAt(LocalDateTime.now())
                .notes("Calculated on bulk payroll creation")
                .build();
    }

    private BigDecimal resolveGrossSalary(HrPerson person) {
        if (person == null || person.getId() == null) return BigDecimal.ZERO;

        try {
            FwAppUser personUser = appUserRepository.findByPersonId(person.getId()).orElse(null);
            if (personUser == null) return BigDecimal.ZERO;

            HrSalaryEmployeeLevel level = hrSalaryEmployeeLevelRepository.findByAppUserId(personUser.getId()).orElse(null);
            if (level == null || level.getBaseLevel() == null || level.getBaseLevel().getBaseSalary() == null) {
                return BigDecimal.ZERO;
            }

            return level.getBaseLevel().getBaseSalary();
        } catch (Exception ex) {
            log.warn("resolveGrossSalary failed for personId {}: {}", person.getId(), ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal resolveTotalOvertimesFromAttendance(HrPayroll payroll, LocalDate monthStart, LocalDate monthEnd) {

        if (payroll == null || payroll.getPerson() == null || payroll.getPerson().getId() == null) {
            return BigDecimal.ZERO;
        }

        // parameter overtime_hours dari form (menit 0..60)
        BigDecimal paramMinutes = payroll.getOvertimeHours() == null ? BigDecimal.ZERO : payroll.getOvertimeHours();
        if (paramMinutes.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        List<HrAttendance> overtimeAttendances = hrAttendanceRepository.findOvertimeAttendances(
                payroll.getPerson().getId(),
                monthStart,
                monthEnd,
                "OVERTIME"
        );

        if (overtimeAttendances == null || overtimeAttendances.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalOvertimeMinutes = 0;

        for (HrAttendance a : overtimeAttendances) {
            if (a.getCheckOut() == null) continue;
            if (a.getWorkSchedule() == null || a.getWorkSchedule().getCheckOut() == null) continue;

            // scheduled checkout time
            LocalDate d = a.getAttendanceDate();
            if (d == null) continue;

            LocalDateTime scheduledOut = LocalDateTime.of(d, a.getWorkSchedule().getCheckOut());
            LocalDateTime actualOut = a.getCheckOut();

            if (actualOut.isAfter(scheduledOut)) {
                long diff = java.time.Duration.between(scheduledOut, actualOut).toMinutes();
                if (diff > 0) totalOvertimeMinutes += diff;
            }
        }

        if (totalOvertimeMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        // multiplier = floor(totalMinutes / paramMinutes) -> integer only
        BigDecimal minutesBD = BigDecimal.valueOf(totalOvertimeMinutes);
        BigDecimal multiplierBD = minutesBD.divide(paramMinutes, 0, RoundingMode.DOWN); // no decimals

        if (multiplierBD.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal basePayment = parseOvertimeMoney(payroll.getOvertimeValuePayment());

        return basePayment.multiply(multiplierBD);
    }

    private BigDecimal parseOvertimeMoney(String overtimeValuePayment) {
        if (overtimeValuePayment == null) return BigDecimal.ZERO;

        String raw = overtimeValuePayment.trim();
        if (raw.isEmpty()) return BigDecimal.ZERO;

        // format: "NilaiPersen | NilaiUang"
        // contoh: "10 | 1000000" atau "0 | 100000"
        try {
            String[] parts = raw.split("\\|");
            if (parts.length == 1) {
                // fallback: kalau ternyata cuma angka tanpa delimiter
                return parseBigDecimalSafe(parts[0]);
            }
            // bagian kedua = NilaiUang
            return parseBigDecimalSafe(parts[1]);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseOvertimePercent(String overtimeValuePayment) {
        if (overtimeValuePayment == null) return BigDecimal.ZERO;

        String raw = overtimeValuePayment.trim();
        if (raw.isEmpty()) return BigDecimal.ZERO;

        try {
            String[] parts = raw.split("\\|");
            if (parts.length == 0) return BigDecimal.ZERO;
            // bagian pertama = NilaiPersen
            return parseBigDecimalSafe(parts[0]);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String nvlString(String s) {
        return s == null ? "" : s;
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

    private String buildAllowanceValueTextForPerson(AddPayrollRequest req,
                                                    HrPerson person,
                                                    LocalDate payrollDate,
                                                    HrPersonPosition personPosition) {

        if (req == null || req.getAllowanceMode() == null) return "0";

        String mode = req.getAllowanceMode().trim();

        // 1) NO ALLOWANCE
        if ("NO ALLOWANCE".equalsIgnoreCase(mode) || mode.isBlank()) {
            return "0";
        }

        // helper sanitize agar delimiter tidak rusak
        java.util.function.Function<String, String> sanitizeName =
                (nm) -> nm == null ? "" : nm.replace("|", " ").replace(":", " ").trim();

        // 2) SELECT ALLOWANCE -> dari req.selectedAllowances
        if ("SELECT ALLOWANCE".equalsIgnoreCase(mode)) {
            if (req.getSelectedAllowances() == null || req.getSelectedAllowances().isEmpty()) return "0";

            StringBuilder sb = new StringBuilder();
            for (HrSalaryAllowance a : req.getSelectedAllowances()) {
                if (a == null) continue;
                String name = sanitizeName.apply(a.getName());
                if (name.isBlank()) continue;

                BigDecimal amount = a.getAmount() == null ? BigDecimal.ZERO : a.getAmount();

                if (!sb.isEmpty()) sb.append("|");
                sb.append(name).append(":").append(amount.toPlainString());
            }
            return sb.isEmpty() ? "0" : sb.toString();
        }

        // 3) BENEFITS PACKAGE -> dari hr_salary_position_allowance by position + active by date
        if ("BENEFITS PACKAGE".equalsIgnoreCase(mode)) {
            if (personPosition == null || personPosition.getPosition() == null) return "0";

            List<HrSalaryPositionAllowance> packages =
                    hrSalaryPositionAllowanceRepository.findByPositionAndCompanyOrderByUpdatedAtAsc(
                            personPosition.getPosition(), personPosition.getCompany()
                    );

            if (packages == null || packages.isEmpty()) return "0";

            StringBuilder sb = new StringBuilder();

            for (HrSalaryPositionAllowance p : packages) {
                if (p == null) continue;

                // filter active by start/end date
                if (p.getStartDate() != null && payrollDate.isBefore(p.getStartDate())) continue;
                if (p.getEndDate() != null && payrollDate.isAfter(p.getEndDate())) continue;

                HrSalaryAllowance a = p.getAllowance();
                if (a == null) continue;

                String name = sanitizeName.apply(a.getName());
                if (name.isBlank()) continue;

                BigDecimal amount = a.getAmount() == null ? BigDecimal.ZERO : a.getAmount();

                if (!sb.isEmpty()) sb.append("|");
                sb.append(name).append(":").append(amount.toPlainString());
            }

            return sb.isEmpty() ? "0" : sb.toString();
        }

        return "0";
    }

    private BigDecimal sumAllowanceValueText(String allowancesValue) {
        if (allowancesValue == null) return BigDecimal.ZERO;
        String raw = allowancesValue.trim();
        if (raw.isEmpty() || "0".equals(raw)) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        String[] items = raw.split("\\|");
        for (String it : items) {
            if (it == null) continue;
            String s = it.trim();
            if (s.isEmpty()) continue;

            int idx = s.lastIndexOf(':');
            String amtStr = (idx >= 0) ? s.substring(idx + 1).trim() : s;

            total = total.add(parseBigDecimalSafe(amtStr));
        }
        return total;
    }

    private BigDecimal resolveHealthInsurancePercentSum() {
        // sum int_val dari sort_order 100,101,102
        List<Integer> orders = List.of(100, 101, 102);

        // Asumsi repo punya method ini. Kalau belum, bikin di repo:
        // List<FwSystem> findBySortOrderIn(Collection<Integer> sortOrders);
        List<FwSystem> rows = fwSystemRepository.findBySortOrderIn(orders);

        if (rows == null || rows.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;
        for (FwSystem s : rows) {
            if (s == null || s.getIntVal() == null) continue;
            sum = sum.add(BigDecimal.valueOf(s.getIntVal()));
        }
        return sum; // contoh: 2 + 1 + 1 = 4 (%)
    }

    private BigDecimal resolveHealthInsuranceDeduction(BigDecimal grossSalary) {
        BigDecimal gross = nvl(grossSalary);
        if (gross.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal percentSum = resolveHealthInsurancePercentSum(); // misal 4
        if (percentSum.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        // gross * percent/100
        return gross.multiply(percentSum)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private int resolveSumAttendance(Long personId, LocalDate startOfMonth, LocalDate startOfNextMonth) {
        if (personId == null) return 0;
        try {
            long c = hrAttendanceRepository.countAttendanceByPersonAndPeriod(personId, startOfMonth, startOfNextMonth);
            return (int) Math.max(0, c);
        } catch (Exception ex) {
            log.warn("resolveSumAttendance failed personId {}: {}", personId, ex.getMessage());
            return 0;
        }
    }

    private BigDecimal computeRealGrossDeduction(BigDecimal grossSalary, Integer sumAttendance, Integer paramAttendanceDays) {
        BigDecimal gross = nvl(grossSalary);

        int sum = sumAttendance == null ? 0 : sumAttendance;
        int param = paramAttendanceDays == null ? 0 : paramAttendanceDays;

        if (param <= 0) {
            // tidak ada basis 100%, supaya tidak crash kita anggap tidak ada potongan
            return BigDecimal.ZERO;
        }

        int diff = sum - param;
        if (diff >= 0) {
            return BigDecimal.ZERO;
        }

        int missing = Math.abs(diff); // jumlah hari kurang
        // missingPct = missing / param * 100
        BigDecimal missingPct = BigDecimal.valueOf(missing)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(param), 6, RoundingMode.HALF_UP);

        // potongan rupiah = gross * missingPct / 100
        BigDecimal deduction = gross
                .multiply(missingPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (deduction.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (deduction.compareTo(gross) > 0) return gross; // safety clamp
        return deduction;
    }

}
