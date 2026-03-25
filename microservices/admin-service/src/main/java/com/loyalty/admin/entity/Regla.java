package com.loyalty.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "reglas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Regla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecommerce_id", nullable = false)
    private Ecommerce ecommerce;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReglaTipo tipo;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 600)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal porcentajeDescuento;

    @Column(nullable = false)
    private Integer prioridad;

    @Column(nullable = false)
    private boolean activa;

    private String productoSku;

    private String temporada;
}
