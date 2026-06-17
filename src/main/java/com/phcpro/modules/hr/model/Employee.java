package com.phcpro.modules.hr.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "employee_number"}),
        @UniqueConstraint(columnNames = {"company_id", "email"})
})
@Getter
@Setter
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "employee_number", length = 30)
    private String employeeNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @Column(name = "inss_number", length = 40)
    private String inssNumber;

    @Column(name = "dependents_count", nullable = false)
    private int dependentsCount = 0;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "employment_status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, TERMINATED
}
