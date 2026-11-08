package mz.multicore.erp.modules.bulkstock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result of a measurement. {@code pendingPickup} is sold material still on the pile (SA-C07) and is
 * deducted before comparing with the system stock. A {@code null} id means a preview that was not saved.
 */
public record BulkMeasurementDTO(
        Long id,
        Long productId,
        String productName,
        String baseUnitCode,
        Long warehouseId,
        String warehouseName,
        String method,
        String methodLabel,
        BigDecimal volumeM3,
        BigDecimal measuredQuantity,
        BigDecimal pendingPickup,
        BigDecimal systemQuantity,
        BigDecimal difference,
        BigDecimal differencePercent,
        BigDecimal tolerancePercent,
        boolean withinTolerance,
        String status,
        String statusLabel,
        String notes,
        String measuredBy,
        LocalDateTime measuredAt,
        String reviewedBy,
        String rejectionReason
) {}
