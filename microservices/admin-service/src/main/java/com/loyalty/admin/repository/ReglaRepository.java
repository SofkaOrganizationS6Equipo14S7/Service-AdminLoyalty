package com.loyalty.admin.repository;

import com.loyalty.admin.entity.Regla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaRepository extends JpaRepository<Regla, Long> {
    List<Regla> findByEcommerceId(Long ecommerceId);
    List<Regla> findByEcommerceIdAndActivaTrue(Long ecommerceId);
}
