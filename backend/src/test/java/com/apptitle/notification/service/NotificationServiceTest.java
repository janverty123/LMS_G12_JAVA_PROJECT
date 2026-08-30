package com.apptitle.notification.service;

import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.notification.entity.Notification;
import com.apptitle.notification.entity.NotificationType;
import com.apptitle.notification.repository.NotificationRepository;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    @Mock private NotificationRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    private NotificationService service;
    private User student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NotificationService(repository, userRepository, enrollmentRepository);
        student = new User(); student.setId(UUID.randomUUID()); student.setEmail("one@example.com");
        when(userRepository.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(student));
    }

    @Test
    void listReturnsOnlyCurrentUsersNotifications() {
        Notification own = new Notification(); own.setId(UUID.randomUUID());
        own.setRecipient(student); own.setType(NotificationType.NEW_ACTIVITY);
        own.setMessage("New activity"); own.setReferenceKey("activity-1");
        when(repository.findByRecipientIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(List.of(own));
        assertEquals(1, service.list(student.getEmail()).size());
    }

    @Test
    void markReadCannotAccessAnotherStudentsNotification() {
        UUID otherNotificationId = UUID.randomUUID();
        when(repository.findByIdAndRecipientId(otherNotificationId, student.getId()))
                .thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () ->
                service.markRead(student.getEmail(), otherNotificationId));
        assertEquals(404, exception.getStatus().value());
    }
}
