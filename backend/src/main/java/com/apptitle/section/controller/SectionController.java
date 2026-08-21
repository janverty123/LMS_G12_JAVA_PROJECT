package com.apptitle.section.controller;

import com.apptitle.section.dto.CreateSectionRequest;
import com.apptitle.section.dto.SectionResponse;
import com.apptitle.section.service.SectionService;
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
 * Teacher-only. Kept minimal on purpose (create + list) — full section
 * management (edit/delete/join-request review/member management) is Phase 3.
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
}
