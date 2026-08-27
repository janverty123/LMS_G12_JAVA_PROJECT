package com.apptitle.classsection.controller;

import com.apptitle.classsection.dto.ClassEnrollmentRequestResponse;
import com.apptitle.classsection.dto.CreateClassJoinRequestRequest;
import com.apptitle.classsection.dto.StudentClassSectionResponse;
import com.apptitle.classsection.service.ClassEnrollmentRequestService;
import com.apptitle.subject.dto.SubjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassSectionController {

    private final ClassEnrollmentRequestService classEnrollmentRequestService;

    public StudentClassSectionController(
            ClassEnrollmentRequestService classEnrollmentRequestService
    ) {
        this.classEnrollmentRequestService = classEnrollmentRequestService;
    }

    @PostMapping("/class-join-requests")
    public ResponseEntity<ClassEnrollmentRequestResponse> createJoinRequest(
            @Valid @RequestBody CreateClassJoinRequestRequest request,
            Authentication authentication
    ) {
        ClassEnrollmentRequestResponse response = classEnrollmentRequestService.createJoinRequest(
                authentication.getName(), request.classCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/class-join-requests")
    public ResponseEntity<List<ClassEnrollmentRequestResponse>> listOwnJoinRequests(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                classEnrollmentRequestService.listOwnJoinRequests(authentication.getName()));
    }

    @GetMapping("/class-section")
    public ResponseEntity<StudentClassSectionResponse> getApprovedClassSection(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                classEnrollmentRequestService.getOwnApprovedClassSection(authentication.getName()));
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectResponse>> listApprovedSubjects(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                classEnrollmentRequestService.listOwnApprovedSubjects(authentication.getName()));
    }
}
