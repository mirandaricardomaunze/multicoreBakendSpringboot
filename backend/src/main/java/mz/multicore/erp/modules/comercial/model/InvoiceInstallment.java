package mz.multicore.erp.modules.comercial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_installments", uniqueConstraints = @UniqueConstraint(
        name = "uk_invoice_installment", columnNames = {"invoice_id", "installment_number"}))
@Getter
@Setter
public class InvoiceInstallment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "total_installments", nullable = false)
    private int totalInstallments;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal remainingBalance = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "notes", length = 255)
    private String notes;

    /**
     * Aplica uma amortização na prestação e recalcula o estado sem exceder o saldo.
     * Devolve o montante que sobrou da amortização (se o pagamento excedeu a prestação).
     */
    public BigDecimal applyPayment(BigDecimal payment) {
        if (payment == null || payment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal toApply = payment.min(this.remainingBalance);
        this.paidAmount = this.paidAmount.add(toApply);
        this.remainingBalance = this.amount.subtract(this.paidAmount);

        updateStatusBasedOnBalance();
        return payment.subtract(toApply);
    }

    public void updateStatusBasedOnBalance() {
        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = "PAID";
            this.remainingBalance = BigDecimal.ZERO;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = "PARTIALLY_PAID";
        } else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            this.status = "OVERDUE";
        } else {
            this.status = "PENDING";
        }
    }
}
