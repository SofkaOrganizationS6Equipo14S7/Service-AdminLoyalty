package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.configuration.CapRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.DiscountPriorityRequest;
import com.loyalty.service_admin.application.mapper.ConfigurationMapper;
import com.loyalty.service_admin.application.port.out.ConfigurationEventPort;
import com.loyalty.service_admin.application.port.out.ConfigurationPersistencePort;
import com.loyalty.service_admin.application.port.out.CurrentUserPort;
import com.loyalty.service_admin.application.validation.ConfigurationBusinessValidator;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationAlreadyExistsException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private ConfigurationPersistencePort repository;
    @Mock
    private ConfigurationEventPort eventPublisher;
    @Mock
    private CurrentUserPort currentUserPort;

    private ConfigurationMapper mapper;
    private ConfigurationBusinessValidator validator;

    private ConfigurationService service;

    @BeforeEach
    void setUp() {
        mapper = new ConfigurationMapper();
        validator = new ConfigurationBusinessValidator();
        service = new ConfigurationService(repository, mapper, eventPublisher, currentUserPort, validator);
    }

    @Test
    void shouldCreateConfiguration() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = validCreate(ecommerceId);

        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(repository.existsByEcommerceId(ecommerceId)).thenReturn(false);
        when(repository.save(any(DiscountSettingsEntity.class))).thenAnswer(invocation -> {
            DiscountSettingsEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setVersion(1L);
            entity.setUpdatedAt(Instant.now());
            entity.getPriorities().forEach(p -> p.setId(UUID.randomUUID()));
            return entity;
        });

        var result = service.createConfiguration(request);

        assertThat(result.version()).isEqualTo(1L);
        verify(eventPublisher).publishConfigUpdated(any());

        ArgumentCaptor<DiscountSettingsEntity> captor = ArgumentCaptor.forClass(DiscountSettingsEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCurrencyCode()).isEqualTo("COP");
    }

    @Test
    void shouldRejectDuplicatedOrders() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = new ConfigurationCreateRequest(
                ecommerceId,
                "COP",
                RoundingRule.HALF_UP,
                new CapRequest(CapType.PERCENTAGE, new BigDecimal("20"), CapAppliesTo.SUBTOTAL),
                List.of(
                        new DiscountPriorityRequest("SEASONAL", 1),
                        new DiscountPriorityRequest("LOYALTY", 1)
                )
        );

        assertThatThrownBy(() -> service.createConfiguration(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VALIDATION_ERROR");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowConflictWhenConfigurationAlreadyExists() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = validCreate(ecommerceId);

        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(repository.existsByEcommerceId(ecommerceId)).thenReturn(true);

        assertThatThrownBy(() -> service.createConfiguration(request))
                .isInstanceOf(ConfigurationAlreadyExistsException.class);
    }

    @Test
    void shouldThrowNotFoundOnPatchWhenConfigurationMissing() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationPatchRequest request = new ConfigurationPatchRequest("COP", RoundingRule.DOWN, null, null);

        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(repository.findByEcommerceId(ecommerceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patchConfiguration(ecommerceId, request))
                .isInstanceOf(ConfigurationNotFoundException.class);
    }

    @Test
    void shouldRejectCrossEcommerceAccessForScopedAdmin() {
        UUID currentEcommerce = UUID.randomUUID();
        UUID targetEcommerce = UUID.randomUUID();

        when(currentUserPort.isSuperAdmin()).thenReturn(false);
        when(currentUserPort.getCurrentUserEcommerceId()).thenReturn(currentEcommerce);

        assertThatThrownBy(() -> service.createConfiguration(validCreate(targetEcommerce)))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void shouldPatchConfigurationSuccessfully() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationPatchRequest request = new ConfigurationPatchRequest(
                "USD",
                RoundingRule.DOWN,
                new CapRequest(CapType.PERCENTAGE, new BigDecimal("15"), CapAppliesTo.TOTAL),
                List.of(new DiscountPriorityRequest("LOYALTY", 1))
        );

        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(repository.findByEcommerceId(ecommerceId)).thenReturn(Optional.of(existingConfig(ecommerceId)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.patchConfiguration(ecommerceId, request);
        assertThat(result.version()).isEqualTo(3L);
        verify(eventPublisher).publishConfigUpdated(any());
    }

    @Test
    void shouldRejectInvalidCurrency() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = new ConfigurationCreateRequest(
                ecommerceId,
                "XXX1",
                RoundingRule.HALF_UP,
                new CapRequest(CapType.PERCENTAGE, new BigDecimal("20"), CapAppliesTo.SUBTOTAL),
                List.of(new DiscountPriorityRequest("SEASONAL", 1))
        );

        assertThatThrownBy(() -> service.createConfiguration(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VALIDATION_ERROR");
    }

    @Test
    void shouldRejectInvalidCapOnCreate() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = new ConfigurationCreateRequest(
                ecommerceId,
                "COP",
                RoundingRule.HALF_UP,
                new CapRequest(CapType.PERCENTAGE, BigDecimal.ZERO, CapAppliesTo.SUBTOTAL),
                List.of(new DiscountPriorityRequest("SEASONAL", 1))
        );

        assertThatThrownBy(() -> service.createConfiguration(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VALIDATION_ERROR");
    }

    @Test
    void shouldRejectDuplicatedPriorityTypes() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationPatchRequest request = new ConfigurationPatchRequest(
                null,
                null,
                null,
                List.of(
                        new DiscountPriorityRequest("LOYALTY", 1),
                        new DiscountPriorityRequest("LOYALTY", 2)
                )
        );

        assertThatThrownBy(() -> service.patchConfiguration(ecommerceId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VALIDATION_ERROR");
    }

    private ConfigurationCreateRequest validCreate(UUID ecommerceId) {
        return new ConfigurationCreateRequest(
                ecommerceId,
                "COP",
                RoundingRule.HALF_UP,
                new CapRequest(CapType.PERCENTAGE, new BigDecimal("20"), CapAppliesTo.SUBTOTAL),
                List.of(
                        new DiscountPriorityRequest("SEASONAL", 1),
                        new DiscountPriorityRequest("LOYALTY", 2)
                )
        );
    }

    private DiscountSettingsEntity existingConfig(UUID ecommerceId) {
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setId(UUID.randomUUID());
        entity.setEcommerceId(ecommerceId);
        entity.setCurrencyCode("COP");
        entity.setRoundingRule("HALF_UP");
        entity.setCapType(CapType.PERCENTAGE);
        entity.setCapValue(new BigDecimal("10"));
        entity.setCapAppliesTo(CapAppliesTo.SUBTOTAL);
        entity.setVersion(3L);
        entity.setUpdatedAt(Instant.now());

        DiscountPriorityEntity priority = new DiscountPriorityEntity();
        priority.setId(UUID.randomUUID());
        priority.setDiscountTypeId(UUID.randomUUID());
        priority.setPriorityLevel(1);
        priority.setDiscountSettingId(entity.getId());
        entity.setPriorities(new ArrayList<>(List.of(priority)));
        return entity;
    }
}
