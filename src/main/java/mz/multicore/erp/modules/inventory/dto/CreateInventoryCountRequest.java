package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;

/** Início de uma sessão de contagem de inventário: o armazém a contar e uma nota opcional. */
public record CreateInventoryCountRequest(
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        String note
) {}
