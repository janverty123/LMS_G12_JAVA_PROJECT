package com.apptitle.activity.entity;

import com.apptitle.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "activity_file_chunks", uniqueConstraints =
        @UniqueConstraint(columnNames = {"activity_file_id", "part_number"}))
public class ActivityFileChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_file_id", nullable = false)
    private ActivityFile activityFile;

    @Column(name = "part_number", nullable = false)
    private int partNumber;

    @Column(name = "e_tag", nullable = false, length = 255)
    private String eTag;

    @Column(nullable = false)
    private long sizeBytes;
}
