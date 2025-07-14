package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.domain.UserId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fw_appuser",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "fw_appuser_username_key", columnNames = "username"),
                @UniqueConstraint(name = "fw_appuser_email_key", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FwAppUser extends AuditableEntity<FwAppUser> implements AppUserInfo{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(length = 100, nullable = false)
    private String email;

    @Column(length = 255, nullable = true)
    private String profileUrl;

    @Column(length = 255, nullable = true)
    private String pictureUrl;

    @Column(length = 100)
    private String nickname;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Optional column: raw password (NOT recommended in production)
    @Column(name = "password", length = 255)
    private String password;

    @Override
    public UserId getUserId() {
        return UserId.of(this.username);
    }

    @Override
    public String getPreferredUsername() {
        return this.nickname;
    }
}
