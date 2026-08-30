package com.apptitle.grade.controller;

import com.apptitle.grade.dto.GradebookResponse;
import com.apptitle.grade.service.GradebookService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/students/me/subjects/{subjectId}/grades")
@PreAuthorize("hasRole('STUDENT')")
public class StudentGradeController {
    private final GradebookService gradebookService;

    public StudentGradeController(GradebookService gradebookService) {
        this.gradebookService = gradebookService;
    }

    @GetMapping
    public ResponseEntity<GradebookResponse.StudentGrade> grades(
            @PathVariable UUID subjectId, Authentication authentication) {
        return ResponseEntity.ok(gradebookService.studentGrades(
                authentication.getName(), subjectId));
    }
}
