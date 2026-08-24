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
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Acréscimos de hora extra por escalão. Molde do {@code PayrollTaxConfig}: configurável por empresa,
 * com vigência e <b>base legal registada</b>. Ver docs/RH_COMPLETO_SPEC.md §B2 e §6.
 *
 * <p><b>Não há valores por omissão.</b> Sem configuração, o sistema recusa-se a valorizar horas
 * extra e diz porquê — em vez de multiplicar por um número inventado. Um acréscimo escrito à sorte
 * é o pior resultado possível: parece certo e paga mal, e ninguém repara até alguém reclamar. Os
 * valores a usar têm de ser confirmados com o contabilista da empresa.
 */
@Entity
@Table(name = "overtime_rate_configs")
@Getter
@Setter
public class OvertimeRateConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** Multiplicador sobre o valor/hora normal, para extra em dia útil dentro do horário diurno. */
    @Column(name = "day_multiplier", nullable = false, precision = 7, scale = 4)
    private BigDecimal dayMultiplier;

    /** Multiplicador para extra prestada dentro da janela nocturna. */
    @Column(name = "night_multiplier", nullable = false, precision = 7, scale = 4)
    private BigDecimal nightMultiplier;

    /** Multiplicador para horas prestadas em dia de descanso ou feriado. */
    @Column(name = "rest_day_multiplier", nullable = false, precision = 7, scale = 4)
    private BigDecimal restDayMultiplier;

    /**
     * De onde vieram estes valores. É o que permite a quem audita — ou ao contabilista seguinte —
     * saber contra o quê conferir, em vez de encontrar três números sem proveniência.
     */
    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Esta configuração vigora na data dada. */
    public boolean coversDate(LocalDate date) {
        return active
                && date != null
                && !date.isBefore(effectiveFrom)
                && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    /** Multiplicador do escalão. Fonte única — nem o serviço nem o painel escolhem o campo à mão. */
    public BigDecimal multiplierFor(OvertimeTier tier) {
        return switch (tier) {
            case DIURNA -> dayMultiplier;
            case NOCTURNA -> nightMultiplier;
            case DESCANSO -> restDayMultiplier;
        };
    }
}
