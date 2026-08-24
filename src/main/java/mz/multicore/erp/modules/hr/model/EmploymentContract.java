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
import java.time.temporal.ChronoUnit;

/**
 * Contrato de trabalho de um colaborador. Um colaborador tem N contratos ao longo do tempo —
 * renovações, mudanças de função — mas <b>um só vigente</b> numa data.
 *
 * <p>A regra que carrega o bloco: <b>o contrato manda na folha</b>. Antes disto, a folha mensal
 * filtrava só por {@code status == ACTIVE} na ficha, pelo que um colaborador cujo contrato terminou
 * a 31 de Julho continuava a receber recibo em Agosto — em silêncio, com saída de tesouraria e
 * tudo. Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
@Entity
@Table(name = "employment_contracts")
@Getter
@Setter
public class EmploymentContract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número da série própria (gapless, por empresa) — molde do {@code DocumentSeries.PAYSLIP}. */
    @Column(name = "contract_number", nullable = false, length = 40)
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Redundante com {@code employee.company}, mas é por aqui que se filtra a empresa activa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status = ContractStatus.RASCUNHO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Nulo em contrato sem termo e em termo incerto — ver {@link ContractType#requiresEndDate}. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Fim do período experimental. Nulo quando não há período experimental acordado. */
    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    /** Salário acordado. É <b>este</b> que a folha usa, não o da ficha do colaborador. */
    @Column(name = "agreed_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal agreedSalary;

    @Column(name = "weekly_hours", nullable = false)
    private int weeklyHours = 40;

    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle;

    @Column(name = "work_location", length = 200)
    private String workLocation;

    /** Justificação do termo. Obrigatório em contrato a termo — exigência da lei laboral. */
    @Column(name = "term_reason", length = 500)
    private String termReason;

    /** Contrato de que este é renovação. O histórico do que foi acordado é imutável. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewed_from_id")
    private EmploymentContract renewedFrom;

    /** Data em que cessou, e porquê. Preenchidas na cessação. */
    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    /**
     * O prazo passou. <b>Fonte única</b> da pergunta — nem o painel nem o serviço comparam datas à
     * mão. O último dia de contrato ainda conta como trabalhado: quem tem contrato "até 31/07"
     * trabalha no dia 31.
     */
    public boolean isExpired(LocalDate today) {
        return today != null && endDate != null && today.isAfter(endDate);
    }

    /**
     * Este contrato cobre a data dada — vigente, já começado e ainda não caducado. É o que a folha
     * mensal pergunta antes de gerar um recibo, e o que garante que ninguém recebe por um mês que
     * já não trabalhou.
     */
    public boolean coversDate(LocalDate date) {
        return status == ContractStatus.VIGENTE
                && date != null
                && !date.isBefore(startDate)
                && !isExpired(date);
    }

    /** Dias até ao fim; zero no último dia, negativo depois. Nulo quando não tem termo. */
    public Long daysUntilEnd(LocalDate today) {
        if (today == null || endDate == null) return null;
        return ChronoUnit.DAYS.between(today, endDate);
    }

    /** Ainda dentro do período experimental na data dada. */
    public boolean isInProbation(LocalDate today) {
        return today != null && probationEndDate != null && !today.isAfter(probationEndDate);
    }
}
