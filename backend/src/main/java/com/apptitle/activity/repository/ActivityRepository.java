package com.apptitle.activity.repository;

import com.apptitle.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByClassSubjectLinkIdOrderByDeadlineAsc(UUID classSubjectLinkId);
    List<Activity> findByDeadlineBetween(Instant from, Instant to);
}
