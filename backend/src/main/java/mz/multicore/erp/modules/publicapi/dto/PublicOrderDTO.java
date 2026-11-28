package mz.multicore.erp.modules.publicapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A customer order as the public API shows it (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3, §3.4, §3.4.3): number, state, lines
 * and totals and the number of the invoice once billed ({@code null} before); no internal ids, costs or margins.
 */
public record PublicOrderDTO(
        String number,
        String externalReference,
        String status,
        String statusLabel,
        String customerName,
        String customerNuit,
        String warehouseName,
        List<PublicOrderLineDTO> lines,
        BigDecimal totalBeforeTax,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        String invoiceNumber) {

    public PublicOrderDTO {
        lines = List.copyOf(lines);
    }
}
