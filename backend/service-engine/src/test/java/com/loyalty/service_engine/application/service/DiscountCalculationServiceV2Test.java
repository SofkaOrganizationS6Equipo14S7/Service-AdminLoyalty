package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.AppliedRuleDetail;
import com.loyalty.service_engine.application.dto.CartItemRequest;
import com.loyalty.service_engine.application.dto.ClassificationResult;
import com.loyalty.service_engine.application.dto.DiscountCalculateRequestV2;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import com.loyalty.service_engine.infrastructure.exception.InvalidCartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountCalculationServiceV2Test {

        @Mock
        private FidelityClassificationService fidelityClassificationService;

        @Mock
        private DiscountPriorityEvaluator discountPriorityEvaluator;

        @Mock
        private DiscountCappingEngine discountCappingEngine;

        @Mock
        private TransactionLogWriter transactionLogWriter;

        @Mock
        private DiscountConfigRepository discountConfigRepository;

        @InjectMocks
        private DiscountCalculationServiceV2 service;

        private UUID ecommerceId;

        @BeforeEach
        void setUp() {
                ecommerceId = UUID.randomUUID();
        }

    @Test
        void testCalculate_appliesExclusiveRuleOverOthers() {
                DiscountCalculateRequestV2 request = validRequest();

                AppliedRuleDetail individual = new AppliedRuleDetail(
                        UUID.randomUUID(),
                        "Fidelity 5%",
                        "FIDELITY",
                        "PERCENTAGE",
                        "INDIVIDUAL",
                        new BigDecimal("5"),
                        new BigDecimal("50.00"),
                        1
                );
                AppliedRuleDetail exclusive = new AppliedRuleDetail(
                        UUID.randomUUID(),
                        "Black Friday 10%",
                        "SEASONAL",
                        "PERCENTAGE",
                        "EXCLUSIVE",
                        new BigDecimal("10"),
                        new BigDecimal("100.00"),
                        2
                );

                when(fidelityClassificationService.classify(any(), any()))
                        .thenReturn(ClassificationResult.of(UUID.randomUUID(), "Gold", 3, new BigDecimal("15"), List.of("min_spent")));
                when(discountPriorityEvaluator.evaluateByPriority(any(), any(), anyString(), any()))
                        .thenReturn(List.of(individual, exclusive));
                when(discountConfigRepository.findByEcommerceIdAndIsActiveTrue(ecommerceId)).thenReturn(Optional.empty());
                when(discountCappingEngine.applyCap(any(), any())).thenReturn(
                        new DiscountCappingEngine.CapResult(new BigDecimal("100.00"), false, null)
                );
                when(discountCappingEngine.applyRounding(any(), any())).thenAnswer(invocation ->
                        ((BigDecimal) invocation.getArgument(0)).setScale(2, java.math.RoundingMode.HALF_UP)
                );
                when(transactionLogWriter.writeLog(any(), any())).thenReturn(UUID.randomUUID());

                var result = service.calculate(request);

                assertThat(result.discountCalculated()).isEqualByComparingTo("100.00");
                assertThat(result.discountApplied()).isEqualByComparingTo("100.00");
                assertThat(result.appliedRules()).hasSize(1);
                assertThat(result.appliedRules().get(0).ruleName()).isEqualTo("Black Friday 10%");
    }

    @Test
        void testCalculate_appliesCapWhenConfigured() {
                DiscountCalculateRequestV2 request = validRequest();
                DiscountConfigEntity config = new DiscountConfigEntity();
                config.setEcommerceId(ecommerceId);
                config.setMaxDiscountLimit(new BigDecimal("80.00"));
                config.setIsActive(true);

                when(fidelityClassificationService.classify(any(), any()))
                        .thenReturn(ClassificationResult.of(UUID.randomUUID(), "Silver", 2, new BigDecimal("10"), List.of("min_order_count")));
                when(discountPriorityEvaluator.evaluateByPriority(any(), any(), anyString(), any()))
                        .thenReturn(List.of(new AppliedRuleDetail(
                                UUID.randomUUID(), "Rule", "FIDELITY", "PERCENTAGE", "CUMULATIVE",
                                new BigDecimal("20"), new BigDecimal("100.00"), 1
                        )));
                when(discountConfigRepository.findByEcommerceIdAndIsActiveTrue(ecommerceId)).thenReturn(Optional.of(config));
                when(discountCappingEngine.applyCap(new BigDecimal("100.00"), new BigDecimal("80.00")))
                        .thenReturn(new DiscountCappingEngine.CapResult(new BigDecimal("80.00"), true, "max_discount_cap"));
                when(discountCappingEngine.applyRounding(any(), any())).thenAnswer(invocation ->
                        ((BigDecimal) invocation.getArgument(0)).setScale(2, java.math.RoundingMode.HALF_UP)
                );
                when(transactionLogWriter.writeLog(any(), any())).thenReturn(UUID.randomUUID());

                var result = service.calculate(request);

                assertThat(result.wasCapped()).isTrue();
                assertThat(result.capReason()).isEqualTo("max_discount_cap");
                assertThat(result.discountApplied()).isEqualByComparingTo("80.00");
    }

    @Test
        void testCalculate_whenNoRules_appliesZeroDiscount() {
                DiscountCalculateRequestV2 request = validRequest();

                when(fidelityClassificationService.classify(any(), any())).thenReturn(ClassificationResult.NONE);
                when(discountPriorityEvaluator.evaluateByPriority(any(), any(), anyString(), any())).thenReturn(List.of());
                when(discountConfigRepository.findByEcommerceIdAndIsActiveTrue(ecommerceId)).thenReturn(Optional.empty());
                when(discountCappingEngine.applyCap(BigDecimal.ZERO, null))
                        .thenReturn(new DiscountCappingEngine.CapResult(BigDecimal.ZERO, false, null));
                when(discountCappingEngine.applyRounding(any(), any())).thenAnswer(invocation ->
                        ((BigDecimal) invocation.getArgument(0)).setScale(2, java.math.RoundingMode.HALF_UP)
                );
                when(transactionLogWriter.writeLog(any(), any())).thenReturn(UUID.randomUUID());

                var result = service.calculate(request);
                assertThat(result.discountApplied()).isEqualByComparingTo("0.00");
                assertThat(result.finalAmount()).isEqualByComparingTo("250.00");
                assertThat(result.appliedRules()).isEmpty();
    }

    @Test
        void testCalculate_invalidCart_throwsInvalidCartException() {
        DiscountCalculateRequestV2 request = new DiscountCalculateRequestV2(
                        ecommerceId,
                        "order-1",
                        "customer-1",
                        BigDecimal.ZERO,
                        0,
                        0,
                        List.of()
        );

        assertThatThrownBy(() -> service.calculate(request))
                        .isInstanceOf(InvalidCartException.class)
                        .hasMessageContaining("Carrito sin items");

                verify(discountPriorityEvaluator, never()).evaluateByPriority(any(), any(), anyString(), any());
                verify(transactionLogWriter, never()).writeLog(any(), any());
        }

        private DiscountCalculateRequestV2 validRequest() {
                return new DiscountCalculateRequestV2(
                        ecommerceId,
                        "order-123",
                        "customer-789",
                        new BigDecimal("2500.00"),
                        15,
                        180,
                        List.of(
                                new CartItemRequest("prod-1", 2, new BigDecimal("100.00"), "electronics"),
                                new CartItemRequest("prod-2", 1, new BigDecimal("50.00"), "accessories")
                        )
                );
    }
}
