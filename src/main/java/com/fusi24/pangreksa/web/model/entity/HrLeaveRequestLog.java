package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_request_log", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLeaveRequestLog extends AuditableEntity<HrLeaveRequestLog> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "leave_application_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "hr_leave_request_log_leave_application_id_fkey")
    )
    private HrLeaveApplication leaveApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatusEnum status;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private HrPerson changedBy;

    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private HrCompany company;
}
