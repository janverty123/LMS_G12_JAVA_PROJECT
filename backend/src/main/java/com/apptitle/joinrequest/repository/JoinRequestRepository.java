package com.apptitle.joinrequest.repository;

import com.apptitle.joinrequest.entity.JoinRequest;
import com.apptitle.joinrequest.entity.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

    List<JoinRequest> findByStudentId(UUID studentId);

    List<JoinRequest> findBySectionId(UUID sectionId);

    List<JoinRequest> findBySectionIdAndStatus(UUID sectionId, JoinRequestStatus status);

    Optional<JoinRequest> findByStudentIdAndSectionId(UUID studentId, UUID sectionId);

    boolean existsByStudentIdAndSectionId(UUID studentId, UUID sectionId);

    void deleteBySectionId(UUID sectionId);
}
