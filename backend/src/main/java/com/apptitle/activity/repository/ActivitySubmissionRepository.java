package com.apptitle.activity.repository;

import com.apptitle.activity.entity.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivitySubmissionRepository extends JpaRepository<ActivitySubmission, UUID> {
    Optional<ActivitySubmission> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
    List<ActivitySubmission> findByActivityId(UUID activityId);
}
