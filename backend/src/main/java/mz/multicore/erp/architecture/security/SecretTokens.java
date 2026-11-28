package mz.multicore.erp.architecture.security;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Random secrets shown once to the ADMIN — public API keys ({@code mk_live_}) and webhook secrets ({@code whsec_}),
 * docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1 and §4.1.1: 32 random bytes in base62, always 43 characters.
 */
public final class SecretTokens {

    private static final int RANDOM_BYTES = 32;
    /** 62^43 is greater than 2^256, so 43 base62 digits hold any 32-byte value. */
    private static final int ENCODED_LENGTH = 43;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final BigInteger RADIX = BigInteger.valueOf(BASE62.length());
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecretTokens() {
    }

    /** {@code prefix} followed by 32 random bytes encoded in base62. */
    public static String generate(String prefix) {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return prefix + base62(new BigInteger(1, bytes));
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
