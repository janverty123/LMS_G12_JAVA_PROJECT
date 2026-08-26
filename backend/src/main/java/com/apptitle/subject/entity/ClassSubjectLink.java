package com.apptitle.subject.entity;

import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a link between a ClassSection and a Subject, which goes through
 * a request/approval workflow initiated by the Class Adviser and approved by
 * the Subject Teacher.
 */
@Getter
@Setter
@Entity
@Table(name = "class_subject_links")
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

    @Column(nullable = false)
    private UUID requestedBy; // Teacher ID who initiated the link
}