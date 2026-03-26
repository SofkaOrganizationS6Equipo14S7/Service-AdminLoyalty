package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {
    Optional<ApiKeyEntity> findByHashedKey(String hashedKey);
    List<ApiKeyEntity> findByEcommerceId(UUID ecommerceId);
}
