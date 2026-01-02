package com.fusi24.pangreksa.web.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Column(name = "position_code", nullable = false, length = 20)
    private String positionCode;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "department_code", nullable = false, length = 50)
    private String departmentCode;

    @Column(name = "pob", length = 50)
    private String pob;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender", length = 50)
    private String gender;

    @Column(name = "ktp_number", length = 16)
    private String ktpNumber;

    // Payroll period (DDL: payroll_date date NOT NULL)
    @Column(name = "payroll_date", nullable = false)
    private LocalDate payrollDate;

    // Parameter (DDL: param_attendance_days int4 NULL)
    @Column(name = "param_attendance_days")
    private Integer paramAttendanceDays;

    // Allowances (DDL: allowances_type varchar(50) default 'STATIC', allowances_value text)
    @Column(name = "allowances_type", length = 50)
    private String allowancesType;

    @Column(name = "allowances_value", columnDefinition = "text")
    private String allowancesValue;

    // Overtime (DDL: overtime_hours numeric(5,2), overtime_type, overtime_value_payment numeric(15,2))
    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "overtime_type", length = 50)
    private String overtimeType;

    @Column(name = "overtime_value_payment", precision = 15, scale = 2)
    private BigDecimal overtimeValuePayment = BigDecimal.ZERO;

    // Others
    @Column(name = "annual_bonus", precision = 15, scale = 2)
    private BigDecimal annualBonus = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 15, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    // Tax snapshot
    @Column(name = "ptkp_code", nullable = false, length = 20)
    private String ptkpCode;

    @Column(name = "ptkp_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal ptkpAmount;

    // Additional (DDL: total_leave_year numeric(15,2) default 0)
    @Column(name = "total_leave_year", precision = 15, scale = 2)
    private BigDecimal totalLeaveYear = BigDecimal.ZERO;

    @JsonIgnoreProperties(value = "payrollInput", allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "payrollInput", cascade = { CascadeType.DETACH, CascadeType.REMOVE })
    private HrPayrollCalculation calculation;
}