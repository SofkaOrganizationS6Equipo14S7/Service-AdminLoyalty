package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerTierRepository extends JpaRepository<CustomerTierEntity, UUID> {

    List<CustomerTierEntity> findByEcommerceIdAndIsActiveTrueOrderByHierarchyLevelAsc(UUID ecommerceId);

    List<CustomerTierEntity> findByIsActiveTrueOrderByHierarchyLevelAsc();

    Optional<CustomerTierEntity> findByEcommerceIdAndNameAndIsActiveTrue(UUID ecommerceId, String name);

    List<CustomerTierEntity> findAllByOrderByHierarchyLevelAsc();

    List<CustomerTierEntity> findByEcommerceId(UUID ecommerceId);

    boolean existsByEcommerceIdAndName(UUID ecommerceId, String name);
}