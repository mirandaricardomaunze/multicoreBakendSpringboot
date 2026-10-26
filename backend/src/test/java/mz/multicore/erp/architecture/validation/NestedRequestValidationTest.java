package mz.multicore.erp.architecture.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mz.multicore.erp.modules.comercial.dto.CompleteOrderPickingRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.OrderPickingConfirmationLineRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardLineRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NestedRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidPickingLineRecursively() {
        var request = new CompleteOrderPickingRequest(
                1L, "TERM-01", null,
                List.of(new OrderPickingConfirmationLineRequest(null, BigDecimal.ZERO, "x".repeat(121))));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidSalesReturnLineRecursively() {
        var request = new SalesReturnWizardRequest(
                1L, "RETURN", 1L, false, null,
                List.of(new SalesReturnWizardLineRequest(1L, BigDecimal.ZERO, true)));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidInstallmentValues() {
        var request = new CreateInstallmentPlanRequest(
                1L, 0, 0, LocalDate.now(), List.of(BigDecimal.ZERO));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidManifestGuideIds() {
        var request = new CreateFleetManifestRequest(
                1L, "Motorista", null, "Maputo", null, null, List.of(0L));
        assertFalse(validator.validate(request).isEmpty());
    }
}
