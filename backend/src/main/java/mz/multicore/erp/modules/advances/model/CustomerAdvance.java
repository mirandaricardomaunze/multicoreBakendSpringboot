package mz.multicore.erp.modules.advances.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A customer advance (SA-C10, {@code docs/ADIANTAMENTOS_SPEC.md}). It only links fiscal documents issued by the
 * existing services: the advance invoice and its receipt, then the credit note and the offset receipt that move
 * the amount onto the final invoice. It never holds money of its own.
 */
@Entity
@Table(name = "customer_advances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_advance_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class CustomerAdvance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerAdvanceStatus status = CustomerAdvanceStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advance_invoice_id", nullable = false)
    private Invoice advanceInvoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_invoice_id")
    private Invoice appliedInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id")
    private CreditNote creditNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offset_receipt_id")
    private Receipt offsetReceipt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "applied_by")
    private String appliedBy;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;
}
