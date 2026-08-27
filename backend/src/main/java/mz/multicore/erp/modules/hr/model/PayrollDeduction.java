package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um compromisso de desconto sobre o salário: adiantamento, empréstimo ou desconto recorrente.
 * Ver docs/RH_COMPLETO_SPEC.md §B6.
 *
 * <p><b>O defeito que fecha:</b> um adiantamento saía da caixa e <b>nunca voltava</b>, porque nada
 * o ligava ao recibo do período. E o recibo mostrava um único {@code otherDeductions} anónimo — a
 * origem clássica da reclamação do trabalhador, que vê o líquido descer sem saber porquê.
 *
 * <p><b>O saldo em dívida não vive aqui.</b> Apura-se das linhas efectivamente aplicadas aos
 * recibos ({@code PayslipDeductionLine}), pela mesma razão que os totais do ponto não são gravados:
 * um saldo gravado é uma segunda verdade e desactualiza-se à primeira anulação de recibo.
 */
@Entity
@Table(name = "payroll_deductions")
@Getter
@Setter
public class PayrollDeduction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PayrollDeductionKind kind;

    /** O que aparece na linha do recibo. Um desconto sem nome volta a ser um número anónimo. */
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    /** Capital em dívida no início. Nulo num recorrente sem fim — esse tem vigência, não capital. */
    @Column(name = "principal_amount", precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal installmentAmount;

    /** Nulo = enquanto estiver activo e dentro da vigência. */
    @Column(name = "installments")
    private Integer installments;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** O dinheiro já saiu da tesouraria (adiantamento e empréstimo saem; recorrente não). */
    @Column(name = "paid_out", nullable = false)
    private boolean paidOut = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Este desconto aplica-se a um período que termina nesta data. */
    public boolean appliesOn(LocalDate periodEnd) {
        return active
                && periodEnd != null
                && !periodEnd.isBefore(startDate)
                && (endDate == null || !periodEnd.isAfter(endDate));
    }

    /**
     * Quanto falta descontar, dado o que já foi aplicado. Um recorrente sem capital nunca fica
     * saldado — devolve sempre a prestação, e é a vigência que o pára.
     */
    public BigDecimal outstanding(BigDecimal alreadyApplied) {
        if (principalAmount == null) {
            return installmentAmount;
        }
        BigDecimal remaining = principalAmount.subtract(
                alreadyApplied == null ? BigDecimal.ZERO : alreadyApplied);
        return remaining.max(BigDecimal.ZERO);
    }

    /** Já não há nada a descontar — o capital foi todo devolvido. */
    public boolean isSettled(BigDecimal alreadyApplied) {
        return principalAmount != null && outstanding(alreadyApplied).signum() <= 0;
    }
}
