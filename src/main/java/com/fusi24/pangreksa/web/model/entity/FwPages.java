package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fw_pages", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FwPages extends AuditableEntity<FwPages>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 255)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "page_url", length = 50)
    private String pageUrl;

    @Column(name = "page_icon", length = 50)
    private String pageIcon;
}