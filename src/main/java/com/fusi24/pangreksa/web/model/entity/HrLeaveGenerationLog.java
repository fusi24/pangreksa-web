package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_leave_generation_log", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLeaveGenerationLog extends AuditableEntity<HrLeaveGenerationLog>{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "data_generated")
    private Integer dataGenerated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private HrCompany company;
}
