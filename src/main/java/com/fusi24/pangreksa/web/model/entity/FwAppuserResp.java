package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "fw_appuser_resp",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "fw_appuser_resp_unique", columnNames = "id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FwAppuserResp extends AuditableEntity<FwAppuserResp>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "responsibility_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fw_appuser_resp_fw_responsibilities_fk")
    )
    private FwResponsibilities responsibility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "appuser_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fw_appuser_resp_fw_appuser_fk")
    )
    private FwAppUser appuser;
}
