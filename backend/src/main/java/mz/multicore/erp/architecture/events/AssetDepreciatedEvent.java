package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento disparado quando uma quota de depreciação de activo fixo é processada.
 * Comunica à contabilidade o gasto do exercício e o aumento das amortizações acumuladas.
 */
public record AssetDepreciatedEvent(
        Long companyId,
        Long assetId,
        String assetCode,
        LocalDate date,
        BigDecimal depreciationAmount
) {}
