package com.phcpro.modules.company.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId; // NUIT / NIF

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;
}
