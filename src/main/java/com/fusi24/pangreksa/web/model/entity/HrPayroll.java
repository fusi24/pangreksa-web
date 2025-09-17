package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.entity.AuditableEntity;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hr_payroll")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrPayroll extends AuditableEntity<HrPayroll> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    // === Foreign Key: Person (Employee) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false, foreignKey = @ForeignKey(name = "fk_hr_payroll_person"))
    private HrPerson person;

    // === Payroll Month ===
    @Column(name = "payroll_month", nullable = false)
    private LocalDate payrollMonth;

    // === Earnings ===
    @Column(name = "variable_allowances", precision = 15, scale = 2)
    private BigDecimal variableAllowances = BigDecimal.ZERO;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "overtime_amount", precision = 15, scale = 2)
    private BigDecimal overtimeAmount = BigDecimal.ZERO;

    @Column(name = "annual_bonus", precision = 15, scale = 2)
    private BigDecimal annualBonus = BigDecimal.ZERO;

    // === Deductions ===
    @Column(name = "other_deductions", precision = 15, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "previous_thp_paid", precision = 15, scale = 2)
    private BigDecimal previousThpPaid = BigDecimal.ZERO;

    // === Attendance & Flags ===
    @Column(name = "attendance_days")
    private Integer attendanceDays;

    @Column(name = "prorated_salary")
    private Boolean proratedSalary = Boolean.FALSE;
}