package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_calculations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrPayrollCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RELATION
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_input_id", nullable = false)
    private HrPayroll payroll;

    // PENGHASILAN
    @Column(name = "base_salary", precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "fixed_allowance_total", precision = 19, scale = 2)
    private BigDecimal fixedAllowanceTotal;

    @Column(name = "variable_allowance_total", precision = 19, scale = 2)
    private BigDecimal variableAllowanceTotal;

    @Column(name = "overtime_amount", precision = 19, scale = 2)
    private BigDecimal overtimeAmount;

    @Column(name = "bonus_amount", precision = 19, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "gross_salary", precision = 19, scale = 2)
    private BigDecimal grossSalary;

    // ATTENDANCE
    @Column(name = "absence_deduction", precision = 19, scale = 2)
    private BigDecimal absenceDeduction;

    @Column(name = "late_deduction", precision = 19, scale = 2)
    private BigDecimal lateDeduction;

    // BPJS EMPLOYEE
    @Column(name = "bpjs_jht_deduction", precision = 19, scale = 2)
    private BigDecimal bpjsJhtDeduction;

    @Column(name = "bpjs_jp_deduction", precision = 19, scale = 2)
    private BigDecimal bpjsJpDeduction;

    @Column(name = "bpjs_jkn_deduction", precision = 19, scale = 2)
    private BigDecimal bpjsJknDeduction;

    // BPJS COMPANY
    @Column(name = "bpjs_jht_company", precision = 19, scale = 2)
    private BigDecimal bpjsJhtCompany;

    @Column(name = "bpjs_jp_company", precision = 19, scale = 2)
    private BigDecimal bpjsJpCompany;

    @Column(name = "bpjs_jkn_company", precision = 19, scale = 2)
    private BigDecimal bpjsJknCompany;

    // TAX
    @Column(name = "pph21_deduction", precision = 19, scale = 2)
    private BigDecimal pph21Deduction;

    // FINAL
    @Column(name = "total_deduction", precision = 19, scale = 2)
    private BigDecimal totalDeduction;

    @Column(name = "net_take_home_pay", precision = 19, scale = 2)
    private BigDecimal netTakeHomePay;

    @Column(name = "ter_category", length = 10)
    private String terCategory;

    @Column(name = "ter_rate_percent", precision = 5, scale = 2)
    private BigDecimal terRatePercent;

    @Column(name = "penghasilan_teratur_amount", precision = 19, scale = 2)
    private BigDecimal penghasilanTeraturAmount;

    @Column(name = "ter_dpp_amount", precision = 19, scale = 2)
    private BigDecimal terDppAmount;

    // META
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "notes")
    private String notes;
}