package mz.multicore.erp.architecture.exception;

/**
 * Body of every public API error (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2.1, API-06): a stable English {@code code}
 * and a PT-MZ {@code message}, nothing else — no stack trace, class name or SQL.
 */
public record PublicApiError(String code, String message) {

    public PublicApiError {
        message = message == null ? "" : message;
    }
}
