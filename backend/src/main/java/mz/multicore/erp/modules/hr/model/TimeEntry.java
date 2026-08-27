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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Marcação de ponto de um colaborador num dia. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>É a origem que faltava: hoje as horas extra do recibo são um número que alguém escreve à mão,
 * que ninguém sabe de onde veio e que ninguém pode contestar. Uma marcação é um facto datado, com
 * autor e proveniência.
 */
@Entity
@Table(name = "time_entries")
@Getter
@Setter
public class TimeEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Redundante com {@code employee.company}, mas é por aqui que se filtra a empresa activa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "check_in", nullable = false)
    private LocalTime checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalTime checkOut;

    /** Pausa não remunerada, em minutos. Sai das horas trabalhadas. */
    @Column(name = "break_minutes", nullable = false)
    private int breakMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private TimeEntrySource source = TimeEntrySource.MANUAL;

    /** Quem registou. Numa marcação manual é a única coisa que responde por ela. */
    @Column(name = "recorded_by", nullable = false, length = 120)
    private String recordedBy;

    @Column(name = "notes", length = 300)
    private String notes;

    /**
     * Horas efectivamente trabalhadas, já sem a pausa. <b>Fonte única</b> do cálculo — nem o
     * serviço nem o painel voltam a subtrair pausas à mão.
     *
     * <p>Uma saída anterior à entrada é tratada como turno que atravessa a meia-noite: quem entra
     * às 22:00 e sai às 06:00 trabalhou oito horas, não menos dezasseis.
     */
    public BigDecimal workedHours() {
        Duration span = Duration.between(checkIn, checkOut);
        if (span.isNegative() || span.isZero()) {
            span = span.plusDays(1);
        }
        long minutes = span.toMinutes() - breakMinutes;
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** O turno atravessa a meia-noite — a saída é no dia seguinte ao da entrada. */
    public boolean crossesMidnight() {
        return !checkOut.isAfter(checkIn);
    }
}
