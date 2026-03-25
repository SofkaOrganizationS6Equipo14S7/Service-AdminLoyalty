package com.loyalty.admin.repository;

import com.loyalty.admin.entity.ConfiguracionDescuento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionDescuentoRepository extends JpaRepository<ConfiguracionDescuento, Long> {
    Optional<ConfiguracionDescuento> findByEcommerceId(Long ecommerceId);
}
