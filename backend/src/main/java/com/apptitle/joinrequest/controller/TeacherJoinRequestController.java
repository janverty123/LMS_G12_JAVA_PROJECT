package com.apptitle.joinrequest.controller;

import com.apptitle.joinrequest.dto.JoinRequestResponse;
import com.apptitle.joinrequest.dto.SectionMemberResponse;
import com.apptitle.joinrequest.service.JoinRequestService;
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
@PreAuthorize("hasRole('TEACHER')")
public class TeacherJoinRequestController {

    private final JoinRequestService joinRequestService;

    public TeacherJoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @GetMapping("/api/teacher/sections/{sectionId}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> listPending(
            @PathVariable UUID sectionId, Authentication authentication) {
        return ResponseEntity.ok(joinRequestService.listPendingForSection(authentication.getName(), sectionId));
    }

    @PutMapping("/api/teacher/join-requests/{id}/approve")
    public ResponseEntity<JoinRequestResponse> approve(
            @PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(joinRequestService.approve(authentication.getName(), id));
    }

    @PutMapping("/api/teacher/join-requests/{id}/decline")
    public ResponseEntity<JoinRequestResponse> decline(
            @PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(joinRequestService.decline(authentication.getName(), id));
    }

    @GetMapping("/api/teacher/sections/{sectionId}/members")
    public ResponseEntity<List<SectionMemberResponse>> listMembers(
            @PathVariable UUID sectionId, Authentication authentication) {
        return ResponseEntity.ok(joinRequestService.listMembers(authentication.getName(), sectionId));
    }

    @DeleteMapping("/api/teacher/sections/{sectionId}/members/{studentId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID sectionId, @PathVariable UUID studentId, Authentication authentication) {
        joinRequestService.removeMember(authentication.getName(), sectionId, studentId);
        return ResponseEntity.noContent().build();
    }
}
