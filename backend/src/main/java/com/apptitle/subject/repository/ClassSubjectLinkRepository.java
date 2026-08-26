package com.apptitle.subject.repository;

import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassSubjectLinkRepository extends JpaRepository<ClassSubjectLink, UUID> {

    List<ClassSubjectLink> findBySubjectIdAndStatus(UUID subjectId, ClassSubjectLinkStatus status);

    List<ClassSubjectLink> findByClassSectionId(UUID classSectionId);

    boolean existsByClassSectionIdAndSubjectId(UUID classSectionId, UUID subjectId);
}