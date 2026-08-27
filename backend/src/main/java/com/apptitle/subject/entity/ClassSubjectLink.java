package com.apptitle.subject.entity;

import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.teacher.entity.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a link between a ClassSection and a Subject, which goes through
 * a request/approval workflow initiated by the Class Adviser and approved by
 * the Subject Teacher.
 */
@Getter
@Setter
@Entity
@Table(
        name = "class_subject_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_section_id", "subject_id"})
)
public class ClassSubjectLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassSubjectLinkStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private Teacher requestedBy;
}
