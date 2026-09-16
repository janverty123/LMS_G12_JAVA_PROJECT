package com.apptitle.user.controller;

import com.apptitle.auth.dto.AuthResponse;
import com.apptitle.user.dto.UpdateProfileRequest;
import com.apptitle.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public AuthResponse get(Principal principal) {
        return profiles.get(principal.getName());
    }

    @PutMapping
    public AuthResponse update(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.update(principal.getName(), request);
    }
}
