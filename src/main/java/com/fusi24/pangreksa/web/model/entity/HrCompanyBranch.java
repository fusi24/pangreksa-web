package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "hr_company_branch",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_branch_code", columnNames = {"company_id", "branch_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrCompanyBranch extends AuditableEntity<HrCompanyBranch> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private HrCompany company;

    @Column(name = "branch_code", length = 30, nullable = false)
    private String branchCode;

    @Column(name = "branch_name", length = 150, nullable = false)
    private String branchName;

    @Column(name = "branch_address")
    private String branchAddress;

    @Column(name = "branch_address_city", length = 100)
    private String branchAddressCity;

    @Column(name = "branch_address_province", length = 100)
    private String branchAddressProvince;

    /**
     * Simpan IANA timezone, contoh:
     * Asia/Jakarta, Asia/Makassar, Asia/Jayapura
     */
    @Column(name = "branch_timezone", length = 60)
    private String branchTimezone;

    @Column(name = "branch_latitude", precision = 9, scale = 6)
    private BigDecimal branchLatitude;

    @Column(name = "branch_longitude", precision = 9, scale = 6)
    private BigDecimal branchLongitude;
}
