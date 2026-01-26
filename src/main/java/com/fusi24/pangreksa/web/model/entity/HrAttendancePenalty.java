package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "hr_attendance_penalty",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "payroll_period", "reference_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAttendancePenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrPerson employee;

    @Column(name = "payroll_period", nullable = false)
    private String payrollPeriod; // YYYY-MM

    @Column(name = "cut_leave_days")
    private BigDecimal cutLeaveDays;

    @Column(name = "cut_allowance_amount")
    private BigDecimal cutAllowanceAmount;

    @Column(name = "source", nullable = false)
    private String source = "ATTENDANCE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_id", nullable = false)
    private HrAttendanceViolation reference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
