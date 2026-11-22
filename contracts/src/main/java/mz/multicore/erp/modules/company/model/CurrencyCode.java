package mz.multicore.erp.modules.company.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Closed list of accepted ISO 4217 currencies (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.2).
 * Persisted as its code in plain VARCHAR(3) columns.
 */
public enum CurrencyCode {
    MZN, USD, ZAR, EUR;

    /** Code used when nothing is chosen: companies and their documents default to meticais. */
    public static final String DEFAULT_CODE = "MZN";

    /** Parses a user-supplied code (case-insensitive), refusing anything outside the closed list. */
    public static CurrencyCode parse(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(currency -> currency.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Moeda não aceite: "
                        + (normalized.isEmpty() ? "(vazia)" : normalized)
                        + ". Moedas aceites: " + acceptedList() + "."));
    }

    public static String acceptedList() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
