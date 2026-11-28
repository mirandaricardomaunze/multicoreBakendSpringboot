package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;

/**
 * The only personal data a webhook carries (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1): the customer's name and NUIT.
 * The walk-in customer (Consumidor Final, NUIT {@code 000000000}) goes with the name stored on the document and
 * {@code nuit = null}.
 */
record WebhookCustomer(String name, String nuit) {

    /**
     * @param documentName name stored on the document (walk-in contact), preferred when present
     * @param clientName   name of the registered client
     * @param taxId        NUIT of the registered client
     */
    static WebhookCustomer of(String documentName, String clientName, String taxId) {
        String nuit = blank(taxId) || WalkInClientProvider.WALK_IN_TAX_ID.equals(taxId.trim()) ? null : taxId.trim();
        String name = !blank(documentName) ? documentName.trim() : blank(clientName) ? null : clientName.trim();
        return new WebhookCustomer(name, nuit);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
