package com.apptitle.progress.controller;

import com.apptitle.progress.dto.StudentProgressResponse;
import com.apptitle.progress.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/students/me/subjects/{subjectId}/progress")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProgressController {
    private final ProgressService service;
    public StudentProgressController(ProgressService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<StudentProgressResponse> own(@PathVariable UUID subjectId,
            Authentication authentication) {
        return ResponseEntity.ok(service.own(authentication.getName(), subjectId));
    }
}
