package com.apptitle.activity.repository;

import com.apptitle.activity.entity.ActivityFile;
import com.apptitle.activity.entity.ActivityFilePurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityFileRepository extends JpaRepository<ActivityFile, UUID> {
    List<ActivityFile> findByActivityIdAndPurposeAndCompletedTrue(
            UUID activityId, ActivityFilePurpose purpose);
}
