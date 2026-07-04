package com.phcpro.modules.inventory.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
public class Warehouse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location")
    private String location;

    @Column(name = "warehouse_number")
    private String warehouseNumber;

    @Column(name = "capacity", precision = 12, scale = 3)
    private BigDecimal capacity;

    /** Activo: um armazém inactivo não aparece nos fluxos operacionais (mas preserva histórico). */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Tipo/função do armazém (Loja, Depósito, Central, Trânsito). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private WarehouseType type = WarehouseType.STORE;

    /** Se este local pode vender ao balcão (POS). Um depósito puro costuma ter isto a falso. */
    @Column(name = "allows_sales", nullable = false)
    private boolean allowsSales = true;

    /** Responsável / gestor do armazém. */
    @Column(name = "manager")
    private String manager;

    /** Telefone / contacto do local. */
    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
