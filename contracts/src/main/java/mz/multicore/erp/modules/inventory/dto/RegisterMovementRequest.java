package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registo de um movimento de stock (ex.: entrada de lote) por ids. */
public record RegisterMovementRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive Long warehouseId,
        @NotNull @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @NotBlank @Size(max = 20) String movementType,
        @Size(max = 100) String batchNumber,
        @Size(max = 100) String serialNumber,
        @Size(max = 255) String description,
        LocalDate expirationDate,
        @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9._:-]+") String idempotencyKey
) {
    public RegisterMovementRequest {
        movementType = movementType == null ? null : movementType.trim().toUpperCase(java.util.Locale.ROOT);
        batchNumber = normalize(batchNumber);
        serialNumber = normalize(serialNumber);
        description = normalize(description);
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public RegisterMovementRequest(Long productId, Long warehouseId, BigDecimal quantity, String movementType,
                                   String batchNumber, String serialNumber, String description,
                                   LocalDate expirationDate) {
        this(productId, warehouseId, quantity, movementType, batchNumber, serialNumber, description,
                expirationDate, null);
    }
}
