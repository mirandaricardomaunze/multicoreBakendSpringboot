package mz.multicore.erp.modules.publicapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A product as the public API shows it (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3.1, API-07): sale data only — never
 * cost, wholesale price, margin, supplier or business unit.
 */
public record PublicProductDTO(String sku, String name, String category, String barcode, String unit,
                               BigDecimal vatRatePercent, BigDecimal priceWithVat, OffsetDateTime updatedAt) {
}
