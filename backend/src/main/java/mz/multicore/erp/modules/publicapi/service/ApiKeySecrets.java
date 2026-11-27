package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.security.SecretTokens;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generation and hashing of public API key secrets (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1):
 * {@code mk_live_} + 32 random bytes in base62 (always 43 characters), hashed with SHA-256.
 */
final class ApiKeySecrets {

    static final String PREFIX = "mk_live_";
    private static final int IDENTIFIER_LENGTH = 8;

    private ApiKeySecrets() {
    }

    static String generate() {
        return SecretTokens.generate(PREFIX);
    }

    /** First characters after the prefix, kept to identify the key in lists and in the audit log. */
    static String identifier(String secret) {
        return secret.substring(PREFIX.length(), PREFIX.length() + IDENTIFIER_LENGTH);
    }

    static String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }
}
