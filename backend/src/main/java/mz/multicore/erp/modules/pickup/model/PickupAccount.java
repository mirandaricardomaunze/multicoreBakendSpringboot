package mz.multicore.erp.modules.pickup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDate;

/**
 * Paid material still to be collected (SA-C07). One account per invoice: the stock already left the
 * sellable balance when the invoice was issued, so this account only tracks the physical handover.
 */
@Entity
@Table(name = "pickup_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pickup_account_invoice", columnNames = {"invoice_id"})
}, indexes = {
        @Index(name = "idx_pickup_account_company_status", columnList = "company_id, status")
})
@Getter
@Setter
public class PickupAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PickupAccountStatus status = PickupAccountStatus.OPEN;

    @Column(name = "notes", length = 500)
    private String notes;
}
