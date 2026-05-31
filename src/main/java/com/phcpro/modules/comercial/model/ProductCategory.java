package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Categoria de produto (Alimentação, Bebidas, Higiene, Limpeza, …).
 * Mantida simples — sem hierarquia — para a maioria das mercearias.
 */
@Entity
@Table(name = "product_categories")
@Getter
@Setter
public class ProductCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /** Cor hex para destaque visual nos cards/labels (ex.: "#10B981"). */
    @Column(name = "color_hex", length = 9)
    private String colorHex;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
