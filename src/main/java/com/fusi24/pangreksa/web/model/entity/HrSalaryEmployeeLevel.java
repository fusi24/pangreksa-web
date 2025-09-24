package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_salary_employee_level")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrSalaryEmployeeLevel extends AuditableEntity<HrSalaryEmployeeLevel> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // DDL: id bigserial/identity
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salary", nullable = false,
            foreignKey = @ForeignKey(name = "salary_employee_level_on_base_salary_level"))
    private HrSalaryBaseLevel baseLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_fwuser", nullable = false,
            foreignKey = @ForeignKey(name = "salary_employee_level_on_fw_user"))
    private FwAppUser appUser;

    // created_by, updated_by, created_at, updated_at diwariskan dari AuditableEntity
}
