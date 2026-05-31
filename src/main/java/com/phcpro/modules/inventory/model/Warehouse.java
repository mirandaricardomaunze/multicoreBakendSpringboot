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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
