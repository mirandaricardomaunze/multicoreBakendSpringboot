package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "client_companies",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_client_company", columnNames = {"client_id", "company_id"})
    )
    private Set<Company> companies = new LinkedHashSet<>();

    public boolean belongsToCompany(Long companyId) {
        return companyId != null && companies.stream().anyMatch(company -> companyId.equals(company.getId()));
    }
}
