package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento disparado quando um abastecimento de combustível de frota é registado.
 * Comunica à contabilidade o gasto de FST (Combustíveis) e a saída de tesouraria.
 */
public record FleetFuelLoggedEvent(
        Long companyId,
        Long fuelLogId,
        String licensePlate,
        LocalDate date,
        BigDecimal totalCost,
        boolean cashPayment
) {}
