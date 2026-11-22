package mz.multicore.erp.modules.accounting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

/**
 * Centro de custo de uma empresa (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1): cada armazém/loja tem o seu
 * ({@link CostCenterType#WAREHOUSE}, acompanha o armazém) e o gestor cria centros manuais. As linhas de lançamento das
 * classes 6 e 7 ficam marcadas com o centro que as gerou.
 */
@Entity
@Table(name = "cost_centers", uniqueConstraints = @UniqueConstraint(
        name = "uk_cost_centers_company_code", columnNames = {"company_id", "code"}))
@Getter
@Setter
public class CostCenter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Único na empresa; num centro de armazém é o código do armazém. */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CostCenterType type;

    /** Armazém ligado; só em centros {@link CostCenterType#WAREHOUSE}. */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** Centro inactivo não aceita lançamentos novos. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean belongsTo(Long companyId) {
        return company != null && company.getId() != null && company.getId().equals(companyId);
    }
}
