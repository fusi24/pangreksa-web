package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.SystemTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fw_system", schema = "public")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FwSystem extends AuditableEntity<FwSystem> {
    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private SystemTypeEnum type;

    @Column(name = "key", length = 250)
    private String key;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "string_val", length = 250)
    private String stringVal;

    @Column(name = "boolean_val")
    private Boolean booleanVal;

    @Column(name = "int_val")
    private Integer intVal;

    @Column(name = "date_val")
    private LocalDate dateVal;

    @Column(name = "datetime_val")
    private LocalDateTime datetimeVal;

    @Column(name = "decimal_val")
    private BigDecimal decimalVal;

}

