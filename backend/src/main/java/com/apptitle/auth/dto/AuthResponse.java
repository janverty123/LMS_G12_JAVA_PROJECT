package com.apptitle.auth.dto;

import com.apptitle.user.entity.Role;

import java.util.UUID;

/**
 * sectionName/joinRequestStatus are null for teacher registration and for
 * login — they're only populated on student registration, to tell the
 * frontend "your account was created AND a join request was sent to X,
 * pending approval" in one response instead of a second round-trip.
 */
public record AuthResponse(
        String token,
        UUID userId,
        String name,
        String email,
        Role role,
        String sectionName,
        String joinRequestStatus
) {
    public static AuthResponse withoutSection(String token, UUID userId, String name, String email, Role role) {
        return new AuthResponse(token, userId, name, email, role, null, null);
    }
}
