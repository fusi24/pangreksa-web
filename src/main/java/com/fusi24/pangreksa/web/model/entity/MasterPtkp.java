package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "master_ptkp")
public class MasterPtkp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_ptkp", nullable = false, unique = true)
    private String kodePtkp;

    @Column(nullable = false)
    private String golongan; // TK, K, KI

    @Column(name = "jumlah_tanggungan", nullable = false)
    private Integer jumlahTanggungan;

    @Column(nullable = false)
    private BigDecimal nominal;

    private Boolean aktif = true;

    // ===== GETTER & SETTER =====

    public Long getId() {
        return id;
    }

    public String getKodePtkp() {
        return kodePtkp;
    }

    public void setKodePtkp(String kodePtkp) {
        this.kodePtkp = kodePtkp;
    }

    public String getGolongan() {
        return golongan;
    }

    public void setGolongan(String golongan) {
        this.golongan = golongan;
    }

    public Integer getJumlahTanggungan() {
        return jumlahTanggungan;
    }

    public void setJumlahTanggungan(Integer jumlahTanggungan) {
        this.jumlahTanggungan = jumlahTanggungan;
    }

    public BigDecimal getNominal() {
        return nominal;
    }

    public void setNominal(BigDecimal nominal) {
        this.nominal = nominal;
    }

    public Boolean getAktif() {
        return aktif;
    }

    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }
}
