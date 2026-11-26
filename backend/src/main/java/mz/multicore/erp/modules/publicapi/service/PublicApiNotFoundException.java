package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/** A public API resource that does not exist in the key's company → 404 with a stable {@code code}. */
public class PublicApiNotFoundException extends BusinessRuleException {

    private final String code;

    public PublicApiNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
