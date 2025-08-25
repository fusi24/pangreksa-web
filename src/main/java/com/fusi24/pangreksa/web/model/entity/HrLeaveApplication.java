package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.model.enumerate.LeaveTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_application", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLeaveApplication extends AuditableEntity<HrLeaveApplication> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrPerson employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_absence_type_id", nullable = false)
    private HrLeaveAbsenceTypes leaveAbsenceType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LeaveStatusEnum status = LeaveStatusEnum.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_to")
    private HrPerson submittedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private HrPerson approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private HrCompany company;
}
