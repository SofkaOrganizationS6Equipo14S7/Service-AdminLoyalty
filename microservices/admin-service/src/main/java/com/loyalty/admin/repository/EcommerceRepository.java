package com.loyalty.admin.repository;

import com.loyalty.admin.entity.Ecommerce;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EcommerceRepository extends JpaRepository<Ecommerce, Long> {
    Optional<Ecommerce> findByApiKey(String apiKey);
}
