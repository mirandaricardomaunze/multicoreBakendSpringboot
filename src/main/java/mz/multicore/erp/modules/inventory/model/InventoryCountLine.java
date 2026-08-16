package mz.multicore.erp.modules.inventory.model;

import mz.multicore.erp.modules.comercial.model.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Linha de uma {@link InventoryCount}: um artigo do armazém e a sua contagem. {@code countedQuantity}
 * fica null até ser contado (contagem cega); {@code systemQuantity} guarda a quantidade do sistema no
 * momento da aplicação (para o histórico da diferença). Não estende BaseEntity — não precisa de auditoria
 * própria (a sessão-pai já a tem).
 */
@Entity
@Table(name = "inventory_count_lines")
@Getter
@Setter
public class InventoryCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Quantidade contada fisicamente. Null = ainda não contado (não é tocado ao aplicar). */
    @Column(name = "counted_quantity")
    private BigDecimal countedQuantity;

    /** Quantidade do sistema no momento da aplicação (snapshot para o histórico da diferença). */
    @Column(name = "system_quantity")
    private BigDecimal systemQuantity;

    @Column(name = "applied", nullable = false)
    private boolean applied = false;
}
