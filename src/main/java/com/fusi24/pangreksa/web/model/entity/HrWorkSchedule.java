package com.fusi24.pangreksa.web.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fusi24.pangreksa.web.model.enumerate.WorkScheduleLabel;
import com.fusi24.pangreksa.web.model.enumerate.WorkScheduleType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrWorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkScheduleType type;

    @Column(nullable = false)
    private LocalTime checkIn;

    @Column(nullable = false)
    private LocalTime checkOut;

    private LocalTime breakStart;
    private LocalTime breakEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkScheduleLabel label;

    @Column(name = "is_overtime_auto")
    private Boolean isOvertimeAuto = false;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "assignment_scope", nullable = false)
    private String assignmentScope = "All"; // "All" or "Selected"

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HrWorkScheduleAssignment> assignments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}