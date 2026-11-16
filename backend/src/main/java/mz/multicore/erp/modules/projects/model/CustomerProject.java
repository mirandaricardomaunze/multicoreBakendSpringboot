package mz.multicore.erp.modules.projects.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.company.model.Company;

/** A client's construction site or project (SA-C09), so a contractor gets one statement per site. */
@Entity
@Table(name = "customer_projects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_project_code", columnNames = {"company_id", "code"})
})
@Getter
@Setter
public class CustomerProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "contact", length = 150)
    private String contact;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
