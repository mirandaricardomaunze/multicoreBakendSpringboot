package mz.multicore.erp.architecture.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para validação declarativa de Bilhete de Identidade (B.I.) em Moçambique.
 * Formato padrão: 12 algarismos numéricos seguidos de 1 letra maiúscula de controlo.
 */
@Documented
@Constraint(validatedBy = ValidBiMZValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBiMZ {
    String message() default "Bilhete de Identidade moçambicano inválido (12 dígitos numéricos seguidos de 1 letra maiúscula).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
