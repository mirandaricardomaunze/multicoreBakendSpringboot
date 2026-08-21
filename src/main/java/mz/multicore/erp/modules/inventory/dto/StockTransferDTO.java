package mz.multicore.erp.modules.inventory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockTransferDTO(
        Long id,
        String transferNumber,
        LocalDateTime transferDate,
        Long companyId,
        Long originWarehouseId,
        String originWarehouseName,
        Long destinationWarehouseId,
        String destinationWarehouseName,
        String status,
        String responsible,
        String vehicle,
        String notes,
        String approvedBy,
        LocalDateTime approvedAt,
        String rejectionReason,
        List<StockTransferLineDTO> lines,
        /** Encomenda de reposição ligada a esta transferência; nulo quando foi feita directamente. */
        Long orderId,
        String orderNumber
) {
    /** Construtor retrocompatível de quem construía o DTO antes da reposição interna existir. */
    public StockTransferDTO(Long id, String transferNumber, LocalDateTime transferDate, Long companyId,
                            Long originWarehouseId, String originWarehouseName, Long destinationWarehouseId,
                            String destinationWarehouseName, String status, String responsible, String vehicle,
                            String notes, String approvedBy, LocalDateTime approvedAt, String rejectionReason,
                            List<StockTransferLineDTO> lines) {
        this(id, transferNumber, transferDate, companyId, originWarehouseId, originWarehouseName,
                destinationWarehouseId, destinationWarehouseName, status, responsible, vehicle, notes,
                approvedBy, approvedAt, rejectionReason, lines, null, null);
    }
}
