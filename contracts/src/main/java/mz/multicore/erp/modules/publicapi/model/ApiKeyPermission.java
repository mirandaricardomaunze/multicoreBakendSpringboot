package mz.multicore.erp.modules.publicapi.model;

/** What a public API key may do (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1, §3.3). */
public enum ApiKeyPermission {
    CATALOG_READ,
    ORDERS_READ,
    ORDERS_WRITE
}
