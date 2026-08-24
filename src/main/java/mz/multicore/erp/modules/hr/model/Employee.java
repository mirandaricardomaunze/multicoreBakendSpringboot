package mz.multicore.erp.modules.hr.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.users.model.AppUser;
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

    /** Fotografia reduzida do colaborador, usada na ficha e identificação visual. */
    @Column(name = "photo")
    private byte[] photo;

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

    /**
     * Banco e conta para o ficheiro de pagamento da folha (§B8.7). Nulos são o caso normal de quem
     * recebe em numerário — e quem os tiver em branco fica <b>listado à parte</b> no ficheiro, em
     * vez de desaparecer dele em silêncio.
     */
    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "bank_account", length = 60)
    private String bankAccount;

    /**
     * Conta de utilizador deste colaborador, quando tem uma. É o que torna "o próprio" identificável:
     * sem ela, submeter férias ou despesa em nome de um colega é indistinguível de o fazer em nome
     * próprio. Nulo é o caso normal numa loja — quem não tem conta não faz self-service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;
}
