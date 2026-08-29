package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifra em repouso para anexos guardados na base de dados — hoje, os comprovativos dos exames de
 * saúde ocupacional. Ver docs/CONFORMIDADE_LEGAL_MZ_SPEC.md §5.
 *
 * <p><b>Porque é que isto existe.</b> Um comprovativo de exame médico é um documento clínico de uma
 * pessoa identificada. Guardado em claro, está legível para quem tenha a base de dados <i>ou um
 * backup</i> — e os backups saem da máquina. A base de dados protege o acesso pela aplicação; não
 * protege o ficheiro que alguém copia.
 *
 * <p><b>Três decisões que fazem isto ser seguro de instalar numa loja já a funcionar:</b>
 *
 * <ol>
 *   <li><b>Os dados dizem o que são.</b> O que é cifrado leva o prefixo {@code MCE1}. Ler um blob
 *       sem esse prefixo devolve-o tal e qual: os anexos gravados antes desta alteração continuam a
 *       abrir, sem migração e sem janela em que ninguém consegue ler nada.
 *   <li><b>Sem chave configurada, não cifra.</b> Uma instalação sem {@code security.attachment-key}
 *       comporta-se como antes. Falhar o arranque por falta de chave transformaria uma melhoria de
 *       segurança numa paragem de serviço.
 *   <li><b>Mas nunca falha em silêncio.</b> Se o blob está cifrado e a chave falta ou está errada, a
 *       leitura é <b>recusada com a razão</b>. Devolver bytes ilegíveis a fingir que são o ficheiro
 *       é como um anexo se perde sem ninguém dar por isso.
 * </ol>
 *
 * <p>AES-256-GCM: cifra e autentica na mesma operação, pelo que um anexo adulterado na base de
 * dados é detectado em vez de descodificado. O IV é aleatório por anexo e viaja com ele — reutilizar
 * um IV em GCM é a única forma de partir este esquema, e assim não há onde o reutilizar.
 */
@Component
public class AttachmentCrypto {

    /** Marca de formato. Quatro bytes que dizem "isto está cifrado, versão 1". */
    static final byte[] MAGIC = {'M', 'C', 'E', '1'};
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AttachmentCrypto(@Value("${security.attachment-key:}") String base64Key) {
        this.key = parseKey(base64Key);
    }

    public boolean isEnabled() {
        return key != null;
    }

    /** Cifra para guardar. Sem chave configurada, devolve o conteúdo tal e qual. */
    public byte[] protect(byte[] plain) {
        if (plain == null || plain.length == 0 || key == null) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] sealed = cipher.doFinal(plain);

            byte[] out = new byte[MAGIC.length + IV_BYTES + sealed.length];
            System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
            System.arraycopy(iv, 0, out, MAGIC.length, IV_BYTES);
            System.arraycopy(sealed, 0, out, MAGIC.length + IV_BYTES, sealed.length);
            return out;
        } catch (Exception ex) {
            throw new BusinessRuleException("Não foi possível cifrar o anexo: " + ex.getMessage());
        }
    }

    /** Decifra o que estiver cifrado. O que foi gravado em claro antes desta alteração passa igual. */
    public byte[] reveal(byte[] stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        if (key == null) {
            throw new BusinessRuleException(
                    "Este anexo está cifrado e o servidor não tem chave configurada "
                            + "(security.attachment-key). Reponha a chave para o poder abrir.");
        }
        try {
            byte[] iv = Arrays.copyOfRange(stored, MAGIC.length, MAGIC.length + IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(stored, MAGIC.length + IV_BYTES, stored.length - MAGIC.length - IV_BYTES);
        } catch (Exception ex) {
            throw new BusinessRuleException(
                    "Não foi possível decifrar o anexo: a chave não corresponde ou o conteúdo foi "
                            + "alterado na base de dados.");
        }
    }

    static boolean isEncrypted(byte[] stored) {
        if (stored == null || stored.length < MAGIC.length + IV_BYTES) {
            return false;
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (stored[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static SecretKey parseKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "security.attachment-key não é Base64 válido. Gere uma chave de 32 bytes: "
                            + "openssl rand -base64 32");
        }
        if (raw.length != 32) {
            throw new IllegalStateException(String.format(
                    "security.attachment-key tem de ter 32 bytes (AES-256); tem %d. "
                            + "Gere-a com: openssl rand -base64 32", raw.length));
        }
        return new SecretKeySpec(raw, "AES");
    }
}
