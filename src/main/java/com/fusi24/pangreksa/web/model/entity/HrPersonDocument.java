package com.fusi24.pangreksa.web.model.entity;

import com.fusi24.pangreksa.web.model.enumerate.ContentTypeEnum;
import com.fusi24.pangreksa.web.model.enumerate.DocumentTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hr_document")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonDocument extends AuditableEntity<HrPersonDocument>  {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_hr_seq")
    @SequenceGenerator(name = "global_hr_seq", sequenceName = "global_hr_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String description;

    @Column(length = 50)
    private String notes;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DocumentTypeEnum type;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentTypeEnum contentType;

    private Long size;

    @Column(length = 50)
    private String filename;

    @Column(length = 250)
    private String path;

    @ManyToOne
    @JoinColumn(name = "reference_id", nullable = false)
    private HrPerson person;
}
