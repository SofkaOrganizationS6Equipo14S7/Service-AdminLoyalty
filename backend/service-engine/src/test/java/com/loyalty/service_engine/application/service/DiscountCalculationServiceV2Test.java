package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.ClassifyResponseV1;
import com.loyalty.service_engine.application.dto.calculate.DiscountCalculateRequestV2;
import com.loyalty.service_engine.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_engine.infrastructure.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscountCalculationServiceV2Test {

    private final EngineConfigurationCacheService cacheService = new EngineConfigurationCacheService();
    private final ClassificationEngine classificationEngine = mock(ClassificationEngine.class);
    private final DiscountCalculationServiceV2 service = new DiscountCalculationServiceV2(cacheService, classificationEngine);

    private static final ClassifyResponseV1 DEFAULT_CLASSIFICATION =
            new ClassifyResponseV1(null, "UNCLASSIFIED", 0, "No matching tier", Instant.now());

    @Test
    void shouldApplyCapByPriority() {
        when(classificationEngine.classify(any())).thenReturn(DEFAULT_CLASSIFICATION);

        UUID ecommerceId = UUID.randomUUID();
        cacheService.upsertFromEvent(new ConfigurationUpdatedEvent(
                "CONFIG_UPDATED",
                UUID.randomUUID(),
                ecommerceId,
                1L,
                "COP",
                ConfigurationUpdatedEvent.RoundingRule.HALF_UP,
                ConfigurationUpdatedEvent.CapType.PERCENTAGE,
                new BigDecimal("20"),
                ConfigurationUpdatedEvent.CapAppliesTo.SUBTOTAL,
                List.of(
                        new ConfigurationUpdatedEvent.PriorityItem(UUID.randomUUID(), "SEASONAL", 1),
                        new ConfigurationUpdatedEvent.PriorityItem(UUID.randomUUID(), "LOYALTY", 2)
                ),
                Instant.now()
        ));

        DiscountCalculateRequestV2 request = new DiscountCalculateRequestV2(
                ecommerceId,
                new BigDecimal("1000"),
                new BigDecimal("1190"),
                new BigDecimal("1000"),
                new BigDecimal("1190"),
                new BigDecimal("5000"),
                10,
                100,
                List.of(
                        new DiscountCalculateRequestV2.DiscountCandidate("LOYALTY", new BigDecimal("150")),
                        new DiscountCalculateRequestV2.DiscountCandidate("SEASONAL", new BigDecimal("100"))
                )
        );

        var result = service.calculate(request);

        assertThat(result.capAmount()).isEqualByComparingTo("200.00");
        assertThat(result.totalApplied()).isEqualByComparingTo("200.00");
        assertThat(result.capped()).isTrue();
        assertThat(result.appliedDiscounts().get(0).type()).isEqualTo("SEASONAL");
    }

    @Test
    void shouldUseDefaultConfigurationWhenNoConfigExists() {
        when(classificationEngine.classify(any())).thenReturn(DEFAULT_CLASSIFICATION);

        UUID ecommerceId = UUID.randomUUID();
        DiscountCalculateRequestV2 request = new DiscountCalculateRequestV2(
                ecommerceId,
                new BigDecimal("100"),
                new BigDecimal("119"),
                new BigDecimal("100"),
                new BigDecimal("119"),
                new BigDecimal("0"),
                0,
                0,
                List.of(new DiscountCalculateRequestV2.DiscountCandidate("LOYALTY", new BigDecimal("20")))
        );

        var result = service.calculate(request);
        assertThat(result.currency()).isEqualTo("COP");
        assertThat(result.roundingRule()).isEqualTo("HALF_UP");
        assertThat(result.totalApplied()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldIgnoreDiscountTypesNotPresentInPriority() {
        when(classificationEngine.classify(any())).thenReturn(DEFAULT_CLASSIFICATION);

        UUID ecommerceId = UUID.randomUUID();
        cacheService.upsertFromEvent(new ConfigurationUpdatedEvent(
                "CONFIG_UPDATED",
                UUID.randomUUID(),
                ecommerceId,
                1L,
                "COP",
                ConfigurationUpdatedEvent.RoundingRule.HALF_UP,
                ConfigurationUpdatedEvent.CapType.PERCENTAGE,
                new BigDecimal("50"),
                ConfigurationUpdatedEvent.CapAppliesTo.SUBTOTAL,
                List.of(new ConfigurationUpdatedEvent.PriorityItem(UUID.randomUUID(), "LOYALTY", 1)),
                Instant.now()
        ));

        DiscountCalculateRequestV2 request = new DiscountCalculateRequestV2(
                ecommerceId,
                new BigDecimal("100"),
                new BigDecimal("119"),
                new BigDecimal("100"),
                new BigDecimal("119"),
                new BigDecimal("0"),
                0,
                0,
                List.of(
                        new DiscountCalculateRequestV2.DiscountCandidate("LOYALTY", new BigDecimal("20")),
                        new DiscountCalculateRequestV2.DiscountCandidate("SEASONAL", new BigDecimal("40"))
                )
        );

        var result = service.calculate(request);
        assertThat(result.totalRequested()).isEqualByComparingTo("20.00");
        assertThat(result.totalApplied()).isEqualByComparingTo("20.00");
        assertThat(result.appliedDiscounts()).hasSize(1);
    }

    @Test
    void shouldFailForInvalidTotals() {
        UUID ecommerceId = UUID.randomUUID();
        DiscountCalculateRequestV2 request = new DiscountCalculateRequestV2(
                ecommerceId,
                new BigDecimal("200"),
                new BigDecimal("100"),
                new BigDecimal("100"),
                new BigDecimal("119"),
                new BigDecimal("0"),
                0,
                0,
                List.of(new DiscountCalculateRequestV2.DiscountCandidate("LOYALTY", new BigDecimal("20")))
        );

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("subtotal");
    }
}
