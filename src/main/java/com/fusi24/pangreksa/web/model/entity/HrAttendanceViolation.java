package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "hr_attendance_violation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "attendance_date", "violation_type"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAttendanceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrPerson employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "violation_type", nullable = false)
    private String violationType; // INCOMPLETE_LOG

    @Column(name = "penalty_flag", nullable = false)
    private Boolean penaltyFlag = true;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
