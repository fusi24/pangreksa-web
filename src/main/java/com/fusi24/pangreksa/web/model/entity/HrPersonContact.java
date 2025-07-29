package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.ContactTypeEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "hr_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonContact extends AuditableEntity<HrPersonContact>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @Column(length = 50)
    private String designation;

    @Column(length = 50)
    private String relationship;

    @Column(name = "string_value", length = 50, nullable = false)
    private String stringValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactTypeEnum type;

    @Column(length = 255)
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "reference_id", nullable = false)
    private HrPerson person;
}

