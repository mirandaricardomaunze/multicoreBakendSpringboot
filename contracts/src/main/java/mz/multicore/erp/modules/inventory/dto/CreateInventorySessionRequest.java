package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.Size;

/**
 * Pedido de abertura de sessão de inventário físico.
 */
public record CreateInventorySessionRequest(
        @Size(max = 255, message = "A descrição não pode exceder 255 caracteres.")
        String description,

        boolean blindCounting,

        @Size(max = 100, message = "O filtro de categoria não pode exceder 100 caracteres.")
        String categoryFilter
) {}
