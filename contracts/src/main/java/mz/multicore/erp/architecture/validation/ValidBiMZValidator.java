package mz.multicore.erp.architecture.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para a anotação {@link ValidBiMZ}.
 * Valores nulos ou em branco são considerados válidos (para campos opcionais; use @NotBlank para tornar obrigatório).
 */
public class ValidBiMZValidator implements ConstraintValidator<ValidBiMZ, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return ValidationPatterns.isValidBi(trimmed);
    }
}
