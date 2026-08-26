package com.apptitle.classsection.entity;

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
 * Represents a classroom section (e.g., "Grade 12 - A") owned by a Class Adviser.
 * This replaces the old Section entity and separates the classroom/roster concept
 * from the Subject concept, per SRS §6 and PROJECT_STATUS.md Section 5.
 */
@Getter
@Setter
@Entity
@Table(name = "class_sections")
public class ClassSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adviser_id", nullable = false)
    private Teacher adviser;

    @Column(nullable = false)
    private String gradeLevel; // e.g., "Grade 12"

    @Column(nullable = false)
    private String section; // e.g., "A"

    @Column(nullable = false)
    private String schoolYear; // e.g., "2026-2027"

    @Column(nullable = false, unique = true, length = 6)
    private String classCode; // e.g., "A7K2P9"
}