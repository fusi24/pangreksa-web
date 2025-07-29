package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_position")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPosition extends AuditableEntity<HrPosition> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private HrCompany company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_structure_id")
    private HrOrgStructure orgStructure;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "level")
    private Integer level;

    @Column(name = "is_managerial")
    private Boolean isManagerial = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to")
    private HrPosition reportsTo;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}

