package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** Passos de cifra à volta do pg_dump e do pg_restore. Sem chave, o backup fica como foi gerado. */
@Component
public class BackupEncryption {

    private final BackupFileCipher cipher;
    private final PostgresBackupCommandBuilder commandBuilder;
    private final PostgresBackupProcessRunner processRunner;

    public BackupEncryption(BackupFileCipher cipher, PostgresBackupCommandBuilder commandBuilder,
                            PostgresBackupProcessRunner processRunner) {
        this.cipher = cipher;
        this.commandBuilder = commandBuilder;
        this.processRunner = processRunner;
    }

    /**
     * Antes de apagar a versão em claro, prova que o dump é legível e que a cópia cifrada abre.
     * Se algo falhar, a versão em claro fica: {@link BackupPlainFileSealer} cifra-a mais tarde.
     */
    File seal(File plain) {
        if (!cipher.isEnabled()) return plain;
        File encrypted = new File(plain.getPath() + BackupFileCipher.ENCRYPTED_SUFFIX);
        try {
            processRunner.runProcess(commandBuilder.buildListCommand(plain.getAbsolutePath()), "pg_restore");
            cipher.encrypt(plain.toPath(), encrypted.toPath());
            cipher.verify(encrypted.toPath());
        } catch (IOException | GeneralSecurityException ex) {
            encrypted.delete();
            throw new BusinessRuleException("Não foi possível cifrar o backup: " + ex.getMessage());
        } catch (RuntimeException ex) {
            encrypted.delete();
            throw ex;
        }
        plain.delete();
        return encrypted;
    }

    static File requireRestorableFile(String path, boolean confirmOverwrite) {
        if (!confirmOverwrite) {
            throw new BusinessRuleException("Restaurar substitui os dados actuais. Confirme a operação para continuar.");
        }
        if (path == null || path.isBlank()) {
            throw new BusinessRuleException("Selecione um ficheiro de backup para restaurar.");
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessRuleException("Ficheiro de backup não encontrado.");
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".dump") && !name.endsWith(".dump" + BackupFileCipher.ENCRYPTED_SUFFIX)) {
            throw new BusinessRuleException("O ficheiro selecionado não é um backup físico (.dump) válido.");
        }
        return file;
    }

    /** O dump decifrado existe só durante o restauro. */
    void withDecrypted(File encrypted, Consumer<File> restore) {
        File plain = new File(encrypted.getParentFile(), "restore-" + UUID.randomUUID() + ".dump");
        try {
            cipher.decrypt(encrypted.toPath(), plain.toPath());
            restore.accept(plain);
        } finally {
            plain.delete();
        }
    }
}
