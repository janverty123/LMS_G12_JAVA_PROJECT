package com.apptitle.activity.controller;

import com.apptitle.activity.dto.ActivityResponse;
import com.apptitle.activity.dto.SaveActivityRequest;
import com.apptitle.activity.dto.ActivityFileDownloadResponse;
import com.apptitle.activity.dto.ActivityFileResponse;
import com.apptitle.activity.dto.ActivityFileUploadRequest;
import com.apptitle.activity.dto.ActivityFileUploadResponse;
import com.apptitle.activity.dto.ActivitySubmissionResponse;
import com.apptitle.activity.dto.ScoreActivitySubmissionRequest;
import com.apptitle.activity.service.ActivityService;
import com.apptitle.activity.service.ActivitySubmissionService;
import com.apptitle.material.dto.CompleteUploadRequest;
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

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherActivityController {

    private final ActivityService activityService;
    private final ActivitySubmissionService submissionService;

    public TeacherActivityController(ActivityService activityService,
            ActivitySubmissionService submissionService) {
        this.activityService = activityService;
        this.submissionService = submissionService;
    }

    @PostMapping("/class-sections/{classSectionId}/subjects/{subjectId}/activities")
    public ResponseEntity<ActivityResponse> create(
            @PathVariable UUID classSectionId,
            @PathVariable UUID subjectId,
            @Valid @RequestBody SaveActivityRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.create(
                authentication.getName(), classSectionId, subjectId, request));
    }

    @GetMapping("/class-sections/{classSectionId}/subjects/{subjectId}/activities")
    public ResponseEntity<List<ActivityResponse>> list(
            @PathVariable UUID classSectionId,
            @PathVariable UUID subjectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(activityService.listForTeacher(
                authentication.getName(), classSectionId, subjectId));
    }

    @PutMapping("/activities/{activityId}")
    public ResponseEntity<ActivityResponse> update(
            @PathVariable UUID activityId,
            @Valid @RequestBody SaveActivityRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(activityService.update(
                authentication.getName(), activityId, request));
    }

    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID activityId,
            Authentication authentication
    ) {
        activityService.delete(authentication.getName(), activityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activities/{activityId}/attachments/uploads/initialize")
    public ResponseEntity<ActivityFileUploadResponse> initializeAttachment(
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityFileUploadRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                submissionService.initializeTeacherAttachment(
                        authentication.getName(), activityId, request));
    }

    @PostMapping("/activity-files/{fileId}/attachments/uploads/complete")
    public ResponseEntity<ActivityFileResponse> completeAttachment(
            @PathVariable UUID fileId,
            @Valid @RequestBody CompleteUploadRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(submissionService.completeTeacherAttachment(
                authentication.getName(), fileId, request));
    }

    @GetMapping("/activities/{activityId}/attachments")
    public ResponseEntity<List<ActivityFileResponse>> attachments(
            @PathVariable UUID activityId, Authentication authentication) {
        return ResponseEntity.ok(submissionService.listAttachments(
                authentication.getName(), activityId));
    }

    @GetMapping("/activities/{activityId}/submissions")
    public ResponseEntity<List<ActivitySubmissionResponse>> submissions(
            @PathVariable UUID activityId, Authentication authentication) {
        return ResponseEntity.ok(submissionService.roster(
                authentication.getName(), activityId));
    }

    @PutMapping("/activity-submissions/{submissionId}/score")
    public ResponseEntity<ActivitySubmissionResponse> score(
            @PathVariable UUID submissionId,
            @Valid @RequestBody ScoreActivitySubmissionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(submissionService.score(
                authentication.getName(), submissionId, request));
    }

    @GetMapping("/activity-files/{fileId}/download")
    public ResponseEntity<ActivityFileDownloadResponse> download(
            @PathVariable UUID fileId, Authentication authentication) {
        return ResponseEntity.ok(submissionService.download(
                authentication.getName(), fileId));
    }
}
