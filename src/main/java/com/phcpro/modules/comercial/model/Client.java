package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
public class Client extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId; // NIF in Portugal / CNPJ/CPF in Brazil

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "address")
    private String address;
}
