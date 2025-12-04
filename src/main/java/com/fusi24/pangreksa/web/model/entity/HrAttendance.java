package com.fusi24.pangreksa.web.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_attendance")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appuser_id", nullable = false)
    private FwAppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private HrPerson person; // Direct link to hr_person

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_schedule_id", nullable = false)
    private HrWorkSchedule workSchedule; // Direct link to hr_work_schedule

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "status")
    private String status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
}