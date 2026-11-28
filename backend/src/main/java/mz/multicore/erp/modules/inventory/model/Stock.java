package mz.multicore.erp.modules.inventory.model;

import mz.multicore.erp.modules.comercial.model.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import mz.multicore.erp.modules.inventory.service.StockAvailabilityPublisher;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "stocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "warehouse_id"})
})
@EntityListeners(StockAvailabilityPublisher.class)
@Getter
@Setter
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optimistic locking: protege o saldo de quantidade contra lost updates em vendas/entradas concorrentes.
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Quantidade tal como estava na base quando a transacção pegou nesta linha (lida, ou zero quando a linha é
     * criada) — serve só para saber, antes do commit, se o disponível cruzou o zero e avisar com
     * {@code StockAvailabilityChangedEvent} (WH-02). Não é coluna.
     */
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BigDecimal quantityAtStart;

    /** Lembra a quantidade com que a transacção começou (ao ler a linha, ou zero ao criá-la). */
    public void rememberQuantityAtStart(BigDecimal value) {
        quantityAtStart = value == null ? BigDecimal.ZERO : value;
    }

    /** Quantidade com que a transacção começou; a actual quando nada foi lembrado. */
    public BigDecimal quantityAtStart() {
        return quantityAtStart == null ? quantity : quantityAtStart;
    }
}
