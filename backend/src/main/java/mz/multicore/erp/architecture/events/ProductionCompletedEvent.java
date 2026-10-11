package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento disparado quando uma ordem de produção industrial é concluída.
 * Comunica à contabilidade a valorização de produtos acabados (CIP) e a variação de produção.
 */
public record ProductionCompletedEvent(
        Long companyId,
        Long productionOrderId,
        String orderNumber,
        LocalDate date,
        BigDecimal totalCipCost
) {}
