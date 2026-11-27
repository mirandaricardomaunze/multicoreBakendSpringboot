package mz.multicore.erp.modules.webhooks.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * {@code X-Multicore-Signature} value (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.1, WH-04): {@code sha256=} + the
 * lower-case hex HMAC-SHA256 of the body bytes, keyed with the full {@code whsec_} secret in UTF-8.
 */
final class WebhookSignature {

    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSignature() {
    }

    static String sign(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 indisponível.", ex);
        }
    }
}
