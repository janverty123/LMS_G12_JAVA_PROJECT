package com.apptitle.material.controller;

import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.material.dto.InitUploadRequest;
import com.apptitle.material.dto.InitUploadResponse;
import com.apptitle.material.dto.LearningMaterialResponse;
import com.apptitle.material.dto.MaterialDownloadResponse;
import com.apptitle.material.service.LearningMaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
public class TeacherLearningMaterialController {

    private final LearningMaterialService learningMaterialService;

    public TeacherLearningMaterialController(
            LearningMaterialService learningMaterialService
    ) {
        this.learningMaterialService = learningMaterialService;
    }

    @PostMapping(
            "/class-sections/{classSectionId}/subjects/{subjectId}/materials/uploads/initialize"
    )
    public ResponseEntity<InitUploadResponse> initUpload(
            @PathVariable UUID classSectionId,
            @PathVariable UUID subjectId,
            @Valid @RequestBody InitUploadRequest request,
            Authentication authentication
    ) {
        InitUploadResponse response = learningMaterialService.initUpload(
                authentication.getName(), classSectionId, subjectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/materials/{materialId}/uploads/complete")
    public ResponseEntity<LearningMaterialResponse> completeUpload(
            @PathVariable UUID materialId,
            @Valid @RequestBody CompleteUploadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningMaterialService.completeUpload(
                authentication.getName(), materialId, request));
    }

    @GetMapping("/class-sections/{classSectionId}/subjects/{subjectId}/materials")
    public ResponseEntity<List<LearningMaterialResponse>> listMaterials(
            @PathVariable UUID classSectionId,
            @PathVariable UUID subjectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningMaterialService.listForTeacher(
                authentication.getName(), classSectionId, subjectId));
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<MaterialDownloadResponse> getDownload(
            @PathVariable UUID materialId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningMaterialService.getTeacherDownload(
                authentication.getName(), materialId));
    }
}
