package mz.multicore.erp.modules.pickup.dto;

import java.util.List;

/** Full pickup account: summary, per-line balances and the slip history (newest first). */
public record PickupAccountDTO(
        PickupAccountSummaryDTO summary,
        String notes,
        boolean invoiceActive,
        List<PickupBalanceLineDTO> lines,
        List<MaterialPickupDTO> pickups
) {}
