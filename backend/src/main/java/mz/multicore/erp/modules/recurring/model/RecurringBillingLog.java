package mz.multicore.erp.modules.recurring.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_billing_logs")
@Getter
@Setter
public class RecurringBillingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private RecurringContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "billed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal billedAmount = BigDecimal.ZERO;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SUCCESS";

    @Column(name = "notes", length = 255)
    private String notes;
}
