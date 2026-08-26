package com.apptitle.classsection.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.joinrequest.entity.JoinRequestStatus;
import com.apptitle.student.entity.Student;
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

/**
 * Replaces the old JoinRequest entity to represent a student's request to join a ClassSection.
 * This is part of the ClassSection/Subject migration per PROJECT_STATUS.md Section 5.
 */
@Getter
@Setter
@Entity
@Table(name = "class_enrollment_requests")
public class ClassEnrollmentRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status;
}