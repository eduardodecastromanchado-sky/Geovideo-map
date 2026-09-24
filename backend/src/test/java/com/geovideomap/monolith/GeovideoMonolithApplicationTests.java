package com.geovideomap.monolith;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Smoke test intentionally disabled in CI.
 *
 * @SpringBootTest would open Hikari against a real MySQL instance, which is not
 * available in the CI environment. All behavioural coverage is provided by the
 * @WebMvcTest slice tests in AuthControllerTest.
 *
 * To run a full application context smoke test locally:
 *   SPRING_DATASOURCE_URL=... SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=... \
 *   ./mvnw test -Dtest=GeovideoMonolithApplicationTests
 */
@Disabled("Requires a live MySQL instance — run manually, not in CI")
class GeovideoMonolithApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: verifies the Spring context starts without errors.
    }
}
