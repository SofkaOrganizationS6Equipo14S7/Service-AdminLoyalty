package com.loyalty.service_admin.application.mapper;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationWriteData;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ConfigurationMapper {

    public DiscountSettingsEntity toEntity(ConfigurationCreateRequest request) {
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setEcommerceId(request.ecommerceId());
        entity.setIsActive(true);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    public void applyPatch(DiscountSettingsEntity entity, ConfigurationPatchRequest request) {
        entity.setUpdatedAt(Instant.now());
    }

    public ConfigurationWriteData toWriteData(DiscountSettingsEntity entity) {
        return new ConfigurationWriteData(entity.getId(), entity.getVersion());
    }

    public ConfigurationUpdatedEvent toUpdatedEvent(DiscountSettingsEntity entity) {
        return new ConfigurationUpdatedEvent(
                "CONFIG_UPDATED",
                entity.getId(),
                entity.getEcommerceId(),
                entity.getVersion(),
                entity.getCurrencyCode(),
                null,
                entity.getCapType(),
                entity.getCapValue(),
                entity.getCapAppliesTo(),
                new ArrayList<>(),
                entity.getUpdatedAt()
        );
    }
}