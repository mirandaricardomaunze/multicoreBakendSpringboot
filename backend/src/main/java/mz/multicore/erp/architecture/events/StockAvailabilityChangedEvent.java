package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * O stock disponível de um artigo num armazém de venda cruzou o zero (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1,
 * WH-02): passou a esgotado ou voltou a ter stock. Disponível = quantidade − reservas activas, nunca negativo (o mesmo
 * cálculo do {@code GET /stock}, §3.3.1). Várias variações na mesma transacção comparam o primeiro antes com o último
 * depois. Leva só números e textos, nunca entidades.
 *
 * @param warehouseCode   número do armazém ({@code null} quando não tem)
 * @param availableBefore disponível antes da primeira variação da transacção
 * @param availableAfter  disponível depois da última variação da transacção
 * @param occurredAt      instante do facto
 */
public record StockAvailabilityChangedEvent(
        Long companyId,
        String sku,
        String productName,
        String warehouseCode,
        String warehouseName,
        BigDecimal availableBefore,
        BigDecimal availableAfter,
        OffsetDateTime occurredAt
) {
    /** {@code true} quando o disponível passou de &gt; 0 a 0. */
    public boolean wentOutOfStock() {
        return positive(availableBefore) && !positive(availableAfter);
    }

    /** {@code true} quando o disponível passou de 0 a &gt; 0. */
    public boolean cameBackInStock() {
        return !positive(availableBefore) && positive(availableAfter);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
