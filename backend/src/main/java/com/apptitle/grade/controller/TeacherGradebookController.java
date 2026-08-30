package com.apptitle.grade.controller;

import com.apptitle.grade.dto.GradeConfigurationRequest;
import com.apptitle.grade.dto.GradeConfigurationResponse;
import com.apptitle.grade.dto.GradebookResponse;
import com.apptitle.grade.service.GradebookService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/class-sections/{classId}/subjects/{subjectId}")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherGradebookController {
    private final GradebookService gradebookService;

    public TeacherGradebookController(GradebookService gradebookService) {
        this.gradebookService = gradebookService;
    }

    @PutMapping("/grade-configuration")
    public ResponseEntity<GradeConfigurationResponse> configure(
            @PathVariable UUID classId, @PathVariable UUID subjectId,
            @Valid @RequestBody GradeConfigurationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(gradebookService.configure(authentication.getName(),
                classId, subjectId, request));
    }

    @GetMapping("/gradebook")
    public ResponseEntity<GradebookResponse> gradebook(
            @PathVariable UUID classId, @PathVariable UUID subjectId,
            Authentication authentication) {
        return ResponseEntity.ok(gradebookService.teacherGradebook(
                authentication.getName(), classId, subjectId));
    }

    @GetMapping("/gradebook/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID classId,
            @PathVariable UUID subjectId, Authentication authentication) {
        byte[] file = gradebookService.export(authentication.getName(), classId, subjectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("gradebook.xlsx").build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
