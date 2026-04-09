package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountTypeRepository extends JpaRepository<DiscountTypeEntity, UUID> {
    Optional<DiscountTypeEntity> findByCode(String code);
}
