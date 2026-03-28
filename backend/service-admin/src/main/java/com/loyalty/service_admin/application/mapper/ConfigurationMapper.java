package com.loyalty.service_admin.application.mapper;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationWriteData;
import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ConfigurationMapper {

    public DiscountConfigurationEntity toEntity(ConfigurationCreateRequest request) {
        DiscountConfigurationEntity entity = new DiscountConfigurationEntity();
        entity.setEcommerceId(request.ecommerceId());
        entity.setCurrency(request.currency().toUpperCase());
        entity.setRoundingRule(request.roundingRule());
        entity.setCapType(request.cap().type());
        entity.setCapValue(request.cap().value());
        entity.setCapAppliesTo(request.cap().appliesTo());
        entity.replacePriorities(request.priority().stream().map(item -> {
            DiscountPriorityEntity priority = new DiscountPriorityEntity();
            priority.setDiscountType(item.type());
            priority.setOrder(item.order());
            return priority;
        }).toList());
        return entity;
    }

    public void applyPatch(DiscountConfigurationEntity entity, ConfigurationPatchRequest request) {
        if (request.currency() != null) {
            entity.setCurrency(request.currency().toUpperCase());
        }
        if (request.roundingRule() != null) {
            entity.setRoundingRule(request.roundingRule());
        }
        if (request.cap() != null) {
            entity.setCapType(request.cap().type());
            entity.setCapValue(request.cap().value());
            entity.setCapAppliesTo(request.cap().appliesTo());
        }
        if (request.priority() != null) {
            entity.replacePriorities(request.priority().stream().map(item -> {
                DiscountPriorityEntity priority = new DiscountPriorityEntity();
                priority.setDiscountType(item.type());
                priority.setOrder(item.order());
                return priority;
            }).toList());
        }
    }

    public ConfigurationWriteData toWriteData(DiscountConfigurationEntity entity) {
        return new ConfigurationWriteData(entity.getId(), entity.getVersion());
    }

    public ConfigurationUpdatedEvent toUpdatedEvent(DiscountConfigurationEntity entity) {
        List<ConfigurationUpdatedEvent.PriorityItem> priority = entity.getPriorities().stream()
                .sorted(Comparator
                        .comparing(DiscountPriorityEntity::getOrder)
                        .thenComparing(item -> item.getId().toString()))
                .map(item -> new ConfigurationUpdatedEvent.PriorityItem(item.getId(), item.getDiscountType(), item.getOrder()))
                .toList();

        return new ConfigurationUpdatedEvent(
                "CONFIG_UPDATED",
                entity.getId(),
                entity.getEcommerceId(),
                entity.getVersion(),
                entity.getCurrency(),
                entity.getRoundingRule(),
                entity.getCapType(),
                entity.getCapValue(),
                entity.getCapAppliesTo(),
                priority,
                entity.getUpdatedAt()
        );
    }
}
