package com.apptitle.section.repository;

import com.apptitle.section.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findByTeacherId(UUID teacherId);

    Optional<Section> findByClassCode(String classCode);

    boolean existsByClassCode(String classCode);
}
