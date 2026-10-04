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
        String orderNumber,
        String driverName,
        String vehiclePlate,
        Long version
) {
    /** Construtor retrocompatível de quem construía o DTO antes da reposição interna existir. */
    public StockTransferDTO(Long id, String transferNumber, LocalDateTime transferDate, Long companyId,
                            Long originWarehouseId, String originWarehouseName, Long destinationWarehouseId,
                            String destinationWarehouseName, String status, String responsible, String vehicle,
                            String notes, String approvedBy, LocalDateTime approvedAt, String rejectionReason,
                            List<StockTransferLineDTO> lines) {
        this(id, transferNumber, transferDate, companyId, originWarehouseId, originWarehouseName,
                destinationWarehouseId, destinationWarehouseName, status, responsible, vehicle, notes,
                approvedBy, approvedAt, rejectionReason, lines, null, null, null, null, null);
    }

    /** Construtor retrocompatível com suporte a reposição interna, sem campos de transporte explícitos. */
    public StockTransferDTO(Long id, String transferNumber, LocalDateTime transferDate, Long companyId,
                            Long originWarehouseId, String originWarehouseName, Long destinationWarehouseId,
                            String destinationWarehouseName, String status, String responsible, String vehicle,
                            String notes, String approvedBy, LocalDateTime approvedAt, String rejectionReason,
                            List<StockTransferLineDTO> lines, Long orderId, String orderNumber) {
        this(id, transferNumber, transferDate, companyId, originWarehouseId, originWarehouseName,
                destinationWarehouseId, destinationWarehouseName, status, responsible, vehicle, notes,
                approvedBy, approvedAt, rejectionReason, lines, orderId, orderNumber, null, null, null);
    }

    /** Construtor retrocompatível com transporte explícito, anterior ao controlo optimista. */
    public StockTransferDTO(Long id, String transferNumber, LocalDateTime transferDate, Long companyId,
                            Long originWarehouseId, String originWarehouseName, Long destinationWarehouseId,
                            String destinationWarehouseName, String status, String responsible, String vehicle,
                            String notes, String approvedBy, LocalDateTime approvedAt, String rejectionReason,
                            List<StockTransferLineDTO> lines, Long orderId, String orderNumber,
                            String driverName, String vehiclePlate) {
        this(id, transferNumber, transferDate, companyId, originWarehouseId, originWarehouseName,
                destinationWarehouseId, destinationWarehouseName, status, responsible, vehicle, notes,
                approvedBy, approvedAt, rejectionReason, lines, orderId, orderNumber, driverName,
                vehiclePlate, null);
    }
}
