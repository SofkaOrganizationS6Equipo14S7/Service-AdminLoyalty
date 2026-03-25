package com.loyalty.admin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracion_descuento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ecommerce_id", nullable = false, unique = true)
    private Ecommerce ecommerce;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal topeMaximo;

    @Column(nullable = false)
    private Integer prioridadGlobal;
}
