package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.MarriageEnum;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "karyawan_ptkp")
public class HrPersonPtkp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "marriage_status", nullable = false)
    private MarriageEnum marriageStatus;

    @Column(name = "ptkp_code", length = 20, nullable = false)
    private String ptkpCode;

    @Column(name = "ptkp_amount", nullable = false)
    private BigDecimal ptkpAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private HrPerson person;

    // ================= GETTER & SETTER =================

    public Long getId() {
        return id;
    }

    public MarriageEnum getMarriageStatus() {
        return marriageStatus;
    }

    public void setMarriageStatus(MarriageEnum marriageStatus) {
        this.marriageStatus = marriageStatus;
    }

    public String getPtkpCode() {
        return ptkpCode;
    }

    public void setPtkpCode(String ptkpCode) {
        this.ptkpCode = ptkpCode;
    }

    public BigDecimal getPtkpAmount() {
        return ptkpAmount;
    }

    public void setPtkpAmount(BigDecimal ptkpAmount) {
        this.ptkpAmount = ptkpAmount;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public HrPerson getPerson() {
        return person;
    }

    public void setPerson(HrPerson person) {
        this.person = person;
    }
}
