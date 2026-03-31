package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.ProductRuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRuleRepository extends JpaRepository<ProductRuleEntity, UUID> {
    
    Optional<ProductRuleEntity> findByEcommerceIdAndProductTypeAndIsActive(
            UUID ecommerceId, String productType, Boolean isActive);
    
    Optional<ProductRuleEntity> findByUidAndEcommerceId(UUID uid, UUID ecommerceId);
    
    Page<ProductRuleEntity> findByEcommerceIdAndIsActive(UUID ecommerceId, Boolean isActive, Pageable pageable);
    
    Page<ProductRuleEntity> findByEcommerceId(UUID ecommerceId, Pageable pageable);
}
