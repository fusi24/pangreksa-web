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
public class HrPayrollCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK: hr_payroll_calculations.payroll_input_id -> hr_payroll.id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_input_id")
    private HrPayroll payrollInput;

    @Column(name = "gross_salary", precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "total_allowances", precision = 15, scale = 2)
    private BigDecimal totalAllowances;

    @Column(name = "total_overtimes", precision = 15, scale = 2)
    private BigDecimal totalOvertimes;

    @Column(name = "total_bonus", precision = 15, scale = 2)
    private BigDecimal totalBonus;

    @Column(name = "total_other_deductions", precision = 15, scale = 2)
    private BigDecimal totalOtherDeductions;

    @Column(name = "total_taxable", precision = 15, scale = 2)
    private BigDecimal totalTaxable;

    @Column(name = "health_deduction")
    private BigDecimal healthDeduction;

    @Column(name = "real_gross_salary")
    private BigDecimal realGrossSalary;

    @Column(name = "net_take_home_pay", precision = 15, scale = 2)
    private BigDecimal netTakeHomePay;

    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
