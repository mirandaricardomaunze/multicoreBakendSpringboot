package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifra os backups físicos com AES-256-GCM, por blocos, para ficheiros grandes não ocuparem a memória.
 * Cada bloco autentica a sua posição e se é o último, por isso trocar ou cortar blocos é detectado.
 * Usa a chave dos anexos: sem ela, o backup não se restaura, por isso tem de existir cópia fora do servidor.
 */
@Component
public class BackupFileCipher {

    static final String ENCRYPTED_SUFFIX = ".enc";
    private static final byte[] MAGIC = {'M', 'C', 'B', '1'};
    private static final int CHUNK_BYTES = 1024 * 1024;
    private static final int NONCE_BYTES = 8;
    private static final int TAG_BITS = 128;

    private final SecretKey key;

    public BackupFileCipher(@Value("${security.attachment-key:}") String base64Key) {
        this.key = base64Key == null || base64Key.isBlank()
                ? null : new SecretKeySpec(Base64.getDecoder().decode(base64Key.trim()), "AES");
    }

    public boolean isEnabled() {
        return key != null;
    }

    public void encrypt(Path plain, Path target) throws IOException, GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        new SecureRandom().nextBytes(nonce);
        try (InputStream in = Files.newInputStream(plain);
             DataOutputStream out = new DataOutputStream(Files.newOutputStream(target))) {
            out.write(MAGIC);
            out.write(nonce);
            byte[] current = in.readNBytes(CHUNK_BYTES);
            for (int index = 0; ; index++) {
                byte[] next = in.readNBytes(CHUNK_BYTES);
                boolean last = next.length == 0;
                byte[] sealed = cipher(Cipher.ENCRYPT_MODE, nonce, index, last).doFinal(current);
                out.writeInt(sealed.length);
                out.write(sealed);
                if (last) return;
                current = next;
            }
        }
    }

    public void decrypt(Path encrypted, Path target) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(encrypted));
             var out = Files.newOutputStream(target)) {
            readAll(in, out);
        } catch (IOException | GeneralSecurityException ex) {
            throw invalid();
        }
    }

    /** Para ficheiros pequenos que se lêem sem nunca passar para o disco em claro. */
    public byte[] decryptToMemory(Path encrypted) {
        var out = new java.io.ByteArrayOutputStream();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(encrypted))) {
            readAll(in, out);
            return out.toByteArray();
        } catch (IOException | GeneralSecurityException ex) {
            throw invalid();
        }
    }

    /** Confirma que todos os blocos se lêem com a chave actual, sem escrever o conteúdo em disco. */
    public void verify(Path encrypted) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(encrypted))) {
            readAll(in, java.io.OutputStream.nullOutputStream());
        } catch (IOException | GeneralSecurityException ex) {
            throw invalid();
        }
    }

    /** Compara por SHA-256 sem gravar o conteúdo decifrado nem carregar o ficheiro inteiro. */
    public boolean matchesPlain(Path encrypted, Path plain) throws IOException, GeneralSecurityException {
        MessageDigest expected = MessageDigest.getInstance("SHA-256");
        MessageDigest actual = MessageDigest.getInstance("SHA-256");
        try (InputStream source = Files.newInputStream(plain);
             DigestOutputStream sink = new DigestOutputStream(java.io.OutputStream.nullOutputStream(), expected)) {
            source.transferTo(sink);
        }
        try (DataInputStream source = new DataInputStream(Files.newInputStream(encrypted));
             DigestOutputStream sink = new DigestOutputStream(java.io.OutputStream.nullOutputStream(), actual)) {
            readAll(source, sink);
        }
        return MessageDigest.isEqual(expected.digest(), actual.digest());
    }

    private void readAll(DataInputStream in, java.io.OutputStream out) throws IOException, GeneralSecurityException {
        if (key == null) {
            throw new BusinessRuleException("Este backup está cifrado e o servidor não tem a chave (ATTACHMENT_KEY).");
        }
        if (!Arrays.equals(in.readNBytes(MAGIC.length), MAGIC)) throw invalid();
        byte[] nonce = in.readNBytes(NONCE_BYTES);
        byte[] sealed = readChunk(in);
        for (int index = 0; ; index++) {
            byte[] next = in.available() > 0 ? readChunk(in) : null;
            out.write(cipher(Cipher.DECRYPT_MODE, nonce, index, next == null).doFinal(sealed));
            if (next == null) return;
            sealed = next;
        }
    }

    private static byte[] readChunk(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > CHUNK_BYTES + TAG_BITS / 8) throw invalid();
        return in.readNBytes(length);
    }

    private Cipher cipher(int mode, byte[] nonce, int index, boolean last) throws GeneralSecurityException {
        byte[] iv = ByteBuffer.allocate(NONCE_BYTES + Integer.BYTES).put(nonce).putInt(index).array();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(new byte[]{(byte) (last ? 1 : 0)});
        return cipher;
    }

    private static BusinessRuleException invalid() {
        return new BusinessRuleException("Backup ilegível: a chave não corresponde ou o ficheiro foi alterado.");
    }
}
