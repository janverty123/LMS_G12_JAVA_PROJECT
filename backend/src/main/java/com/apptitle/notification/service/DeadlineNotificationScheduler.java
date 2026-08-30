package com.apptitle.notification.service;

import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.notification.entity.NotificationType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DeadlineNotificationScheduler {
    private final ActivityRepository activityRepository;
    private final NotificationService notificationService;

    public DeadlineNotificationScheduler(ActivityRepository activityRepository,
            NotificationService notificationService) {
        this.activityRepository = activityRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void notifyUpcomingDeadlines() {
        Instant now = Instant.now();
        activityRepository.findByDeadlineBetween(now, now.plus(24, ChronoUnit.HOURS))
                .forEach(activity -> notificationService.notifyApprovedStudents(
                        activity.getClassSubjectLink().getClassSection(),
                        NotificationType.UPCOMING_DEADLINE,
                        "Upcoming deadline: " + activity.getTitle(),
                        activity.getId() + ":deadline"));
    }
}
