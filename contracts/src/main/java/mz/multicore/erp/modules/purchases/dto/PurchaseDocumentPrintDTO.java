package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What the purchase-order and purchase PDFs print (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3.1).
 * Amounts are in the document currency; {@code baseTotalAmount} is the stored total in the company base currency.
 */
public record PurchaseDocumentPrintDTO(
        Long id,
        Long companyId,
        String documentNumber,
        LocalDateTime documentDate,
        String supplierName,
        String supplierTaxId,
        String warehouseName,
        String currency,
        String baseCurrency,
        BigDecimal exchangeRate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal baseTotalAmount,
        List<Line> lines
) {

    /** One printed line: article, quantity, unit price, VAT rate (fraction) and line total with VAT. */
    public record Line(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate,
                       BigDecimal lineTotal) {}

    /** Documents in the base currency show only the currency code, without rate nor base total. */
    public boolean foreignCurrency() {
        return currency != null && !currency.equalsIgnoreCase(baseCurrency);
    }
}
