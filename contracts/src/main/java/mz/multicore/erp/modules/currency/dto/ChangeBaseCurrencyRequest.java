package mz.multicore.erp.modules.currency.dto;

/** New base currency of the active company; refused once the company has movements (SPEC §3.1). */
public record ChangeBaseCurrencyRequest(String currency) {
}
