package com.apptitle.classsection.controller;

import com.apptitle.classsection.dto.ClassSectionResponse;
import com.apptitle.classsection.service.ClassSectionService;
import com.apptitle.joinrequest.dto.ClassEnrollmentRequestResponse;
import com.apptitle.joinrequest.service.ClassEnrollmentRequestService;
import com.apptitle.subject.dto.SubjectResponse;
import com.apptitle.subject.service.ClassSubjectLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student endpoints for class section and subject access.
 */
@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassSectionController {

    private final ClassEnrollmentRequestService classEnrollmentRequestService;
    private final ClassSectionService classSectionService;
    private final ClassSubjectLinkService classSubjectLinkService;

    public StudentClassSectionController(
            ClassEnrollmentRequestService classEnrollmentRequestService,
            ClassSectionService classSectionService,
            ClassSubjectLinkService classSubjectLinkService
    ) {
        this.classEnrollmentRequestService = classEnrollmentRequestService;
        this.classSectionService = classSectionService;
        this.classSubjectLinkService = classSubjectLinkService;
    }

    @PostMapping("/class-join-requests")
    public ResponseEntity<ClassEnrollmentRequestResponse> createJoinRequest(
            @RequestBody String classCode,
            Authentication authentication
    ) {
        // Implementation will be updated after JoinRequest is renamed to ClassEnrollmentRequest
        return null;
    }

    @GetMapping("/class-join-requests")
    public ResponseEntity<List<ClassEnrollmentRequestResponse>> listOwnJoinRequests(Authentication authentication) {
        // Implementation will be updated after JoinRequest is renamed to ClassEnrollmentRequest
        return null;
    }

    @GetMapping("/class-section")
    public ResponseEntity<ClassSectionResponse> getApprovedClassSection(Authentication authentication) {
        // Implementation will be updated after JoinRequest is renamed to ClassEnrollmentRequest
        return null;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectResponse>> listApprovedSubjects(Authentication authentication) {
        // Implementation will be added after ClassSubjectLink is fully implemented
        return null;
    }
}