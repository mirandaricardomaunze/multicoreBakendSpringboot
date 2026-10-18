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
        Long version,
        Long businessUnitId,
        String businessUnitName,
        /** Exige confirmação de recepção no destino (stock em trânsito). */
        boolean requiresReceipt,
        /** Momento em que a mercadoria saiu da origem (início do trânsito). */
        LocalDateTime dispatchedAt,
        String receivedBy,
        LocalDateTime receivedAt,
        String receiptNotes
) {
    /** Construtor retrocompatível de 23 argumentos (anterior ao stock em trânsito). */
    public StockTransferDTO(Long id, String transferNumber, LocalDateTime transferDate, Long companyId,
                            Long originWarehouseId, String originWarehouseName, Long destinationWarehouseId,
                            String destinationWarehouseName, String status, String responsible, String vehicle,
                            String notes, String approvedBy, LocalDateTime approvedAt, String rejectionReason,
                            List<StockTransferLineDTO> lines, Long orderId, String orderNumber,
                            String driverName, String vehiclePlate, Long version,
                            Long businessUnitId, String businessUnitName) {
        this(id, transferNumber, transferDate, companyId, originWarehouseId, originWarehouseName,
                destinationWarehouseId, destinationWarehouseName, status, responsible, vehicle, notes,
                approvedBy, approvedAt, rejectionReason, lines, orderId, orderNumber, driverName,
                vehiclePlate, version, businessUnitId, businessUnitName, false, null, null, null, null);
    }

    /** Construtor retrocompatível para 21 argumentos (anterior a businessUnitId/businessUnitName). */
    public StockTransferDTO(Long id, String transferNumber, LocalDateTime transferDate, Long companyId,
                            Long originWarehouseId, String originWarehouseName, Long destinationWarehouseId,
                            String destinationWarehouseName, String status, String responsible, String vehicle,
                            String notes, String approvedBy, LocalDateTime approvedAt, String rejectionReason,
                            List<StockTransferLineDTO> lines, Long orderId, String orderNumber,
                            String driverName, String vehiclePlate, Long version) {
        this(id, transferNumber, transferDate, companyId, originWarehouseId, originWarehouseName,
                destinationWarehouseId, destinationWarehouseName, status, responsible, vehicle, notes,
                approvedBy, approvedAt, rejectionReason, lines, orderId, orderNumber, driverName,
                vehiclePlate, version, null, null);
    }

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
