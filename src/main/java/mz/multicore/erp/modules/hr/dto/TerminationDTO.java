package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * O acerto final de um colaborador que sai. Ver docs/RH_COMPLETO_SPEC.md §B3.
 *
 * @param netAmount        pode ser <b>negativo</b>: quem sai com um empréstimo maior do que os
 *                         proporcionais fica a dever à empresa, e isso tem de aparecer
 * @param warnings         o que ficou por calcular por falta de configuração (§6), em PT-MZ.
 *                         Um acerto que esconde o que não sabe calcular é pior do que um acerto
 *                         incompleto que o diz
 */
public record TerminationDTO(
        Long id,
        String settlementNumber,
        Long employeeId,
        String employeeName,
        Long contractId,
        String contractNumber,
        LocalDate terminationDate,
        String reason,
        String reasonLabel,
        boolean noticeServed,
        BigDecimal totalEarnings,
        BigDecimal totalDeductions,
        BigDecimal netAmount,
        String status,
        String statusLabel,
        LocalDate paymentDate,
        String notes,
        List<TerminationLineDTO> lines,
        List<String> warnings
) {
    public record TerminationLineDTO(String description, BigDecimal amount, boolean earning) {}
}
