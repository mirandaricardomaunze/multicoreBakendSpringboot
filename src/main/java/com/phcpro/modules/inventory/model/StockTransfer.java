package com.phcpro.modules.inventory.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
@Getter
@Setter
public class StockTransfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true)
    private String transferNumber;

    @Column(name = "transfer_date", nullable = false)
    private LocalDateTime transferDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_warehouse_id", nullable = false)
    private Warehouse originWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id", nullable = false)
    private Warehouse destinationWarehouse;

    /**
     * Ciclo de vida da guia. Nasce PENDING_APPROVAL — o stock só sai do armazém de origem
     * quando um MANAGER/ADMIN aprova (status APPROVED). REJECTED/CANCELLED nunca movem stock.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING_APPROVAL;

    @Column(name = "responsible")
    private String responsible;

    @Column(name = "vehicle")
    private String vehicle;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Quem aprovou/rejeitou a guia (username). Null enquanto pendente. */
    @Column(name = "approved_by")
    private String approvedBy;

    /** Momento da decisão de aprovação/rejeição. Null enquanto pendente. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Motivo da rejeição, quando status = REJECTED. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockTransferLine> lines = new ArrayList<>();
}
