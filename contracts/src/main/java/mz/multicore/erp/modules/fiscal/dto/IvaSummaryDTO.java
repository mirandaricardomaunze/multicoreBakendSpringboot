package mz.multicore.erp.modules.fiscal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Apuramento periódico de IVA (Modelo A - Moçambique).
 * outputTax      = IVA liquidado em vendas (faturado e cobrado ao cliente)
 * inputTax       = IVA deduzido das compras (suportado a fornecedores)
 * previousCredit = Crédito fiscal reportado do período anterior
 * netDue         = outputTax - inputTax - previousCredit
 * payableAmount  = Montante a pagar ao Estado (se netDue > 0)
 * creditToCarry  = Crédito a transportar para o período seguinte (se netDue < 0)
 * fiscalStatus   = A_PAGAR ou CREDITO_A_TRANSPORTAR
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
        BigDecimal previousCredit,
        BigDecimal payableAmount,
        BigDecimal creditToCarry,
        String fiscalStatus,
        List<IvaLineDTO> sales,
        List<IvaLineDTO> purchases
) {

    public IvaSummaryDTO(
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
        this(
                year,
                month,
                companyId,
                salesBase,
                outputTax,
                purchasesBase,
                inputTax,
                netDue,
                BigDecimal.ZERO,
                netDue != null && netDue.compareTo(BigDecimal.ZERO) > 0 ? netDue : BigDecimal.ZERO,
                netDue != null && netDue.compareTo(BigDecimal.ZERO) < 0 ? netDue.abs() : BigDecimal.ZERO,
                netDue != null && netDue.compareTo(BigDecimal.ZERO) > 0 ? "A_PAGAR" : "CREDITO_A_TRANSPORTAR",
                sales,
                purchases
        );
    }

    public record IvaLineDTO(
            String documentNumber,
            String partner,
            BigDecimal base,
            BigDecimal tax,
            BigDecimal total
    ) {}
}
