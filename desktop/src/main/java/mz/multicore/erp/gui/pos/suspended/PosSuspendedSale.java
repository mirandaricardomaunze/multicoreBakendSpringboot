package mz.multicore.erp.gui.pos.suspended;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venda posta em espera neste posto. Não é um documento: não reserva stock nem tem efeito fiscal.
 * Guarda só o identificador dos artigos, para que o preço e o IVA sejam os do momento em que retoma.
 */
public record PosSuspendedSale(
        String id,
        Long companyId,
        String operator,
        LocalDateTime suspendedAt,
        Long clientId,
        String clientName,
        Long warehouseId,
        Long quotationId,
        String quotationNumber,
        BigDecimal total,
        List<Line> lines) {

    public record Line(
            Long productId,
            String productName,
            BigDecimal quantity,
            BigDecimal discount,
            String batch,
            String serial,
            String note,
            BigDecimal customUnitPrice) {}
}
