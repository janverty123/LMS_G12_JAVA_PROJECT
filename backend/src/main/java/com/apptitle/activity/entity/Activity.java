package com.apptitle.activity.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.teacher.entity.Teacher;
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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "activities")
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_link_id", nullable = false)
    private ClassSubjectLink classSubjectLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Teacher createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal perfectScore;

    @Column(nullable = false)
    private Instant deadline;

    @Column(nullable = false, length = 5000)
    private String instructions;

    @Column(nullable = false)
    private boolean allowStudentSelfSubmissionScore;
}
