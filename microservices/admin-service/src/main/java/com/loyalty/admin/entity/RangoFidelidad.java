package com.loyalty.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "rango_fidelidad")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RangoFidelidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecommerce_id", nullable = false)
    private Ecommerce ecommerce;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer minPuntos;

    @Column(nullable = false)
    private Integer maxPuntos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal porcentajeDescuento;
}
