package com.apptitle.material.repository;

import com.apptitle.material.entity.MaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, UUID> {
}
