package com.apptitle.material.controller;

import com.apptitle.material.dto.LearningMaterialResponse;
import com.apptitle.material.dto.MaterialDownloadResponse;
import com.apptitle.material.service.LearningMaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentLearningMaterialController {

    private final LearningMaterialService learningMaterialService;

    public StudentLearningMaterialController(
            LearningMaterialService learningMaterialService
    ) {
        this.learningMaterialService = learningMaterialService;
    }

    @GetMapping("/subjects/{subjectId}/materials")
    public ResponseEntity<List<LearningMaterialResponse>> listMaterials(
            @PathVariable UUID subjectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningMaterialService.listForStudent(
                authentication.getName(), subjectId));
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<MaterialDownloadResponse> getDownload(
            @PathVariable UUID materialId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningMaterialService.getStudentDownload(
                authentication.getName(), materialId));
    }
}
