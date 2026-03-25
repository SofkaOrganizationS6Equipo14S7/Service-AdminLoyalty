package com.loyalty.admin.repository;

import com.loyalty.admin.entity.RangoFidelidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RangoFidelidadRepository extends JpaRepository<RangoFidelidad, Long> {
    List<RangoFidelidad> findByEcommerceId(Long ecommerceId);
}
