package com.apptitle.material.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.teacher.entity.Teacher;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "learning_materials")
public class LearningMaterial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_link_id", nullable = false)
    private ClassSubjectLink classSubjectLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private Teacher uploadedBy;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false, unique = true, length = 700)
    private String storageKey;

    @Column(nullable = false)
    private long fileSizeBytes;

    @Column(nullable = false, unique = true, length = 255)
    private String uploadId;

    @Column(nullable = false)
    private long chunkSizeBytes;

    @Column(nullable = false)
    private int totalParts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningMaterialStatus status = LearningMaterialStatus.INITIALIZED;

    private Instant completedAt;

    @OneToMany(
            mappedBy = "learningMaterial",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MaterialChunk> chunks = new ArrayList<>();
}
