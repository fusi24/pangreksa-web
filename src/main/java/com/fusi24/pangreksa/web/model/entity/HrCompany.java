package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "hr_company")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrCompany extends AuditableEntity<HrCompany> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private HrCompany parent;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "short_name", length = 20)
    private String shortName;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "establishment_date")
    private LocalDate establishmentDate;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "website", length = 100)
    private String website;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_hr_managed")
    private Boolean isHrManaged = true;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}

