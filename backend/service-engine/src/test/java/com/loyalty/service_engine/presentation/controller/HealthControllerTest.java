package com.loyalty.service_engine.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void healthShouldReturnUpStatusAndServiceName() {
        ResponseEntity<Map<String, Object>> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("service-engine", response.getBody().get("service"));
        assertTrue(((Long) response.getBody().get("timestamp")) > 0);
    }
}

