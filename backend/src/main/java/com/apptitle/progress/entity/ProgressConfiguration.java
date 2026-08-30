package com.apptitle.progress.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.subject.entity.ClassSubjectLink;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "progress_configurations")
public class ProgressConfiguration extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_link_id", nullable = false, unique = true)
    private ClassSubjectLink classSubjectLink;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal onTrackMinimum;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal needsAttentionMinimum;
}
