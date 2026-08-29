package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-08 do harness de conformidade: o comprovativo do exame deixa de estar em claro na base de
 * dados e nos backups.
 */
class AttachmentCryptoTest {

    private static final String KEY = Base64.getEncoder()
            .encodeToString("chave-de-32-bytes-para-aes256!!!".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_KEY = Base64.getEncoder()
            .encodeToString("outra-chave-de-32-bytes-aes256!!".getBytes(StandardCharsets.UTF_8));

    private final byte[] scan = "PDF do exame de aptidão".getBytes(StandardCharsets.UTF_8);

    @Test
    void protectThenRevealReturnsTheSameFile() {
        AttachmentCrypto crypto = new AttachmentCrypto(KEY);
        byte[] stored = crypto.protect(scan);

        assertNotEquals(-1, indexOf(stored, "MCE1".getBytes(StandardCharsets.UTF_8)),
                "o que fica gravado tem de se identificar como cifrado");
        assertEquals(-1, indexOf(stored, scan), "o conteúdo não pode ser legível no que fica gravado");
        assertArrayEquals(scan, crypto.reveal(stored));
    }

    /** Dois anexos iguais não podem produzir o mesmo blob — é o IV por anexo a fazer o seu papel. */
    @Test
    void sameFileEncryptedTwiceProducesDifferentBytes() {
        AttachmentCrypto crypto = new AttachmentCrypto(KEY);
        assertFalse(java.util.Arrays.equals(crypto.protect(scan), crypto.protect(scan)));
    }

    /**
     * A propriedade que torna isto instalável numa loja a funcionar: o que foi gravado em claro
     * antes desta alteração continua a abrir, sem migração.
     */
    @Test
    void legacyPlainAttachmentsStillOpen() {
        assertArrayEquals(scan, new AttachmentCrypto(KEY).reveal(scan));
        assertArrayEquals(scan, new AttachmentCrypto("").reveal(scan));
    }

    /** Sem chave configurada nada muda — uma instalação existente não pára por causa disto. */
    @Test
    void withoutKeyNothingIsEncrypted() {
        AttachmentCrypto crypto = new AttachmentCrypto("");
        assertFalse(crypto.isEnabled());
        assertArrayEquals(scan, crypto.protect(scan));
    }

    /** Mas nunca falha em silêncio: cifrado sem chave é recusado, não devolvido como lixo. */
    @Test
    void encryptedWithoutKeyIsRefusedLoudly() {
        byte[] stored = new AttachmentCrypto(KEY).protect(scan);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> new AttachmentCrypto("").reveal(stored));
        assertTrue(error.getMessage().contains("chave"));
    }

    @Test
    void wrongKeyIsRefusedInsteadOfReturningGarbage() {
        byte[] stored = new AttachmentCrypto(KEY).protect(scan);
        assertThrows(BusinessRuleException.class, () -> new AttachmentCrypto(OTHER_KEY).reveal(stored));
    }

    /** GCM autentica: um anexo mexido na base de dados é detectado, não descodificado. */
    @Test
    void tamperedAttachmentIsDetected() {
        AttachmentCrypto crypto = new AttachmentCrypto(KEY);
        byte[] stored = crypto.protect(scan);
        stored[stored.length - 1] ^= 0x7F;
        assertThrows(BusinessRuleException.class, () -> crypto.reveal(stored));
    }

    @Test
    void keyOfWrongSizeIsRejectedAtStartupWithInstructions() {
        String short16 = Base64.getEncoder().encodeToString("chave-curta-1234".getBytes(StandardCharsets.UTF_8));
        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> new AttachmentCrypto(short16));
        assertTrue(error.getMessage().contains("32 bytes"));
        assertTrue(error.getMessage().contains("openssl"));
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
