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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma retenção da folha que ainda não foi entregue ao Estado.
 * Ver docs/RH_COMPLETO_SPEC.md §B5.
 *
 * <p><b>O que esta tabela existe para impedir:</b> até aqui o IRPS retido e o INSS eram calculados,
 * impressos no mapa fiscal e nunca mais tocados. O dinheiro ficava na conta da empresa sem estar
 * marcado como dívida — indistinguível de dinheiro próprio. Quem o gasta não descobre o buraco no
 * mês em que o gasta; descobre no dia da entrega.
 *
 * <p>A obrigação é <b>por período e por tipo</b>, porque é assim que se entrega. Nasce quando a
 * folha é <b>paga</b> (é no pagamento que a retenção acontece) e fecha quando alguém a entrega,
 * com saída de tesouraria pela mesma porta do recibo.
 */
@Entity
@Table(name = "payroll_liabilities", uniqueConstraints = @UniqueConstraint(
        name = "uk_payroll_liabilities_period_type",
        columnNames = {"company_id", "ref_year", "ref_month", "liability_type"}))
@Getter
@Setter
public class PayrollLiability extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "ref_year", nullable = false)
    private int year;

    @Column(name = "ref_month", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_type", nullable = false, length = 30)
    private PayrollLiabilityType liabilityType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * Nulo enquanto o prazo legal não estiver configurado (§6). A obrigação nasce na mesma: não
     * saber o prazo nunca foi razão para perder o rasto do dinheiro.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollLiabilityStatus status = PayrollLiabilityStatus.POR_ENTREGAR;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "delivered_by", length = 120)
    private String deliveredBy;

    public boolean isPending() {
        return status == PayrollLiabilityStatus.POR_ENTREGAR;
    }

    /**
     * Já passou do prazo. Derivado da data contra "hoje", nunca gravado — mesma lição da caducidade
     * da cotação e do contrato: sem agendador nocturno e sem linhas desactualizadas entre passagens.
     */
    public boolean isOverdue(LocalDate today) {
        return isPending() && dueDate != null && today != null && today.isAfter(dueDate);
    }

    /** Dias até ao prazo; nulo quando o prazo não está configurado. */
    public Long daysUntilDue(LocalDate today) {
        if (dueDate == null || today == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
    }
}
