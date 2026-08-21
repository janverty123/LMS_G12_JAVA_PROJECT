package com.apptitle.section.entity;

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
 * A teacher's classroom for one section+subject (e.g. "Grade 12 - Rossum" /
 * "ICT Programming") — Google Classroom-style: one teacher creates a
 * separate Section per class they teach, each with its own join code.
 *
 * classCode is how a student identifies which classroom to request joining
 * at registration (typed in, not picked from a list/browser). It plays no
 * verification role beyond "does this code correspond to a real classroom" —
 * actually getting in still requires teacher approval of the resulting
 * JoinRequest.
 */
@Getter
@Setter
@Entity
@Table(name = "sections")
public class Section extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subjectName;

    @Column(nullable = false, unique = true, length = 8)
    private String classCode;
}
