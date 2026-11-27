package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/**
 * A refusal of the public API with its HTTP status and a stable English {@code code}; the message is PT-MZ
 * (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2).
 */
public class PublicApiException extends BusinessRuleException {

    private final int status;
    private final String code;

    public PublicApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** 400: the request is incomplete for the rule it triggers (e.g. {@code CUSTOMER_NAME_REQUIRED}). */
    public static PublicApiException badRequest(String code, String message) {
        return new PublicApiException(400, code, message);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
