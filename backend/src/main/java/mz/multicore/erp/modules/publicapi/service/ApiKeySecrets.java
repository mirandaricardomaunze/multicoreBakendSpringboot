package mz.multicore.erp.modules.publicapi.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generation and hashing of public API key secrets (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1):
 * {@code mk_live_} + 32 random bytes in base62 (always 43 characters), hashed with SHA-256.
 */
final class ApiKeySecrets {

    static final String PREFIX = "mk_live_";
    private static final int RANDOM_BYTES = 32;
    /** 62^43 is greater than 2^256, so 43 base62 digits hold any 32-byte value. */
    private static final int ENCODED_LENGTH = 43;
    private static final int IDENTIFIER_LENGTH = 8;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final BigInteger RADIX = BigInteger.valueOf(BASE62.length());
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeySecrets() {
    }

    static String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return PREFIX + base62(new BigInteger(1, bytes));
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

    private static String base62(BigInteger value) {
        char[] digits = new char[ENCODED_LENGTH];
        BigInteger remaining = value;
        for (int i = ENCODED_LENGTH - 1; i >= 0; i--) {
            BigInteger[] division = remaining.divideAndRemainder(RADIX);
            digits[i] = BASE62.charAt(division[1].intValue());
            remaining = division[0];
        }
        return new String(digits);
    }
}
