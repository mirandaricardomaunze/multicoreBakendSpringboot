package com.phcpro.architecture.validation;

import com.phcpro.architecture.exception.BusinessRuleException;

/**
 * Single source of truth for NUIT/NIF validation.
 * Rule: exactly 9 numeric digits.
 */
public final class TaxIdValidator {

    private static final String NUIT_PATTERN = "\\d{9}";

    private TaxIdValidator() {}

    public static void validate(String taxId) {
        if (taxId == null || !taxId.matches(NUIT_PATTERN)) {
            throw new BusinessRuleException("NUIT/NIF inválido. Deve conter exatamente 9 algarismos.");
        }
    }
}
