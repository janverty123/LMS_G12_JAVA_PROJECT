package com.apptitle.score.controller;

import com.apptitle.score.dto.ReviewScoreProposalRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherScoreProposalController {
    private final ScoreProposalService service;

    public TeacherScoreProposalController(ScoreProposalService service) {
        this.service = service;
    }

    @GetMapping("/activities/{activityId}/score-proposals")
    public ResponseEntity<List<ScoreProposalResponse>> list(@PathVariable UUID activityId,
            Authentication authentication) {
        return ResponseEntity.ok(service.listForTeacher(authentication.getName(), activityId));
    }

    @PostMapping("/score-proposals/{proposalId}/approve")
    public ResponseEntity<ScoreProposalResponse> approve(@PathVariable UUID proposalId,
            @Valid @RequestBody ReviewScoreProposalRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.approve(authentication.getName(), proposalId, request));
    }

    @PostMapping("/score-proposals/{proposalId}/reject")
    public ResponseEntity<ScoreProposalResponse> reject(@PathVariable UUID proposalId,
            Authentication authentication) {
        return ResponseEntity.ok(service.reject(authentication.getName(), proposalId));
    }
}
