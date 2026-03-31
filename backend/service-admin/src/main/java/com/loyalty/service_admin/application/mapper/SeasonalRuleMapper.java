package com.loyalty.service_admin.application.mapper;

import com.loyalty.service_admin.application.dto.SeasonalRuleResponse;
import com.loyalty.service_admin.domain.entity.SeasonalRuleEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for SeasonalRuleEntity ↔ SeasonalRuleResponse
 */
@Component
public class SeasonalRuleMapper {

    /**
     * Map entity to response DTO
     */
    public SeasonalRuleResponse toResponse(SeasonalRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SeasonalRuleResponse(
            entity.getUid().toString(),
            entity.getEcommerceId().toString(),
            entity.getName(),
            entity.getDescription(),
            entity.getDiscountPercentage(),
            entity.getDiscountType(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Map entity to response DTO (for list operations)
     */
    public SeasonalRuleResponse toResponseForList(SeasonalRuleEntity entity) {
        return toResponse(entity);
    }
}
