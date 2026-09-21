package mz.multicore.erp.modules.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.inventory.dto.InventoryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_physical_sessions")
@Getter
@Setter
@NoArgsConstructor
public class InventoryPhysicalSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_number", nullable = false, length = 50)
    private String inventoryNumber;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InventoryStatus status = InventoryStatus.DRAFT;

    @Column(name = "blind_counting", nullable = false)
    private boolean blindCounting = false;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "total_items", nullable = false)
    private int totalItems = 0;

    @Column(name = "items_counted", nullable = false)
    private int itemsCounted = 0;

    @Column(name = "total_surplus_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalSurplusValue = BigDecimal.ZERO;

    @Column(name = "total_deficit_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDeficitValue = BigDecimal.ZERO;

    @Column(name = "net_financial_impact", nullable = false, precision = 19, scale = 4)
    private BigDecimal netFinancialImpact = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryPhysicalItem> items = new ArrayList<>();

    public void recalculateTotals() {
        this.totalItems = items.size();
        int counted = 0;
        BigDecimal surplus = BigDecimal.ZERO;
        BigDecimal deficit = BigDecimal.ZERO;

        for (InventoryPhysicalItem item : items) {
            item.recalculate();
            if (item.getCountedQuantity().compareTo(BigDecimal.ZERO) > 0 || item.getNotes() != null) {
                counted++;
            }
            BigDecimal impact = item.getFinancialImpact();
            if (impact.compareTo(BigDecimal.ZERO) > 0) {
                surplus = surplus.add(impact);
            } else if (impact.compareTo(BigDecimal.ZERO) < 0) {
                deficit = deficit.add(impact.abs());
            }
        }
        this.itemsCounted = counted;
        this.totalSurplusValue = surplus;
        this.totalDeficitValue = deficit;
        this.netFinancialImpact = surplus.subtract(deficit);
    }
}
