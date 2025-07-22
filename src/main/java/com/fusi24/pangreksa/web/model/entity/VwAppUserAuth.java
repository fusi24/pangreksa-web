package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_appuser_auth")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VwAppUserAuth {

    @Id
    private Long id;
    @Column(name = "username")
    private String username;
    @Column(name = "email")
    private String email;
    @Column(name = "nickname")
    private String nickname;
    @Column(name = "responsibility")
    private String responsibility;
    @Column(name = "label")
    private String label;
    @Column(name = "role")
    private String role;
    @Column(name = "description")
    private String description;
    @Column(name = "url")
    private String url;
    @Column(name = "sort_order")
    private double sortOrder;
    @Column(name = "page_icon")
    private String pageIcon;
    @Column(name = "is_active")
    private boolean isActive;
    @Column(name = "page_id")
    private Long pageId;
    @Column(name = "menu_id")
    private Long menuId;

}
