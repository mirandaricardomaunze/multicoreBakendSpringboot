package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateStockTransferRequest(
        @NotNull Long companyId,
        @NotNull Long originWarehouseId,
        @NotNull Long destinationWarehouseId,
        @Size(max = 120, message = "O responsável deve ter no máximo 120 caracteres.")
        String responsible,
        @Size(max = 50, message = "O veículo deve ter no máximo 50 caracteres.")
        String vehicle,
        @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
        String notes,
        @NotEmpty @Valid List<CreateStockTransferLineRequest> lines,
        @Size(max = 120, message = "O nome do motorista deve ter no máximo 120 caracteres.")
        String driverName,
        @Size(max = 30, message = "A matrícula do veículo deve ter no máximo 30 caracteres.")
        String vehiclePlate
) {
    public CreateStockTransferRequest {
        responsible = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(responsible);
        vehicle = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(vehicle);
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
        driverName = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(driverName);
        vehiclePlate = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(vehiclePlate);
    }
    public CreateStockTransferRequest(
            Long companyId,
            Long originWarehouseId,
            Long destinationWarehouseId,
            String responsible,
            String vehicle,
            String notes,
            List<CreateStockTransferLineRequest> lines
    ) {
        this(companyId, originWarehouseId, destinationWarehouseId, responsible, vehicle, notes, lines, responsible, vehicle);
    }
}

