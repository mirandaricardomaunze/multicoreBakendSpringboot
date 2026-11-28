package mz.multicore.erp.modules.publicapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of {@code POST /api/public/v1/orders} (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, §3.4.1). No prices: they come
 * from the system. Customer by {@code nuit} (with {@code customerName} when the NUIT is new to the company) or, without
 * NUIT, Consumidor Final with {@code contactName} and {@code contactPhone}.
 */
public record CreatePublicOrderRequest(
        @NotBlank(message = "Indique a referência externa da encomenda (externalReference).")
        @Size(max = 80, message = "A referência externa deve ter no máximo 80 caracteres.")
        String externalReference,
        String nuit,
        @Size(max = 200, message = "O nome do cliente deve ter no máximo 200 caracteres.")
        String customerName,
        @Size(max = 120, message = "O nome de contacto deve ter no máximo 120 caracteres.")
        String contactName,
        @Size(max = 40, message = "O telefone de contacto deve ter no máximo 40 caracteres.")
        String contactPhone,
        @NotBlank(message = "Indique o armazém de venda (warehouse).")
        String warehouse,
        @NotEmpty(message = "A encomenda deve conter pelo menos uma linha.") @Valid
        List<PublicOrderLineRequest> lines,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String notes) {
}
