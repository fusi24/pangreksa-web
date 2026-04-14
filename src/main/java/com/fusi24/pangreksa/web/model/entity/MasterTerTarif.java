package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "master_ter_tarif")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterTerTarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jenis_ter", nullable = false, length = 10)
    private String jenisTer;

    @Column(name = "bruto_min", precision = 18, scale = 2, nullable = false)
    private BigDecimal brutoMin;

    @Column(name = "bruto_max", precision = 18, scale = 2, nullable = false)
    private BigDecimal brutoMax;

    @Column(name = "tarif_persen", precision = 5, scale = 2, nullable = false)
    private BigDecimal tarifPersen;

    @Column(name = "urutan", nullable = false)
    private Integer urutan;

    @Column(name = "aktif")
    private Boolean aktif;
}