package com.apptitle.subject.repository;

import com.apptitle.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findBySubjectTeacherId(UUID teacherId);

    Optional<Subject> findBySubjectCode(String subjectCode);

    boolean existsByName(String name);
}