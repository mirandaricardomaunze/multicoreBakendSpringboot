package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Locale;

/** Cifra os backups que ficaram em claro: os anteriores à cifra e os de uma cifra que falhou. */
@Component
public class BackupPlainFileSealer {

    private static final Logger log = LoggerFactory.getLogger(BackupPlainFileSealer.class);
    private static final long QUIET_MS = 60L * 60 * 1000;

    private final BackupFileCipher cipher;
    private final AuditLogService auditLogService;
    private final String backupDir;

    public BackupPlainFileSealer(BackupFileCipher cipher, AuditLogService auditLogService,
                                 @Value("${backup.dir:backups}") String backupDir) {
        this.cipher = cipher;
        this.auditLogService = auditLogService;
        this.backupDir = backupDir;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    public void sealOnSchedule() {
        sealPlainFiles(System.currentTimeMillis());
    }

    /** Um ficheiro alterado há menos de uma hora pode ainda estar a ser escrito. */
    int sealPlainFiles(long nowMs) {
        File[] files = new File(backupDir).listFiles();
        if (!cipher.isEnabled() || files == null) return 0;
        int sealed = 0;
        for (File file : files) {
            if (!file.isFile() || !isPlainBackup(file.getName()) || nowMs - file.lastModified() < QUIET_MS) continue;
            try {
                seal(file);
                sealed++;
            } catch (IOException | GeneralSecurityException | RuntimeException ex) {
                log.error("Não foi possível cifrar o backup {}: {}", file.getName(), ex.getMessage());
            }
        }
        if (sealed > 0) {
            auditLogService.logEvent(null, null, "BACKUP_PLAIN_SEALED", sealed + " backup(s) em claro cifrado(s).");
        }
        return sealed;
    }

    /** A cópia cifrada guarda a data original, para a retenção não lhe dar um novo prazo. */
    private void seal(File plain) throws IOException, GeneralSecurityException {
        File encrypted = new File(plain.getPath() + BackupFileCipher.ENCRYPTED_SUFFIX);
        if (!opens(encrypted.toPath())) {
            cipher.encrypt(plain.toPath(), encrypted.toPath());
            cipher.verify(encrypted.toPath());
        }
        encrypted.setLastModified(plain.lastModified());
        if (!plain.delete()) throw new IOException("a cópia em claro não pôde ser apagada");
    }

    private boolean opens(Path encrypted) {
        try {
            cipher.verify(encrypted);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isPlainBackup(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".dump") || lower.endsWith(".json");
    }
}
