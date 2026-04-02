package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountApplicationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountApplicationLogRepository extends JpaRepository<DiscountApplicationLogEntity, UUID> {
    List<DiscountApplicationLogEntity> findByEcommerceId(UUID ecommerceId);
    List<DiscountApplicationLogEntity> findByExternalOrderId(String externalOrderId);
}
