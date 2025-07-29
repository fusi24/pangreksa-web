package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "hr_address")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonAddress extends AuditableEntity<HrPersonAddress>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @Column(name = "full_address", length = 500, nullable = false)
    private String fullAddress;

    @Column(name = "is_default")
    private Boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "reference_id", nullable = false)
    private HrPerson person;
}

