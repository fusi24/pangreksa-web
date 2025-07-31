package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.EducationTypeEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "hr_education")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonEducation  extends AuditableEntity<HrPersonEducation> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @Column(length = 50, nullable = false)
    private String institution;

    @Column(length = 50, nullable = false)
    private String program;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    private LocalDate startDate;

    private LocalDate finishDate;

    @Column(name = "certificate_title", length = 50)
    private String certificateTitle;

    @Column(name = "certificate_expiration")
    private LocalDate certificateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EducationTypeEnum type;

    @ManyToOne
    @JoinColumn(name = "reference_id", nullable = false)
    private HrPerson person;
}

