package mz.multicore.erp.modules.pickup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Hands over part of the paid material. {@code requestKey} makes the request idempotent so a double
 * click or a retry after a network failure never issues two slips.
 */
public record IssuePickupRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Conta de levantamento é obrigatória.") Long accountId,
        @NotBlank(message = "Indique quem levanta o material.") String collectorName,
        String collectorDocument,
        String vehiclePlate,
        String notes,
        String requestKey,
        @NotEmpty(message = "Indique pelo menos uma linha a levantar.") List<@Valid IssuePickupLineRequest> lines
) {}
