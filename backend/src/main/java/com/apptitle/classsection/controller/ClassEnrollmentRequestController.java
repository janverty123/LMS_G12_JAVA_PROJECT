package com.apptitle.classsection.controller;

import com.apptitle.classsection.dto.ClassEnrollmentRequestResponse;
import com.apptitle.classsection.dto.SectionMemberResponse;
import com.apptitle.classsection.service.ClassEnrollmentRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class ClassEnrollmentRequestController {

    private final ClassEnrollmentRequestService classEnrollmentRequestService;

    public ClassEnrollmentRequestController(
            ClassEnrollmentRequestService classEnrollmentRequestService
    ) {
        this.classEnrollmentRequestService = classEnrollmentRequestService;
    }

    @GetMapping("/class-sections/{classSectionId}/join-requests")
    public ResponseEntity<List<ClassEnrollmentRequestResponse>> listPending(
            @PathVariable UUID classSectionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classEnrollmentRequestService.listPendingForClassSection(
                authentication.getName(), classSectionId));
    }

    @PutMapping("/class-enrollment-requests/{id}/approve")
    public ResponseEntity<ClassEnrollmentRequestResponse> approve(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                classEnrollmentRequestService.approve(authentication.getName(), id));
    }

    @PutMapping("/class-enrollment-requests/{id}/decline")
    public ResponseEntity<ClassEnrollmentRequestResponse> decline(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                classEnrollmentRequestService.decline(authentication.getName(), id));
    }

    @GetMapping("/class-sections/{classSectionId}/students")
    public ResponseEntity<List<SectionMemberResponse>> listMembers(
            @PathVariable UUID classSectionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classEnrollmentRequestService.listMembers(
                authentication.getName(), classSectionId));
    }

    @DeleteMapping("/class-sections/{classSectionId}/students/{studentId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID classSectionId,
            @PathVariable UUID studentId,
            Authentication authentication
    ) {
        classEnrollmentRequestService.removeMember(
                authentication.getName(), classSectionId, studentId);
        return ResponseEntity.noContent().build();
    }
}
