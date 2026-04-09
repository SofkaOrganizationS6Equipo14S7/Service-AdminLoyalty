package com.loyalty.service_admin.application.mapper;

import com.loyalty.service_admin.application.dto.configuration.*;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.model.RoundingRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConfigurationMapper Unit Tests")
class ConfigurationMapperTest {

    private ConfigurationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ConfigurationMapper();
    }

    @Test
    @DisplayName("testToEntity_MapsAllFieldsCorrectly")
    void testToEntity_MapsAllFieldsCorrectly() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.ecommerceId()).thenReturn(ecommerceId);
        when(request.currency()).thenReturn("USD");
        when(request.roundingRule()).thenReturn(RoundingRule.HALF_UP);
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("PRODUCT", 1),
                new DiscountPriorityRequest("SEASONAL", 2)
        ));

        // Act
        DiscountSettingsEntity entity = mapper.toEntity(request);

        // Assert
        assertEquals(ecommerceId, entity.getEcommerceId());
        assertEquals("USD", entity.getCurrencyCode());
        assertEquals("HALF_UP", entity.getRoundingRule());
        assertTrue(entity.getIsActive());
        assertEquals(1L, entity.getVersion());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(2, entity.getPriorities().size());
    }

    @Test
    @DisplayName("testToEntity_NullPriorities_CreatesEmptyList")
    void testToEntity_NullPriorities_CreatesEmptyList() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.ecommerceId()).thenReturn(UUID.randomUUID());
        when(request.currency()).thenReturn("EUR");
        when(request.roundingRule()).thenReturn(RoundingRule.HALF_UP);
        when(request.priority()).thenReturn(null);

        // Act
        DiscountSettingsEntity entity = mapper.toEntity(request);

        // Assert
        assertNotNull(entity.getPriorities());
        assertTrue(entity.getPriorities().isEmpty());
    }

    @Test
    @DisplayName("testApplyPatch_UpdatesCurrency")
    void testApplyPatch_UpdatesCurrency() {
        // Arrange
        DiscountSettingsEntity entity = createEntityWithVersion(1L);
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn("EUR");
        when(request.roundingRule()).thenReturn(null);
        when(request.priority()).thenReturn(null);

        // Act
        mapper.applyPatch(entity, request);

        // Assert
        assertEquals("EUR", entity.getCurrencyCode());
        assertEquals(2L, entity.getVersion());
    }

    @Test
    @DisplayName("testApplyPatch_UpdatesRoundingRule")
    void testApplyPatch_UpdatesRoundingRule() {
        // Arrange
        DiscountSettingsEntity entity = createEntityWithVersion(1L);
        entity.setRoundingRule("HALF_UP");
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn(null);
        when(request.roundingRule()).thenReturn(RoundingRule.DOWN);
        when(request.priority()).thenReturn(null);

        // Act
        mapper.applyPatch(entity, request);

        // Assert
        assertEquals("DOWN", entity.getRoundingRule());
    }

    @Test
    @DisplayName("testApplyPatch_ReplacesPriorities")
    void testApplyPatch_ReplacesPriorities() {
        // Arrange
        DiscountSettingsEntity entity = createEntityWithVersion(1L);
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn(null);
        when(request.roundingRule()).thenReturn(null);
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("CLASSIFICATION", 1)
        ));

        // Act
        mapper.applyPatch(entity, request);

        // Assert
        assertEquals(2L, entity.getVersion());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    @DisplayName("testApplyPatch_NullFields_NoChanges")
    void testApplyPatch_NullFields_NoChanges() {
        // Arrange
        DiscountSettingsEntity entity = createEntityWithVersion(3L);
        entity.setCurrencyCode("USD");
        entity.setRoundingRule("HALF_UP");
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn(null);
        when(request.roundingRule()).thenReturn(null);
        when(request.priority()).thenReturn(null);

        // Act
        mapper.applyPatch(entity, request);

        // Assert
        assertEquals("USD", entity.getCurrencyCode());
        assertEquals("HALF_UP", entity.getRoundingRule());
        assertEquals(4L, entity.getVersion());
    }

    @Test
    @DisplayName("testToWriteData_MapsIdAndVersion")
    void testToWriteData_MapsIdAndVersion() {
        // Arrange
        UUID id = UUID.randomUUID();
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setId(id);
        entity.setVersion(5L);

        // Act
        ConfigurationWriteData result = mapper.toWriteData(entity);

        // Assert
        assertEquals(id, result.configId());
        assertEquals(5L, result.version());
    }

    @Test
    @DisplayName("testToUpdatedEvent_MapsCorrectly")
    void testToUpdatedEvent_MapsCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();
        Instant now = Instant.now();
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setId(id);
        entity.setEcommerceId(ecommerceId);
        entity.setVersion(2L);
        entity.setCurrencyCode("USD");
        entity.setUpdatedAt(now);

        // Act
        ConfigurationUpdatedEvent event = mapper.toUpdatedEvent(entity);

        // Assert
        assertEquals("CONFIG_UPDATED", event.eventType());
        assertEquals(id, event.configId());
        assertEquals(ecommerceId, event.ecommerceId());
        assertEquals(2L, event.version());
        assertEquals("USD", event.currency());
        assertEquals(now, event.updatedAt());
    }

    private DiscountSettingsEntity createEntityWithVersion(long version) {
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setId(UUID.randomUUID());
        entity.setEcommerceId(UUID.randomUUID());
        entity.setCurrencyCode("USD");
        entity.setVersion(version);
        entity.setPriorities(new ArrayList<>());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
