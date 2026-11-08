package mz.multicore.erp.modules.pickup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cancels a pickup slip; the reason is mandatory and stays in the audit trail. */
public record CancelPickupRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotBlank(message = "Indique o motivo da anulação.") String reason
) {}
