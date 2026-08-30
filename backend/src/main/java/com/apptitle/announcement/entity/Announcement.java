package com.apptitle.announcement.entity;

import com.apptitle.classsection.entity.ClassSection;
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

@Getter
@Setter
@Entity
@Table(name = "announcements")
public class Announcement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Teacher author;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;
}
