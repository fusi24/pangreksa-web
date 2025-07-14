package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "fw_responsibilities",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "fw_responsibilities_responsibility_labela_key", columnNames = "label")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FwResponsibilities extends AuditableEntity<FwResponsibilities>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label", length = 50, nullable = false)
    private String label;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
