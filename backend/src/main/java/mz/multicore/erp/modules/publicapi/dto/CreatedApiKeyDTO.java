package mz.multicore.erp.modules.publicapi.dto;

/** Result of creating a key: the only time the full {@code secret} leaves the server (§3.1). */
public record CreatedApiKeyDTO(ApiKeyDTO key, String secret) {
}
