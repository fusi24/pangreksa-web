package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.LeaveTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_leave_balance", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLeaveBalance extends AuditableEntity<HrLeaveBalance>{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hr_leave_balance_seq")
    @SequenceGenerator(name = "hr_leave_balance_seq", sequenceName = "hr_leave_balance_seq", allocationSize = 1)
    private Long id;

    @Column(name = "year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveTypeEnum leaveType;

    @Column(name = "allocated_days")
    private int allocatedDays = 0;

    @Column(name = "used_days")
    private int usedDays = 0;

    @Column(name = "remaining_days", insertable = false, updatable = false)
    private int remainingDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrPerson employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private HrCompany company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_log_id", foreignKey = @ForeignKey(name = "hr_leave_balance_generation_log_id_fkey"))
    private HrLeaveGenerationLog generationLog;
}
