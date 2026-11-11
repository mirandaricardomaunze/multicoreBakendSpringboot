package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Devolução de mercadoria ao fornecedor (série interna DF). As quantidades devolvidas ficam nas linhas da compra
 * ({@link PurchaseLine#getReturnedQuantity()}); este documento guarda os totais para a dívida, a contabilidade e o IVA.
 */
@Entity
@Table(name = "supplier_returns", uniqueConstraints = @UniqueConstraint(
        name = "uk_supplier_returns_company_number", columnNames = {"company_id", "return_number"}))
@Getter
@Setter
public class SupplierReturn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_number", nullable = false, length = 40)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @Column(name = "return_date", nullable = false)
    private LocalDateTime returnDate = LocalDateTime.now();

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;
}
