package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.GenderEnum;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "karyawan_tanggungan")
public class HrPersonTanggungan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String relation; // Suami / Istri / Anak Kandung

    @Column(nullable = false)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenderEnum gender;

    @Column(name = "still_dependent", nullable = false)
    private Boolean stillDependent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private HrPerson person;

    // ================= GETTER & SETTER =================

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public GenderEnum getGender() {
        return gender;
    }

    public void setGender(GenderEnum gender) {
        this.gender = gender;
    }

    public Boolean getStillDependent() {
        return stillDependent;
    }

    public void setStillDependent(Boolean stillDependent) {
        this.stillDependent = stillDependent;
    }

    public HrPerson getPerson() {
        return person;
    }

    public void setPerson(HrPerson person) {
        this.person = person;
    }
}
