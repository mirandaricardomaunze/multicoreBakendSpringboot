package com.phcpro.modules.fiscal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Apuramento mensal de IVA.
 * outputTax = IVA liquidado em vendas (faturado e cobrado ao cliente)
 * inputTax  = IVA deduzido das compras (suportado a fornecedores)
 * netDue    = outputTax − inputTax (positivo: a entregar à AT; negativo: a recuperar)
 */
public record IvaSummaryDTO(
        int year,
        int month,
        Long companyId,
        BigDecimal salesBase,
        BigDecimal outputTax,
        BigDecimal purchasesBase,
        BigDecimal inputTax,
        BigDecimal netDue,
        List<IvaLineDTO> sales,
        List<IvaLineDTO> purchases
) {
    public record IvaLineDTO(
            String documentNumber,
            String partner,
            BigDecimal base,
            BigDecimal tax,
            BigDecimal total
    ) {}
}
