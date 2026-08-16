package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts", uniqueConstraints = @UniqueConstraint(
        name = "uk_receipts_company_number", columnNames = {"company_id", "receipt_number"}))
@Getter
@Setter
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treasury_account_id", nullable = false)
    private TreasuryAccount treasuryAccount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CASH, BANK_TRANSFER, CARD

    @Column(name = "receipt_date", nullable = false)
    private LocalDateTime receiptDate = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    private String status = "COMPLETED"; // COMPLETED, CANCELLED

    @Column(name = "cancellation_reason")
    private String cancellationReason;
}
