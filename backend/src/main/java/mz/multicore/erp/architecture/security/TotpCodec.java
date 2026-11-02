package mz.multicore.erp.architecture.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;

/** Códigos de 6 dígitos (RFC 6238), compatíveis com Google Authenticator e semelhantes. */
public final class TotpCodec {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int STEP_SECONDS = 30;

    private TotpCodec() {}

    public static String newSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xFF);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        return out.toString();
    }

    public static String otpauthUri(String username, String secret) {
        String account = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return "otpauth://totp/Multicore:" + account + "?secret=" + secret + "&issuer=Multicore";
    }

    /** Aceita o intervalo actual e o vizinho de cada lado, para tolerar relógios ligeiramente diferentes. */
    public static boolean verify(String secret, String code, Instant now) {
        return matchingStep(secret, code, now).isPresent();
    }

    /** Devolve o intervalo do código, para que o servidor nunca aceite o mesmo intervalo duas vezes. */
    public static OptionalLong matchingStep(String secret, String code, Instant now) {
        if (secret == null || code == null || !code.trim().matches("\\d{6}")) return OptionalLong.empty();
        long step = now.getEpochSecond() / STEP_SECONDS;
        byte[] given = code.trim().getBytes(StandardCharsets.US_ASCII);
        OptionalLong match = OptionalLong.empty();
        for (long candidate = step - 1; candidate <= step + 1; candidate++) {
            if (MessageDigest.isEqual(code(secret, candidate).getBytes(StandardCharsets.US_ASCII), given)) {
                match = OptionalLong.of(candidate);
            }
        }
        return match;
    }

    static String code(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int value = (ByteBuffer.wrap(hash, offset, 4).getInt() & 0x7FFFFFFF) % 1_000_000;
            return String.format("%06d", value);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA1 indisponível.", ex);
        }
    }

    private static byte[] decode(String secret) {
        ByteBuffer out = ByteBuffer.allocate(secret.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char letter : secret.toCharArray()) {
            buffer = (buffer << 5) | BASE32.indexOf(letter);
            bits += 5;
            if (bits >= 8) {
                out.put((byte) (buffer >> (bits - 8)));
                bits -= 8;
            }
        }
        return out.array();
    }
}
