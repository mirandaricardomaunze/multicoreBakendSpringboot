package mz.multicore.erp.architecture.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declarative validation of Mozambican telephone numbers.
 * Supports mobile (82/83, 84/85, 86/87) and landline (21-28) networks, with or without +258.
 */
@Documented
@Constraint(validatedBy = ValidPhoneMZValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneMZ {
    String message() default "Número de telefone moçambicano inválido (operadoras 82/83, 84/85, 86/87 ou fixos 21-28).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
