package mz.multicore.erp.modules.currency.dto;

/**
 * Base currency of the active company (SPEC §3.1) and whether it can still change: {@code editable} is false
 * once the company has any purchase, invoice or stock movement, so the settings screen shows it read-only.
 */
public record BaseCurrencyDTO(String currency, boolean editable) {
}
