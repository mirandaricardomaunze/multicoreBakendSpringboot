package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha de fatura disponível para devolução com dados de embalagem e saldo devolvível.
 */
public record ReturnableInvoiceLineDTO(
        Long invoiceLineId,
        Long productId,
        String productCode,
        String productName,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal invoicedQuantity,
        BigDecimal alreadyReturnedQuantity,
        BigDecimal returnableQuantity,
        BigDecimal boxes,
        int packsPerBox,
        int unitsPerPack,
        String batchNumber,
        LocalDate expiryDate
) {
    public ReturnableInvoiceLineDTO {
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (invoicedQuantity == null) invoicedQuantity = BigDecimal.ZERO;
        if (alreadyReturnedQuantity == null) alreadyReturnedQuantity = BigDecimal.ZERO;
        if (returnableQuantity == null) {
            BigDecimal diff = invoicedQuantity.subtract(alreadyReturnedQuantity);
            returnableQuantity = diff.signum() > 0 ? diff : BigDecimal.ZERO;
        }
        if (boxes == null) boxes = new BigDecimal("0.00");
    }
}
