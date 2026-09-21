package mz.multicore.erp.modules.inventory.dto;

/**
 * Pedido de abertura de sessão de inventário físico.
 */
public record CreateInventorySessionRequest(
        String description,
        boolean blindCounting,
        String categoryFilter
) {}
