package mz.multicore.erp.modules.publicapi.service;

/** A public API resource that does not exist in the key's company → 404 with a stable {@code code}. */
public class PublicApiNotFoundException extends PublicApiException {

    public PublicApiNotFoundException(String code, String message) {
        super(404, code, message);
    }
}
