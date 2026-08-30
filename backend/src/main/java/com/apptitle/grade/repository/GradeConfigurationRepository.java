package com.apptitle.grade.repository;

import com.apptitle.grade.entity.GradeConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GradeConfigurationRepository extends JpaRepository<GradeConfiguration, UUID> {
    Optional<GradeConfiguration> findByClassSubjectLinkId(UUID classSubjectLinkId);
}
