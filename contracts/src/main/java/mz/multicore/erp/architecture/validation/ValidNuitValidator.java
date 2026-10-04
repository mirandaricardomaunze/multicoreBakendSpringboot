package mz.multicore.erp.architecture.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para a anotação {@link ValidNuit}.
 * Valores nulos ou em branco são considerados válidos (para campos opcionais; use @NotBlank para tornar obrigatório).
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
