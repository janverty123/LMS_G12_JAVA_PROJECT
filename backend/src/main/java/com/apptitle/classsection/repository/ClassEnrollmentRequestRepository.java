package com.apptitle.classsection.repository;

import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassEnrollmentRequestRepository extends JpaRepository<ClassEnrollmentRequest, UUID> {

    List<ClassEnrollmentRequest> findByClassSectionIdAndStatus(
            UUID classSectionId, ClassEnrollmentRequestStatus status);

    List<ClassEnrollmentRequest> findByStudentId(UUID studentId);

    List<ClassEnrollmentRequest> findByStudentIdAndStatus(
            UUID studentId, ClassEnrollmentRequestStatus status);

    Optional<ClassEnrollmentRequest> findByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

    boolean existsByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

    boolean existsByStudentIdAndStatus(UUID studentId, ClassEnrollmentRequestStatus status);

    void deleteByClassSectionId(UUID classSectionId);

    void deleteByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);
}
