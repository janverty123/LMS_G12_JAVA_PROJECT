package com.apptitle.classsection.repository;

import com.apptitle.classsection.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSectionRepository extends JpaRepository<ClassSection, UUID> {

    List<ClassSection> findByAdviserId(UUID adviserId);

    Optional<ClassSection> findByClassCode(String classCode);

    boolean existsByClassCode(String classCode);
}