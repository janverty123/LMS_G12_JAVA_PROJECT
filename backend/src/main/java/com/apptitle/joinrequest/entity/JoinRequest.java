package com.apptitle.joinrequest.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.section.entity.Section;
import com.apptitle.student.entity.Student;
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
 * A student's request to join a Section. Created automatically during
 * student registration (student typed a valid classroom code), starting in
 * PENDING status. A student only has real access to a section's materials/
 * activities/grades once a teacher moves this to APPROVED — that transition,
 * along with the teacher's pending-requests list and member management, is
 * Phase 3 work building on this entity.
 *
 * Unique per (student, section): a student can't have two outstanding
 * requests for the same classroom. Re-requesting after a DECLINE isn't
 * supported yet — deliberately left as a Phase 3 decision (e.g. whether a
 * decline resets the row to PENDING vs. requires teacher-initiated re-add).
 */
@Getter
@Setter
@Entity
@Table(
        name = "join_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"})
)
public class JoinRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status = JoinRequestStatus.PENDING;
}
