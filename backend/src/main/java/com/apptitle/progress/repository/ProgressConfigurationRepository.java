package com.apptitle.progress.repository;

import com.apptitle.progress.entity.ProgressConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgressConfigurationRepository extends JpaRepository<ProgressConfiguration, UUID> {
    Optional<ProgressConfiguration> findByClassSubjectLinkId(UUID linkId);
}
