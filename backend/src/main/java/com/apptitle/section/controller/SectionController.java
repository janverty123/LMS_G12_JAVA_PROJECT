package com.apptitle.section.controller;

import com.apptitle.section.dto.CreateSectionRequest;
import com.apptitle.section.dto.SectionResponse;
import com.apptitle.section.dto.UpdateSectionRequest;
import com.apptitle.section.service.SectionService;
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
 * Teacher-only. Full CRUD for sections. Pending-join-request review and
 * member management live in TeacherJoinRequestController instead.
 */
@RestController
@RequestMapping("/api/teacher/sections")
@PreAuthorize("hasRole('TEACHER')")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping
    public ResponseEntity<SectionResponse> createSection(
            @Valid @RequestBody CreateSectionRequest request,
            Authentication authentication
    ) {
        SectionResponse response = sectionService.createSection(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SectionResponse>> listOwnSections(Authentication authentication) {
        return ResponseEntity.ok(sectionService.listOwnSections(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSectionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(sectionService.updateSection(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id, Authentication authentication) {
        sectionService.deleteSection(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
