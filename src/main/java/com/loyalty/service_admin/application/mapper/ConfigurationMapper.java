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
        entity.setCurrencyCode(request.currency());
        entity.setRoundingRule(request.roundingRule().name());
        entity.setIsActive(true);
        entity.setVersion(1L);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        // Crear prioridades
        List<DiscountPriorityEntity> priorities = new ArrayList<>();
        if (request.priority() != null) {
            for (var priorityReq : request.priority()) {
                DiscountPriorityEntity priority = new DiscountPriorityEntity();
                priority.setDiscountTypeId(UUID.nameUUIDFromBytes(priorityReq.type().getBytes()));
                priority.setPriorityLevel(priorityReq.order());
                priority.setIsActive(true);
                priority.setCreatedAt(Instant.now());
                priority.setUpdatedAt(Instant.now());
                priorities.add(priority);
            }
        }
        entity.setPriorities(priorities);
        
        return entity;
    }

    public void applyPatch(DiscountSettingsEntity entity, ConfigurationPatchRequest request) {
        if (request.currency() != null) {
            entity.setCurrencyCode(request.currency());
        }
        if (request.roundingRule() != null) {
            entity.setRoundingRule(request.roundingRule().name());
        }
        if (request.priority() != null && !request.priority().isEmpty()) {
            List<DiscountPriorityEntity> priorities = new ArrayList<>();
            for (var priorityReq : request.priority()) {
                DiscountPriorityEntity priority = new DiscountPriorityEntity();
                priority.setDiscountTypeId(UUID.nameUUIDFromBytes(priorityReq.type().getBytes()));
                priority.setPriorityLevel(priorityReq.order());
                priority.setIsActive(true);
                priority.setCreatedAt(Instant.now());
                priority.setUpdatedAt(Instant.now());
                priorities.add(priority);
            }
            entity.replacePriorities(priorities);
        }
        entity.setVersion(entity.getVersion() + 1);
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
                null,
                null,
                null,
                new ArrayList<>(),
                entity.getUpdatedAt()
        );
    }
}