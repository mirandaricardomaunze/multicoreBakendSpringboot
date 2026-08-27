package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DeliveryGuideDTO(
        Long id,
        String guideNumber,
        LocalDateTime guideDate,
        Long companyId,
        Long orderId,
        String orderNumber,
        Long clientId,
        String clientName,
        Long warehouseId,
        String warehouseName,
        String status,
        BigDecimal totalAmount,
        String responsible,
        String vehicle,
        String notes,
        String approvedBy,
        LocalDateTime approvedAt,
        String rejectionReason,
        List<DeliveryGuideLineDTO> lines
) {}
