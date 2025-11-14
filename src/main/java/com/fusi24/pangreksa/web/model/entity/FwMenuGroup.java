package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fw_menu_group")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FwMenuGroup extends  AuditableEntity<FwMenuGroup> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    private String code;

    private String name;

    private String description;

    private Boolean isActive;
}
