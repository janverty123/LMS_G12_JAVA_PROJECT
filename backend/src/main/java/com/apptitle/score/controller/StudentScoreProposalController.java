package com.apptitle.score.controller;

import com.apptitle.activity.dto.ActivityFileUploadResponse;
import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.score.dto.InitializeScoreProofRequest;
import com.apptitle.score.dto.ScoreProposalResponse;
import com.apptitle.score.service.ScoreProposalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentScoreProposalController {
    private final ScoreProposalService service;

    public StudentScoreProposalController(ScoreProposalService service) {
        this.service = service;
    }

    @PostMapping("/activities/{activityId}/score-proposal/uploads/initialize")
    public ResponseEntity<ActivityFileUploadResponse> initialize(
            @PathVariable UUID activityId,
            @Valid @RequestBody InitializeScoreProofRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.initialize(authentication.getName(), activityId, request));
    }

    @PostMapping("/score-proposal-files/{fileId}/uploads/complete")
    public ResponseEntity<ScoreProposalResponse> complete(@PathVariable UUID fileId,
            @Valid @RequestBody CompleteUploadRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.complete(authentication.getName(), fileId, request));
    }

    @GetMapping("/activities/{activityId}/score-proposal")
    public ResponseEntity<ScoreProposalResponse> own(@PathVariable UUID activityId,
            Authentication authentication) {
        ScoreProposalResponse response = service.own(authentication.getName(), activityId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }
}
