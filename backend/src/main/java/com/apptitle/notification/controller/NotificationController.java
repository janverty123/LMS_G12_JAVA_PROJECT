package com.apptitle.notification.controller;

import com.apptitle.notification.dto.NotificationResponse;
import com.apptitle.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(Authentication auth) {
        return ResponseEntity.ok(service.list(auth.getName()));
    }
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> read(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.markRead(auth.getName(), id));
    }
    @PutMapping("/read-all")
    public ResponseEntity<Void> readAll(Authentication auth) {
        service.markAllRead(auth.getName()); return ResponseEntity.noContent().build();
    }
}
