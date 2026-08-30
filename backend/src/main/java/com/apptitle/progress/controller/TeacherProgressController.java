package com.apptitle.progress.controller;

import com.apptitle.progress.dto.ProgressConfigurationRequest;
import com.apptitle.progress.dto.StudentProgressResponse;
import com.apptitle.progress.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/class-sections/{classId}/subjects/{subjectId}/progress")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherProgressController {
    private final ProgressService service;
    public TeacherProgressController(ProgressService service) { this.service = service; }

    @PutMapping("/configuration")
    public ResponseEntity<Void> configure(@PathVariable UUID classId,
            @PathVariable UUID subjectId,
            @Valid @RequestBody ProgressConfigurationRequest request,
            Authentication authentication) {
        service.configure(authentication.getName(), classId, subjectId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<StudentProgressResponse>> dashboard(
            @PathVariable UUID classId, @PathVariable UUID subjectId,
            Authentication authentication) {
        return ResponseEntity.ok(service.dashboard(authentication.getName(), classId, subjectId));
    }
}
