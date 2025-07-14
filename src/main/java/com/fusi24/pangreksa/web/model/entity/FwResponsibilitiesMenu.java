package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fw_responsibilities_menu", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "fw_responsibilities_menu_unique", columnNames = "id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FwResponsibilitiesMenu  extends AuditableEntity<FwResponsibilities>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false,
            foreignKey = @ForeignKey(name = "fw_responsibilities_menu_fw_menus_fk"))
    private FwMenus menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsibility_id", nullable = false,
            foreignKey = @ForeignKey(name = "fw_responsibilities_menu_fw_responsibilities_fk"))
    private FwResponsibilities responsibility;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
