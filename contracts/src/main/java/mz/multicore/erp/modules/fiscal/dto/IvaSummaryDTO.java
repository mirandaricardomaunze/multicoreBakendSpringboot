package mz.multicore.erp.modules.fiscal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Periodic VAT assessment (Mozambican Model A).
 * outputTax      = VAT assessed on sales and charged to customers
 * inputTax       = VAT deducted on purchases and paid to suppliers
 * previousCredit = Tax credit carried forward from the previous period
 * netDue         = outputTax - inputTax - previousCredit
 * payableAmount  = Amount payable to the State when netDue > 0
 * creditToCarry  = Credit carried forward to the next period when netDue < 0
 * fiscalStatus   = A_PAGAR or CREDITO_A_TRANSPORTAR
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
