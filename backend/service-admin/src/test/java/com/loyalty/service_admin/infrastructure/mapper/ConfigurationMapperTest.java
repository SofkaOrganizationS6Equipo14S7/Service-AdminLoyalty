package com.loyalty.service_admin.infrastructure.mapper;

import com.loyalty.service_admin.application.dto.configuration.CapRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.DiscountPriorityRequest;
import com.loyalty.service_admin.application.mapper.ConfigurationMapper;
import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationMapperTest {

    private final ConfigurationMapper mapper = new ConfigurationMapper();

    @Test
    void shouldMapCreateRequestToEntity() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationCreateRequest request = new ConfigurationCreateRequest(
                ecommerceId,
                "cop",
                RoundingRule.HALF_UP,
                new CapRequest(CapType.PERCENTAGE, new BigDecimal("10"), CapAppliesTo.SUBTOTAL),
                List.of(
                        new DiscountPriorityRequest("LOYALTY", 2),
                        new DiscountPriorityRequest("SEASONAL", 1)
                )
        );

        DiscountConfigurationEntity entity = mapper.toEntity(request);

        assertThat(entity.getEcommerceId()).isEqualTo(ecommerceId);
        assertThat(entity.getCurrency()).isEqualTo("COP");
        assertThat(entity.getPriorities()).hasSize(2);
    }

    @Test
    void shouldApplyPatchAndMapUpdatedEvent() {
        DiscountConfigurationEntity entity = new DiscountConfigurationEntity();
        entity.setId(UUID.randomUUID());
        entity.setEcommerceId(UUID.randomUUID());
        entity.setCurrency("COP");
        entity.setRoundingRule(RoundingRule.HALF_UP);
        entity.setCapType(CapType.PERCENTAGE);
        entity.setCapValue(new BigDecimal("10"));
        entity.setCapAppliesTo(CapAppliesTo.SUBTOTAL);
        entity.setVersion(2L);
        entity.setUpdatedAt(Instant.now());

        DiscountPriorityEntity p1 = new DiscountPriorityEntity();
        p1.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        p1.setOrder(1);
        p1.setDiscountType("SEASONAL");
        p1.setConfiguration(entity);

        DiscountPriorityEntity p2 = new DiscountPriorityEntity();
        p2.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        p2.setOrder(1);
        p2.setDiscountType("LOYALTY");
        p2.setConfiguration(entity);
        entity.setPriorities(new ArrayList<>(List.of(p1, p2)));

        ConfigurationPatchRequest patch = new ConfigurationPatchRequest(
                "usd",
                RoundingRule.DOWN,
                new CapRequest(CapType.PERCENTAGE, new BigDecimal("20"), CapAppliesTo.TOTAL),
                List.of(new DiscountPriorityRequest("LOYALTY", 1))
        );

        mapper.applyPatch(entity, patch);
        assertThat(entity.getCurrency()).isEqualTo("USD");
        assertThat(entity.getRoundingRule()).isEqualTo(RoundingRule.DOWN);
        assertThat(entity.getPriorities()).hasSize(1);

        entity.getPriorities().get(0).setId(UUID.randomUUID());
        var event = mapper.toUpdatedEvent(entity);
        assertThat(event.eventType()).isEqualTo("CONFIG_UPDATED");
        assertThat(event.version()).isEqualTo(2L);
        assertThat(event.priority()).hasSize(1);
        assertThat(mapper.toWriteData(entity).version()).isEqualTo(2L);
    }
}
