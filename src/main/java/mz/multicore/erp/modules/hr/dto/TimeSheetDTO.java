package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Folha de ponto de um mês: o estado do período e uma linha por colaborador.
 * Ver docs/RH_COMPLETO_SPEC.md §B2.
 */
public record TimeSheetDTO(
        int year,
        int month,
        String status,
        String statusLabel,
        boolean closed,
        String closedBy,
        List<TimeSheetLineDTO> lines
) {
    /**
     * O apuramento de um colaborador no mês. As horas extra vêm <b>separadas por escalão</b> porque
     * a lei as trata de maneira diferente — juntá-las num total só perderia a informação que decide
     * quanto se paga.
     */
    public record TimeSheetLineDTO(
            Long employeeId,
            String employeeName,
            int expectedDays,
            int workedDays,
            /** Dias previstos sem qualquer marcação — candidatos a falta. */
            int missingDays,
            BigDecimal expectedHours,
            BigDecimal workedHours,
            BigDecimal normalHours,
            /** Extra em dia normal, dentro do horário diurno. */
            BigDecimal overtimeDayHours,
            /** Extra em dia normal, dentro da janela nocturna. */
            BigDecimal overtimeNightHours,
            /** Horas prestadas em dia de descanso ou feriado — todas extraordinárias. */
            BigDecimal restDayHours,
            int lateArrivals
    ) {}
}
