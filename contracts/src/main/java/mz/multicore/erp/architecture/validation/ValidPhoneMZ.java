package mz.multicore.erp.architecture.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para validação declarativa de número de telefone em Moçambique.
 * Suporta redes móveis (82/83, 84/85, 86/87) e fixas (21-28), com ou sem indicativo +258.
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
