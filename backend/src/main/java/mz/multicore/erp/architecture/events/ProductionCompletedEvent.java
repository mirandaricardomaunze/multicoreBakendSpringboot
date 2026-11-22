package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento disparado quando uma ordem de produção industrial é concluída.
 * Comunica à contabilidade a valorização de produtos acabados (CIP) e a variação de produção.
 *
 * @param warehouseId armazém onde o produto acabado entrou — a variação de produção fica no seu centro de custo
 *                    (CC-05); {@code null} usa o centro por omissão
 */
public record ProductionCompletedEvent(
        Long companyId,
        Long productionOrderId,
        String orderNumber,
        LocalDate date,
        BigDecimal totalCipCost,
        Long warehouseId
) {
    /** Ordem sem armazém de destino conhecido (centro por omissão). */
    public ProductionCompletedEvent(Long companyId, Long productionOrderId, String orderNumber, LocalDate date,
                                    BigDecimal totalCipCost) {
        this(companyId, productionOrderId, orderNumber, date, totalCipCost, null);
    }
}
