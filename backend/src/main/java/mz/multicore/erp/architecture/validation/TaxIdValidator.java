package mz.multicore.erp.architecture.validation;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.validation.ValidationPatterns;

/**
 * Single source of truth for NUIT/NIF validation on the backend.
 * Rule: exactly 9 numeric digits as defined in ValidationPatterns.
 */
public final class TaxIdValidator {

    private TaxIdValidator() {}

    public static void validate(String taxId) {
        if (taxId == null || !ValidationPatterns.NUIT_PATTERN.matcher(taxId.trim()).matches()) {
            throw new BusinessRuleException("NUIT/NIF inválido. Deve conter exatamente 9 algarismos.");
        }
    }
}
