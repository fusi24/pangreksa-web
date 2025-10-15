package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fw_menus", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "fw_menus_unique", columnNames = "id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FwMenus extends AuditableEntity<FwMenus>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @Column(name = "can_view")
    private Boolean canView = true;

    @Column(name = "can_create")
    private Boolean canCreate = false;

    @Column(name = "can_edit")
    private Boolean canEdit = false;

    @Column(name = "can_delete")
    private Boolean canDelete = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "label", length = 50, nullable = false)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fw_menus_fw_pages_fk"))
    private FwPages page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private FwMenuGroup group;
}
