package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.SecretTokens;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Webhook secrets (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1.1): {@code whsec_} + 32 random bytes in base62, stored
 * encrypted at rest with {@link AttachmentCrypto} (AES-256-GCM, {@code MCE1}) as Base64 text. Without a configured
 * key outside prod the bytes stay in clear, as attachments do.
 */
final class WebhookSecrets {

    static final String PREFIX = "whsec_";

    private WebhookSecrets() {
    }

    static String generate() {
        return SecretTokens.generate(PREFIX);
    }

    static String seal(AttachmentCrypto crypto, String secret) {
        return Base64.getEncoder().encodeToString(crypto.protect(secret.getBytes(StandardCharsets.UTF_8)));
    }

    /** Clear secret for the HMAC signature of each delivery (WH-04). */
    static String open(AttachmentCrypto crypto, String stored) {
        return new String(crypto.reveal(Base64.getDecoder().decode(stored)), StandardCharsets.UTF_8);
    }
}
