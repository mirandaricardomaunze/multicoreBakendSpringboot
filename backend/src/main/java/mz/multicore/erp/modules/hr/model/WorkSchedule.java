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
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Horário de trabalho: quantas horas se esperam em cada dia da semana. Ver
 * docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>É <b>isto</b> que transforma marcações em assiduidade. Sem horário, "trabalhou 6 horas" não
 * diz nada — pode ser uma hora a menos ou duas a mais. É o horário que diz qual era a expectativa,
 * e é por diferença a ele que nascem o atraso, a hora extra e a falta.
 */
@Entity
@Table(name = "work_schedules")
@Getter
@Setter
public class WorkSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "monday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal mondayHours = new BigDecimal("8.00");
    @Column(name = "tuesday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal tuesdayHours = new BigDecimal("8.00");
    @Column(name = "wednesday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal wednesdayHours = new BigDecimal("8.00");
    @Column(name = "thursday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal thursdayHours = new BigDecimal("8.00");
    @Column(name = "friday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal fridayHours = new BigDecimal("8.00");
    /** Zero significa dia de descanso — e é isso que torna a hora trabalhada nele especial. */
    @Column(name = "saturday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal saturdayHours = BigDecimal.ZERO;
    @Column(name = "sunday_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal sundayHours = BigDecimal.ZERO;

    /**
     * Hora de entrada prevista. Sem ela o atraso é incalculável — "chegou tarde" só existe contra
     * uma expectativa, e horas/dia sozinhas não dizem a que horas se começa.
     */
    @Column(name = "expected_start_time", nullable = false)
    private LocalTime expectedStartTime = LocalTime.of(8, 0);

    /** Minutos de atraso ainda tolerados. Acima disto, é atraso registado. */
    @Column(name = "late_tolerance_minutes", nullable = false)
    private int lateToleranceMinutes = 10;

    /**
     * Janela de trabalho nocturno. É <b>dado</b> e não constante no código de propósito: o intervalo
     * varia com a convenção aplicável, e o valor a usar tem de ser confirmado com o contabilista da
     * empresa (ver §6 da spec). O que o sistema garante é a contagem, não a escolha do intervalo.
     */
    @Column(name = "night_start", nullable = false)
    private LocalTime nightStart = LocalTime.of(20, 0);
    @Column(name = "night_end", nullable = false)
    private LocalTime nightEnd = LocalTime.of(6, 0);

    /** Horas esperadas num dia da semana. Zero = dia de descanso. */
    public BigDecimal expectedHours(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> mondayHours;
            case TUESDAY -> tuesdayHours;
            case WEDNESDAY -> wednesdayHours;
            case THURSDAY -> thursdayHours;
            case FRIDAY -> fridayHours;
            case SATURDAY -> saturdayHours;
            case SUNDAY -> sundayHours;
        };
    }

    /**
     * Dia sem trabalho previsto. <b>Fonte única</b> da pergunta: é o que distingue uma hora extra
     * normal de uma hora prestada em dia de descanso, que a lei trata de outra maneira.
     */
    public boolean isRestDay(DayOfWeek day) {
        return expectedHours(day).signum() == 0;
    }

    /** A hora cai na janela nocturna (que pode atravessar a meia-noite). */
    public boolean isNightHour(LocalTime time) {
        if (time == null) return false;
        if (nightStart.isBefore(nightEnd)) {
            return !time.isBefore(nightStart) && time.isBefore(nightEnd);
        }
        // Janela que atravessa a meia-noite: 20:00→06:00 é "depois das 20" ou "antes das 6".
        return !time.isBefore(nightStart) || time.isBefore(nightEnd);
    }
}
