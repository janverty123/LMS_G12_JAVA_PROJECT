package com.apptitle.announcement.controller;

import com.apptitle.announcement.dto.AnnouncementResponse;
import com.apptitle.announcement.dto.SaveAnnouncementRequest;
import com.apptitle.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AnnouncementController {
    private final AnnouncementService service;
    public AnnouncementController(AnnouncementService service) { this.service = service; }

    @PostMapping("/teacher/class-sections/{classId}/announcements")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AnnouncementResponse> create(@PathVariable UUID classId,
            @Valid @RequestBody SaveAnnouncementRequest request, Authentication auth) {
        return ResponseEntity.ok(service.create(auth.getName(), classId, request));
    }
    @GetMapping("/teacher/class-sections/{classId}/announcements")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AnnouncementResponse>> teacherList(@PathVariable UUID classId,
            Authentication auth) { return ResponseEntity.ok(service.teacherList(auth.getName(), classId)); }
    @PutMapping("/teacher/announcements/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AnnouncementResponse> update(@PathVariable UUID id,
            @Valid @RequestBody SaveAnnouncementRequest request, Authentication auth) {
        return ResponseEntity.ok(service.update(auth.getName(), id, request));
    }
    @DeleteMapping("/teacher/announcements/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        service.delete(auth.getName(), id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/students/me/announcements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AnnouncementResponse>> studentList(Authentication auth) {
        return ResponseEntity.ok(service.studentList(auth.getName()));
    }
}
