package mz.multicore.erp.modules.productionplan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A batch made for a plan line: ingredients out, product in, with the real cost. */
@Entity
@Table(name = "production_runs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_production_run_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class ProductionRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private ProductionPlanLine line;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "batch_number", nullable = false, length = 60)
    private String batchNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "ingredient_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal ingredientCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;
}
