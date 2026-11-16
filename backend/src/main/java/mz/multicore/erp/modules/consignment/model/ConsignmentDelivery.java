package mz.multicore.erp.modules.consignment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Goods left at a reseller (SA-C11, {@code docs/CONSIGNACAO_ROTA_SPEC.md}). Delivering moves them to the
 * consignment warehouse; settling brings the returns back and invoices what was sold.
 */
@Entity
@Table(name = "consignment_deliveries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consignment_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class ConsignmentDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "number", nullable = false, length = 40)
    private String number;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "route", length = 80)
    private String route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consignment_warehouse_id", nullable = false)
    private Warehouse consignmentWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsignmentStatus status = ConsignmentStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "settled_by")
    private String settledBy;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ConsignmentLine> lines = new ArrayList<>();
}
