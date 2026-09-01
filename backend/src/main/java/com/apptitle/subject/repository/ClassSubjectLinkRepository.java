package com.apptitle.subject.repository;

import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSubjectLinkRepository extends JpaRepository<ClassSubjectLink, UUID> {

    List<ClassSubjectLink> findBySubjectIdAndStatus(UUID subjectId, ClassSubjectLinkStatus status);

    List<ClassSubjectLink> findBySubjectId(UUID subjectId);

    List<ClassSubjectLink> findByClassSectionId(UUID classSectionId);

    List<ClassSubjectLink> findByClassSectionIdAndStatus(
            UUID classSectionId, ClassSubjectLinkStatus status);

    Optional<ClassSubjectLink> findByClassSectionIdAndSubjectId(
            UUID classSectionId, UUID subjectId);

    boolean existsByClassSectionIdAndSubjectId(UUID classSectionId, UUID subjectId);

    void deleteByClassSectionId(UUID classSectionId);

    void deleteBySubjectId(UUID subjectId);
}
