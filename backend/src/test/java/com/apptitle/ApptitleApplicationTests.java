package com.apptitle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Requires a running Postgres instance (see docker-compose.yml) since the
 * datasource is not mocked at this phase. Once Testcontainers is introduced
 * for CI (Phase 10), this can run without a manually started database.
 */
@SpringBootTest
class ApptitleApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context fails to start (bad config, missing beans,
        // broken security chain, etc.) this test fails — which is the point.
    }
}
