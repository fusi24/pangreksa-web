package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fusi24.pangreksa.web.model.entity.HrDepartment;

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
    private HrDepartment department;

    public HrDepartment getDepartment() {
        return department;
    }

    public void setDepartment(HrDepartment department) {
        this.department = department;
    }

    @Column(name = "position_description", nullable = false, length = 35)
    private String position_description;

}
