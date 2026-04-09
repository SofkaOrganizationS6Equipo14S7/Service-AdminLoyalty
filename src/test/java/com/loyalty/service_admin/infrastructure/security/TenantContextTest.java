package com.loyalty.service_admin.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext Unit Tests")
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetCurrentTenant() {
        // Arrange
        UUID tenantId = UUID.randomUUID();

        // Act
        TenantContext.setCurrentTenant(tenantId);

        // Assert
        assertEquals(tenantId, TenantContext.getCurrentTenant());
    }

    @Test
    void testGetCurrentTenant_notSet_returnsNull() {
        // Act & Assert
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testClear_removesTenant() {
        // Arrange
        TenantContext.setCurrentTenant(UUID.randomUUID());

        // Act
        TenantContext.clear();

        // Assert
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testSetCurrentTenant_nullValue_doesNotSet() {
        // Act
        TenantContext.setCurrentTenant(null);

        // Assert
        assertNull(TenantContext.getCurrentTenant());
    }
}
