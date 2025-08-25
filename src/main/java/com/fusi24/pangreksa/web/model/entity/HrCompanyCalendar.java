package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.CalendarTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "hr_company_calendar", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrCompanyCalendar extends AuditableEntity<HrCompanyCalendar> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_fw_seq")
    @SequenceGenerator(name = "global_fw_seq", sequenceName = "global_fw_seq", allocationSize = 1)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", nullable = false, length = 100)
    private CalendarTypeEnum calendarType;  // could be ENUM in the future

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "based_on", columnDefinition = "TEXT")
    private String basedOn;

    @Column(name = "document_file", length = 255)
    private String documentFile;
}
