package com.apptitle.classsection.controller;

import com.apptitle.classsection.dto.ClassSectionResponse;
import com.apptitle.classsection.dto.CreateClassSectionRequest;
import com.apptitle.classsection.dto.UpdateClassSectionRequest;
import com.apptitle.classsection.service.ClassSectionService;
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

/**
 * Teacher-only endpoints for managing ClassSections.
 */
@RestController
@RequestMapping("/api/teacher/class-sections")
@PreAuthorize("hasRole('TEACHER')")
public class ClassSectionController {

    private final ClassSectionService classSectionService;

    public ClassSectionController(ClassSectionService classSectionService) {
        this.classSectionService = classSectionService;
    }

    @PostMapping
    public ResponseEntity<ClassSectionResponse> createClassSection(
            @Valid @RequestBody CreateClassSectionRequest request,
            Authentication authentication
    ) {
        ClassSectionResponse response = classSectionService.createClassSection(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClassSectionResponse>> listOwnClassSections(Authentication authentication) {
        return ResponseEntity.ok(classSectionService.listOwnClassSections(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassSectionResponse> updateClassSection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClassSectionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classSectionService.updateClassSection(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassSection(@PathVariable UUID id, Authentication authentication) {
        classSectionService.deleteClassSection(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}