package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_ter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterTer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jenis_ter", nullable = false, length = 10)
    private String jenisTer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_ptkp_id", nullable = false)
    private MasterPtkp masterPtkp;

    @Column(name = "aktif")
    private Boolean aktif;
}