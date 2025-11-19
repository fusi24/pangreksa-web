package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;
// Hapus: import com.fusi24.pangreksa.web.model.entity.HrDepartment;
// Ganti dengan:
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure; // <--- BARU

@Entity
@Table(name = "hr_position_level")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrPositionLevel extends AuditableEntity<HrPositionLevel> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "position", nullable = false, length = 35)
    private String position;

    @ManyToOne
    @JoinColumn(name = "department_id") // kolom baru di DB
    private HrOrgStructure department; // <--- TIPE DIPERBAIKI

    public HrOrgStructure getDepartment() { // <--- TIPE DIPERBAIKI
        return department;
    }

    public void setDepartment(HrOrgStructure department) { // <--- TIPE DIPERBAIKI
        this.department = department;
    }

    @Column(name = "position_description", nullable = false, length = 35)
    private String position_description;

}