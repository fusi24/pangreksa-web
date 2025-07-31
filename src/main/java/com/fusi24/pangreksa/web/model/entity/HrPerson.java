package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.GenderEnum;
import com.fusi24.pangreksa.web.model.enumerate.MarriageEnum;
import com.fusi24.pangreksa.web.model.enumerate.NationalityEnum;
import com.fusi24.pangreksa.web.model.enumerate.ReligionEnum;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "hr_person")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrPerson extends AuditableEntity<HrPerson> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "pob", length = 50)
    private String pob;

    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private GenderEnum gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality")
    private NationalityEnum nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "religion")
    private ReligionEnum religion;

    @Enumerated(EnumType.STRING)
    @Column(name = "marriage")
    private MarriageEnum marriage;


    @Column(name = "ktp_number", length = 16, unique = true)
    private String ktpNumber;
}

