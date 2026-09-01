package com.apptitle.subject.controller;

import com.apptitle.subject.dto.ClassSubjectLinkResponse;
import com.apptitle.subject.dto.CreateClassSubjectLinkRequest;
import com.apptitle.subject.service.ClassSubjectLinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
 * Teacher-only endpoints for managing ClassSection-Subject links.
 */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class ClassSubjectLinkController {

    private final ClassSubjectLinkService classSubjectLinkService;

    public ClassSubjectLinkController(ClassSubjectLinkService classSubjectLinkService) {
        this.classSubjectLinkService = classSubjectLinkService;
    }

    @PostMapping("/class-sections/{classSectionId}/subject-links")
    public ResponseEntity<ClassSubjectLinkResponse> createLinkRequest(
            @PathVariable UUID classSectionId,
            @Valid @RequestBody CreateClassSubjectLinkRequest request,
            Authentication authentication
    ) {
        ClassSubjectLinkResponse response = classSubjectLinkService.createLinkRequest(
                authentication.getName(), classSectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/subject-links/{id}/approve")
    public ResponseEntity<ClassSubjectLinkResponse> approveLink(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSubjectLinkService.approveLink(authentication.getName(), id));
    }

    @PutMapping("/subject-links/{id}/decline")
    public ResponseEntity<ClassSubjectLinkResponse> declineLink(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSubjectLinkService.declineLink(authentication.getName(), id));
    }

    @GetMapping("/subjects/{subjectId}/link-requests")
    public ResponseEntity<List<ClassSubjectLinkResponse>> listPendingLinksForSubject(
            @PathVariable UUID subjectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSubjectLinkService.listPendingLinksForSubject(authentication.getName(), subjectId));
    }

    @GetMapping("/subjects/{subjectId}/links")
    public ResponseEntity<List<ClassSubjectLinkResponse>> listLinksForSubject(
            @PathVariable UUID subjectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSubjectLinkService.listLinksForSubject(authentication.getName(), subjectId));
    }

    @GetMapping("/class-sections/{classSectionId}/subjects")
    public ResponseEntity<List<ClassSubjectLinkResponse>> listSubjectsForClassSection(
            @PathVariable UUID classSectionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSubjectLinkService.listLinksForClassSection(authentication.getName(), classSectionId));
    }
}
