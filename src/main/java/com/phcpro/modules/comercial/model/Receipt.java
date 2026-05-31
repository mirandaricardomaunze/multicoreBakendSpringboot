package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.financeira.model.TreasuryAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts")
@Getter
@Setter
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true)
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
