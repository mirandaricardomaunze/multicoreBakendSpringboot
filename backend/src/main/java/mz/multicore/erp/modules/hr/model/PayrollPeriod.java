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
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;

/**
 * O mês da folha salarial, aberto ou fechado. Ver docs/RH_COMPLETO_SPEC.md §B8.6.
 *
 * <p><b>O que fecha:</b> {@code processMonthlyPayroll} corria para qualquer mês, sempre. Um mês já
 * pago, já entregue ao Estado e já contabilizado continuava a aceitar recibos novos — e cada recibo
 * novo nesse mês desalinhava a retenção já declarada (§B5) sem nada avisar.
 *
 * <p>Note-se que é <b>coisa diferente</b> do fecho da folha de <i>ponto</i> ({@code TimeSheet}):
 * o ponto fecha-se para as horas deixarem de mudar antes de a folha correr; a folha fecha-se depois
 * de estar paga, para o mês deixar de aceitar recibos. Um não substitui o outro.
 */
@Entity
@Table(name = "payroll_periods", uniqueConstraints = @UniqueConstraint(
        name = "uk_payroll_periods", columnNames = {"company_id", "ref_year", "ref_month"}))
@Getter
@Setter
public class PayrollPeriod extends BaseEntity {

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

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ABERTO"; // ABERTO | FECHADO

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "reopen_reason", length = 500)
    private String reopenReason;

    public boolean isClosed() {
        return "FECHADO".equals(status);
    }
}
