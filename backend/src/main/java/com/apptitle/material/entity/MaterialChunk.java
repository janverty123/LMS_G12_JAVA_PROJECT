package com.apptitle.material.entity;

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
@Table(
        name = "material_chunks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"learning_material_id", "part_number"})
)
public class MaterialChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_material_id", nullable = false)
    private LearningMaterial learningMaterial;

    @Column(name = "part_number", nullable = false)
    private int partNumber;

    @Column(nullable = false, length = 255)
    private String eTag;

    @Column(nullable = false)
    private long sizeBytes;
}
