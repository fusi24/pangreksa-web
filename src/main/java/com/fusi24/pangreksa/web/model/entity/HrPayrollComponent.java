package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_component")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrPayrollComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RELATION
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_calculation_id", nullable = false)
    private HrPayrollCalculation payrollCalculation;

    // TYPE
    @Column(name = "component_type", nullable = false)
    private String componentType; // EARNING / DEDUCTION

    @Column(name = "component_group", nullable = false)
    private String componentGroup;

    @Column(name = "component_code")
    private String componentCode;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
