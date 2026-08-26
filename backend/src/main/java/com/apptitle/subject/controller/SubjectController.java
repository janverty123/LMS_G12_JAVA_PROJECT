package com.apptitle.subject.controller;

import com.apptitle.subject.dto.SubjectResponse;
import com.apptitle.subject.dto.CreateSubjectRequest;
import com.apptitle.subject.dto.UpdateSubjectRequest;
import com.apptitle.subject.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

/**
 * Teacher-only endpoints for managing Subjects.
 */
@RestController
@RequestMapping("/api/teacher/subjects")
@PreAuthorize("hasRole('TEACHER')")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody CreateSubjectRequest request,
            Authentication authentication
    ) {
        SubjectResponse response = subjectService.createSubject(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> listOwnSubjects(Authentication authentication) {
        return ResponseEntity.ok(subjectService.listOwnSubjects(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(subjectService.updateSubject(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID id, Authentication authentication) {
        subjectService.deleteSubject(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}