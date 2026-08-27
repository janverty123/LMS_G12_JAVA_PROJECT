package com.apptitle.material.repository;

import com.apptitle.material.entity.LearningMaterial;
import com.apptitle.material.entity.LearningMaterialStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {

    List<LearningMaterial> findByClassSubjectLinkIdAndStatusOrderByCreatedAtDesc(
            UUID classSubjectLinkId,
            LearningMaterialStatus status
    );
}
