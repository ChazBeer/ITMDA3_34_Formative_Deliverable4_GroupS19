package org.example.cafeteria_ordering_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
                "jwt.secret=test-only-secret-key-for-cafeteria-integration-tests-2026",
                "jwt.expiration-ms=900000"
        }
)
@ActiveProfiles("test")
class CafeteriaOrderingBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
