package com.apptitle.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public, unauthenticated endpoint. Used by the frontend's landing page in
 * Phase 1 to confirm the two applications and the database connection are
 * actually wired together, before any real feature exists to test with.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "apptitle-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
