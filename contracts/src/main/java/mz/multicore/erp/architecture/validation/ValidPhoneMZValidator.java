package mz.multicore.erp.architecture.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link ValidPhoneMZ} annotation.
 * Null or blank values are valid for optional fields; use @NotBlank to make a field mandatory.
 */
public class ValidPhoneMZValidator implements ConstraintValidator<ValidPhoneMZ, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return ValidationPatterns.isValidPhone(trimmed);
    }
}
