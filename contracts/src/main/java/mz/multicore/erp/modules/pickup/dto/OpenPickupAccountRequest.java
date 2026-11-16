package mz.multicore.erp.modules.pickup.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Turns a realised invoice into a pickup account; the deadline is optional. */
public record OpenPickupAccountRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Fatura é obrigatória.") Long invoiceId,
        LocalDate deadline,
        String notes
) {}
