package com.apptitle.activity.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.student.entity.Student;
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
@Table(name = "activity_files")
public class ActivityFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityFilePurpose purpose;

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

    @Column(nullable = false)
    private boolean completed;

    private Instant completedAt;

    @OneToMany(mappedBy = "activityFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityFileChunk> chunks = new ArrayList<>();
}
