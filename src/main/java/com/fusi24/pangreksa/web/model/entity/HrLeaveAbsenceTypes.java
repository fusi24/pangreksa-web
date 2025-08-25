package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.LeaveAbsenceTypeEnum;
import com.fusi24.pangreksa.web.model.enumerate.LeaveTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_leave_absence_types", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLeaveAbsenceTypes extends AuditableEntity<HrLeaveAbsenceTypes>{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_absence_type", nullable = false, length = 50)
    private LeaveAbsenceTypeEnum leaveAbsenceType; // PAID_LEAVE or UNPAID_LEAVE

    @Column(name = "leave_type", length = 50)
    private String leaveType; // e.g. ANNUAL, SICK, MARRIAGE, etc.

    @Column(name = "label", length = 150)
    private String label;

    @Column(name = "is_enable", nullable = false)
    private Boolean isEnable = Boolean.TRUE;

    @Column(name = "description")
    private String description;

    @Column(name = "max_allowed_days")
    private Integer maxAllowedDays;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
