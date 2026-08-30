package com.apptitle.activity.controller;

import com.apptitle.activity.dto.ActivityResponse;
import com.apptitle.activity.dto.ActivityFileDownloadResponse;
import com.apptitle.activity.dto.ActivityFileResponse;
import com.apptitle.activity.dto.ActivityFileUploadRequest;
import com.apptitle.activity.dto.ActivityFileUploadResponse;
import com.apptitle.activity.dto.ActivitySubmissionResponse;
import com.apptitle.activity.service.ActivityService;
import com.apptitle.activity.service.ActivitySubmissionService;
import com.apptitle.material.dto.CompleteUploadRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentActivityController {

    private final ActivityService activityService;
    private final ActivitySubmissionService submissionService;

    public StudentActivityController(ActivityService activityService,
            ActivitySubmissionService submissionService) {
        this.activityService = activityService;
        this.submissionService = submissionService;
    }

    @GetMapping("/subjects/{subjectId}/activities")
    public ResponseEntity<List<ActivityResponse>> list(
            @PathVariable UUID subjectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(activityService.listForStudent(
                authentication.getName(), subjectId));
    }

    @PostMapping("/activities/{activityId}/submission/uploads/initialize")
    public ResponseEntity<ActivityFileUploadResponse> initializeSubmission(
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityFileUploadRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(submissionService.initializeStudentSubmission(
                authentication.getName(), activityId, request));
    }

    @PostMapping("/activity-files/{fileId}/submission/uploads/complete")
    public ResponseEntity<ActivitySubmissionResponse> completeSubmission(
            @PathVariable UUID fileId,
            @Valid @RequestBody CompleteUploadRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(submissionService.completeStudentSubmission(
                authentication.getName(), fileId, request));
    }

    @GetMapping("/activities/{activityId}/submission")
    public ResponseEntity<ActivitySubmissionResponse> ownSubmission(
            @PathVariable UUID activityId, Authentication authentication) {
        return ResponseEntity.ok(submissionService.getOwnSubmission(
                authentication.getName(), activityId));
    }

    @GetMapping("/activities/{activityId}/attachments")
    public ResponseEntity<List<ActivityFileResponse>> attachments(
            @PathVariable UUID activityId, Authentication authentication) {
        return ResponseEntity.ok(submissionService.listAttachments(
                authentication.getName(), activityId));
    }

    @GetMapping("/activity-files/{fileId}/download")
    public ResponseEntity<ActivityFileDownloadResponse> download(
            @PathVariable UUID fileId, Authentication authentication) {
        return ResponseEntity.ok(submissionService.download(
                authentication.getName(), fileId));
    }
}
