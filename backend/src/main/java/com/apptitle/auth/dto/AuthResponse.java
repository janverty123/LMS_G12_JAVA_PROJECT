package com.apptitle.auth.dto;

import com.apptitle.user.entity.Role;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String name,
        String email,
        Role role,
        String lrn,
        String profilePicture
) {
    public AuthResponse(String token, UUID userId, String name, String email, Role role) {
        this(token, userId, name, email, role, null, null);
    }
}
