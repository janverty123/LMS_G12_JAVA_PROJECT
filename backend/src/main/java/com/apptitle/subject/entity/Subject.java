package com.apptitle.subject.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.teacher.entity.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an academic subject (e.g., "Mathematics") owned by a Subject Teacher.
 * This is a new entity for the ClassSection/Subject migration per PROJECT_STATUS.md Section 5.
 */
@Getter
@Setter
@Entity
@Table(name = "subjects")
public class Subject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_teacher_id", nullable = false)
    private Teacher subjectTeacher;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Mathematics"

    @Column(nullable = false, unique = true, length = 7)
    private String subjectCode; // e.g., "MATH7K2"
}