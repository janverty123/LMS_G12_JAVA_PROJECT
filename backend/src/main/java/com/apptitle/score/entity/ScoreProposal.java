package com.apptitle.score.entity;

import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivityFile;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.student.entity.Student;
import com.apptitle.teacher.entity.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "score_proposals", uniqueConstraints =
        @UniqueConstraint(columnNames = {"activity_id", "student_id"}))
public class ScoreProposal extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proof_file_id", nullable = false)
    private ActivityFile proofFile;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal reportedScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal approvedScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScoreProposalStatus status;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Teacher reviewedBy;
}
