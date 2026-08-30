package com.apptitle.announcement.repository;

import com.apptitle.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    List<Announcement> findByClassSectionIdOrderByCreatedAtDesc(UUID classSectionId);
}
