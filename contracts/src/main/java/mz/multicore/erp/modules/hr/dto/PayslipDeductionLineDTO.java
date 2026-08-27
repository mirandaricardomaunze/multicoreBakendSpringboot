package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;

/** Uma linha de desconto de um recibo — o que substitui o total anónimo (RHC-63). */
public record PayslipDeductionLineDTO(
        Long id,
        Long deductionId,
        String kind,
        String kindLabel,
        String description,
        BigDecimal amount
) {}
