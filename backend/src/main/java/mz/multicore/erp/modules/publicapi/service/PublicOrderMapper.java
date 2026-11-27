package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderLineDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;

/** The commercial order DTO to the public one (SPEC §3.3, §3.4): no ids, VAT rate in percent, dates with offset. */
final class PublicOrderMapper {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PublicOrderMapper() {
    }

    static PublicOrderDTO toPublic(OrderDTO order, String externalReference) {
        boolean walkIn = WalkInClientProvider.WALK_IN_TAX_ID.equals(order.clientTaxId());
        return new PublicOrderDTO(
                order.orderNumber(),
                externalReference,
                order.status(),
                order.statusLabel(),
                walkIn && order.walkInName() != null ? order.walkInName() : order.clientName(),
                walkIn ? null : order.clientTaxId(),
                order.warehouseName(),
                order.lines().stream().map(l -> new PublicOrderLineDTO(l.sku(), l.productName(), l.quantity(),
                        l.unitPrice(), percent(l.taxRate()), l.lineTotal())).toList(),
                order.totalBeforeTax(),
                order.taxAmount(),
                order.totalAmount(),
                order.createdAt() == null ? null : order.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    private static BigDecimal percent(BigDecimal rate) {
        return rate == null ? null : rate.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }
}
