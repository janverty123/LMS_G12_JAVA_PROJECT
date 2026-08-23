package com.apptitle.joinrequest.controller;

import com.apptitle.joinrequest.dto.CreateJoinRequestRequest;
import com.apptitle.joinrequest.dto.JoinRequestResponse;
import com.apptitle.joinrequest.dto.StudentSectionResponse;
import com.apptitle.joinrequest.service.JoinRequestService;
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

/**
 * Covers joining classrooms AFTER registration (a student's very first
 * join request is created inline during registration itself — see
 * AuthService — since that flow requires a code up front too). This is
 * for every subsequent classroom a student wants to join.
 */
@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentJoinRequestController {

    private final JoinRequestService joinRequestService;

    public StudentJoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @PostMapping("/join-requests")
    public ResponseEntity<JoinRequestResponse> createJoinRequest(
            @Valid @RequestBody CreateJoinRequestRequest request, Authentication authentication) {
        JoinRequestResponse response = joinRequestService.createJoinRequest(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> listOwnJoinRequests(Authentication authentication) {
        return ResponseEntity.ok(joinRequestService.listOwnJoinRequests(authentication.getName()));
    }

    @GetMapping("/sections")
    public ResponseEntity<List<StudentSectionResponse>> listOwnApprovedSections(Authentication authentication) {
        return ResponseEntity.ok(joinRequestService.listOwnApprovedSections(authentication.getName()));
    }
}
