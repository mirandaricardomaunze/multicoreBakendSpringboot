package mz.multicore.erp.modules.quality.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validação dos inputs HTTP de Qualidade")
class QualityInputValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("QIV-01: equipamento rejeita empresa, texto e prazo inválidos")
    void rejectsInvalidEquipment() {
        var request = new EquipmentCreateRequest(
                null, " ", " ", null, 0, null, -1, true, "x".repeat(2001));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("QIV-02: atribuição rejeita IDs e quantidade inválidos")
    void rejectsInvalidAssignment() {
        var request = new EquipmentAssignmentRequest(
                0L, null, -1L, LocalDate.now(), 0, null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("QIV-03: inputs válidos são aceites")
    void acceptsValidInputs() {
        var equipment = new EquipmentCreateRequest(
                1L, "EPI-01", "Capacete", EquipmentCategory.PPE_PROTECTION,
                12, PeriodUnit.MONTHS, "Protecção");
        var assignment = new EquipmentAssignmentRequest(
                1L, 10L, 20L, LocalDate.now(), 1);

        assertTrue(validator.validate(equipment).isEmpty());
        assertTrue(validator.validate(assignment).isEmpty());
    }
}
