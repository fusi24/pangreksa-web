package com.fusi24.pangreksa.web.model.entity;

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

    // SNAPSHOT IDENTITAS
    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // ORGANIZATION
    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "position_code", nullable = false)
    private String positionCode;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "department_code", nullable = false)
    private String departmentCode;

    // PERSONAL
    @Column(name = "pob")
    private String pob;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender")
    private String gender;

    @Column(name = "ktp_number")
    private String ktpNumber;

    // EMPLOYMENT
    @Column(name = "status_employee")
    private String statusEmployee;

    @Column(name = "join_date")
    private LocalDate joinDate;

    // PAYROLL CONTEXT
    @Column(name = "payroll_date", nullable = false)
    private LocalDate payrollDate;

    @Column(name = "param_attendance_days", nullable = false)
    private Integer paramAttendanceDays;

    // SALARY
    @Column(name = "base_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    // VARIABLE INPUT
    @Column(name = "overtime_hours", precision = 10, scale = 2)
    private BigDecimal overtimeHours;

    @Column(name = "overtime_amount", precision = 19, scale = 2)
    private BigDecimal overtimeAmount;

    @Column(name = "bonus_amount", precision = 19, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "other_deductions", precision = 19, scale = 2)
    private BigDecimal otherDeductions;

    // PTKP
    @Column(name = "ptkp_code", nullable = false)
    private String ptkpCode;

    @Column(name = "ptkp_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal ptkpAmount;

    // ATTENDANCE
    @Column(name = "sum_attendance")
    private Integer sumAttendance;
}