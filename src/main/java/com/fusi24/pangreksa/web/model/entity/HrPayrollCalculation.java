package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_calculations")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrPayrollCalculation extends AuditableEntity<HrPayrollCalculation> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    // === Reference to Payroll Input ===
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_input_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payroll_calculation_input"))
    private HrPayroll payrollInput;

    // === Calculation Results ===
    @Column(name = "gross_salary", precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "bpjs_health_deduction", precision = 15, scale = 2)
    private BigDecimal bpjsHealthDeduction;

    @Column(name = "bpjs_jht_deduction", precision = 15, scale = 2)
    private BigDecimal bpjsJhtDeduction;

    @Column(name = "bpjs_jp_deduction", precision = 15, scale = 2)
    private BigDecimal bpjsJpDeduction;

    @Column(name = "annual_income_before_tax", precision = 15, scale = 2)
    private BigDecimal annualIncomeBeforeTax;

    @Column(name = "ptkp_applied", precision = 15, scale = 2)
    private BigDecimal ptkpApplied;

    @Column(name = "taxable_income", precision = 15, scale = 2)
    private BigDecimal taxableIncome;

    @Column(name = "pph21_amount", precision = 15, scale = 2)
    private BigDecimal pph21Amount;

    @Column(name = "net_take_home_pay", precision = 15, scale = 2)
    private BigDecimal netTakeHomePay;

    // === Audit timestamp (if not already in AuditableEntity) ===
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    // === Optional Notes ===
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}