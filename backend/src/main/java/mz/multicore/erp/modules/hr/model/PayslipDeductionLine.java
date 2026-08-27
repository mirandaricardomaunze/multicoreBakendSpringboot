package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.math.BigDecimal;

/**
 * O que um recibo levou de facto de um desconto. Ver docs/RH_COMPLETO_SPEC.md §B6.
 *
 * <p>É esta tabela que faz duas coisas ao mesmo tempo: <b>discrimina o recibo</b> (uma linha por
 * desconto, em vez de um total anónimo) e <b>é a fonte do saldo em dívida</b>, que por isso não
 * precisa de ser gravado em lado nenhum.
 *
 * <p>Único por recibo+desconto: sem isso, reprocessar um recibo cobrava a mesma prestação duas
 * vezes ao colaborador e o empréstimo ficava pago mais depressa do que devia.
 */
@Entity
@Table(name = "payslip_deduction_lines", uniqueConstraints = @UniqueConstraint(
        name = "uk_payslip_deduction_lines", columnNames = {"payslip_id", "deduction_id"}))
@Getter
@Setter
public class PayslipDeductionLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deduction_id", nullable = false)
    private PayrollDeduction deduction;

    /** Copiada do compromisso no momento em que se aplica — o recibo emitido não muda depois. */
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}
