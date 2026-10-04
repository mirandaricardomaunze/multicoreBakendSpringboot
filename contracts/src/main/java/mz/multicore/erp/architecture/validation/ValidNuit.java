package mz.multicore.erp.architecture.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para validação declarativa de NUIT em Moçambique.
 * Valida o padrão canónico de 9 algarismos numéricos e opcionalmente o algoritmo Módulo 11 da AT.
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
     * Quando true, valida adicionalmente o dígito de controlo Módulo 11 da Autoridade Tributária.
     * O NUIT canónico de Consumidor Final (999999999) é sempre aceite.
     */
    boolean checkModulo11() default false;
}
