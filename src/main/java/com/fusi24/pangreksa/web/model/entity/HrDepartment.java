package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_department")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrDepartment extends AuditableEntity<HrDepartment>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    private String code;

    private String name;

    private String description;

    private Boolean isActive;

}
