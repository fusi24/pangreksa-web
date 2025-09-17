package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_salary_employee_level")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at_hsel", nullable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at_hsel", nullable = false))
})
@AssociationOverrides({
        @AssociationOverride(name = "createdBy", joinColumns = @JoinColumn(name = "created_by_hsel")),
        @AssociationOverride(name = "updatedBy", joinColumns = @JoinColumn(name = "updated_by_hsel"))
})
public class HrSalaryEmployeeLevel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hsel")
    private Long idHsel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hsbl", nullable = false)
    private HrSalaryBaseLevel baseLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_fu", nullable = false)
    private FwAppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_hsel", updatable = false)
    private FwAppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_hsel")
    private FwAppUser updatedBy;

    @Column(name = "created_at_hsel", updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at_hsel", insertable = false, updatable = false)
    private Instant updatedAt;
}
