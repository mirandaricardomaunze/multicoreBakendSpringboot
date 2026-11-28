package mz.multicore.erp.modules.publicapi.dto;

import java.math.BigDecimal;

/** A line of a public order: system price without VAT, VAT rate in percent and the line total with VAT. */
public record PublicOrderLineDTO(String sku, String name, BigDecimal quantity, BigDecimal unitPrice,
                                 BigDecimal vatRatePercent, BigDecimal lineTotal) {
}
