package com.apptitle.classsection.repository;

import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.joinrequest.entity.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassEnrollmentRequestRepository extends JpaRepository<ClassEnrollmentRequest, UUID> {

    List<ClassEnrollmentRequest> findByClassSectionIdAndStatus(UUID classSectionId, JoinRequestStatus status);

    List<ClassEnrollmentRequest> findByStudentId(UUID studentId);

    Optional<ClassEnrollmentRequest> findByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

    boolean existsByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

    void deleteByClassSectionId(UUID classSectionId);

    void deleteByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);
}