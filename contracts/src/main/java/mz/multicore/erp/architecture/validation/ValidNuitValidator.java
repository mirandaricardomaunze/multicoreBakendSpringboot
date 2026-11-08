package mz.multicore.erp.architecture.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link ValidNuit} annotation.
 * Null or blank values are valid for optional fields; use @NotBlank to make a field mandatory.
 */
public class ValidNuitValidator implements ConstraintValidator<ValidNuit, String> {

    private boolean checkModulo11;

    @Override
    public void initialize(ValidNuit constraintAnnotation) {
        this.checkModulo11 = constraintAnnotation.checkModulo11();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim().replace(" ", "").replace("-", "");
        if (trimmed.isEmpty()) {
            return true;
        }
        if (checkModulo11) {
            return ValidationPatterns.isValidNuit(trimmed);
        }
        return ValidationPatterns.NUIT_PATTERN.matcher(trimmed).matches();
    }
}
