package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InventoryInputValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void movementRejectsInvalidIdsPrecisionLengthsAndKey() {
        RegisterMovementRequest request = new RegisterMovementRequest(
                0L, -1L, new BigDecimal("1234567890.1234"), " ",
                "L".repeat(101), "S".repeat(101), "D".repeat(256), LocalDate.now(), "invalid key");

        assertTrue(validator.validate(request).size() >= 8);
    }

    @Test
    void movementNormalizesTextsBeforeFingerprintAndPersistence() {
        RegisterMovementRequest request = new RegisterMovementRequest(
                1L, 2L, new BigDecimal("1.000"), " entry ", " L-1 ", " ", " Entrada ", null,
                " MOVEMENT-1 ");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("ENTRY", request.movementType());
        assertEquals("L-1", request.batchNumber());
        assertNull(request.serialNumber());
        assertEquals("Entrada", request.description());
        assertEquals("MOVEMENT-1", request.idempotencyKey());
    }

    @Test
    void adjustmentRejectsInvalidTenantPrecisionReasonAndKey() {
        CreateStockAdjustmentRequest request = new CreateStockAdjustmentRequest(
                0L, -1L, 0L, new BigDecimal("1.1234"), "X".repeat(161), "invalid key");

        assertTrue(validator.validate(request).size() >= 6);
    }
}
