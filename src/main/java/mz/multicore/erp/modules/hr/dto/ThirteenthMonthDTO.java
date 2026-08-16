package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.util.List;

/** Apuramento do 13.º mês (subsídio de Natal) proporcional ao tempo de serviço no ano. */
public record ThirteenthMonthDTO(
        int year,
        BigDecimal total,
        List<ThirteenthMonthLineDTO> lines
) {
    public record ThirteenthMonthLineDTO(
            Long employeeId,
            String employeeNumber,
            String employeeName,
            BigDecimal baseSalary,
            int monthsWorked,
            BigDecimal amount
    ) {}
}
