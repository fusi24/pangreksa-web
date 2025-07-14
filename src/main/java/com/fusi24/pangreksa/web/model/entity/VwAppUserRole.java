package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_appuser_role")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VwAppUserRole{

    @Id
    private Long id;
    @Column(name = "username")
    private String username;
    @Column(name = "email")
    private String email;
    @Column(name = "role")
    private String role;
}
