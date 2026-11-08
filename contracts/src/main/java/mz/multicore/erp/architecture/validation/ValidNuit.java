package mz.multicore.erp.architecture.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declarative validation of Mozambican NUIT values.
 * Validates the canonical nine-digit pattern and optionally the Tax Authority's Modulus 11 algorithm.
 */
@Documented
@Constraint(validatedBy = ValidNuitValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNuit {
    String message() default "NUIT moçambicano inválido (deve conter exatamente 9 algarismos numéricos).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * When true, also validates the Tax Authority's Modulus 11 check digit.
     * The canonical final-consumer NUIT (999999999) is always accepted.
     */
    boolean checkModulo11() default false;
}
