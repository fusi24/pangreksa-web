package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hr_salary_allowance")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrSalaryAllowance extends AuditableEntity<HrSalaryAllowance>{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @Column(name = "allowance_name", length = 10, nullable = false)
    private String name;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private HrCompany company;

    @Column(name = "is_attendance_based", nullable = false)
    private Boolean isAttendanceBased = false;

    @Column(name = "penalty_rate", precision = 5, scale = 2)
    private BigDecimal penaltyRate;

}
