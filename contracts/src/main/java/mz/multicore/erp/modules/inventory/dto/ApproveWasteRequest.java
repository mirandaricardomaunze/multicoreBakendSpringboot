package mz.multicore.erp.modules.inventory.dto;

public record ApproveWasteRequest(
        boolean approved,
        String notes
) {}
