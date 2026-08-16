package mz.multicore.erp.modules.inventory.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sessão de <b>contagem de inventário</b> persistente (contagem cega + reconciliação). Nasce
 * {@code DRAFT} com uma linha por artigo do armazém; as contagens podem ser guardadas e retomadas
 * mais tarde. Ao aplicar, cada linha contada gera um ajuste de stock (via {@code InventoryService}) e
 * a sessão passa a {@code APPLIED} — ficando o histórico auditável (sistema → contado por artigo).
 */
@Entity
@Table(name = "inventory_counts")
@Getter
@Setter
public class InventoryCount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InventoryCountStatus status = InventoryCountStatus.DRAFT;

    @Column(name = "note", length = 500)
    private String note;

    /** Momento em que a sessão foi aplicada (ajustes lançados). Null enquanto DRAFT/CANCELLED. */
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    /** Quem aplicou (username). Null enquanto DRAFT/CANCELLED. */
    @Column(name = "applied_by")
    private String appliedBy;

    @OneToMany(mappedBy = "inventoryCount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InventoryCountLine> lines = new ArrayList<>();
}
