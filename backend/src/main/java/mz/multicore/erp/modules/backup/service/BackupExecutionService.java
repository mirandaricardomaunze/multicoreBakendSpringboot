package mz.multicore.erp.modules.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Gestor de persistência e gravação física de ficheiros de cópia de segurança em disco.
 */
@Service
public class BackupExecutionService {

    private final String backupDirectory;
    private final BackupFileCipher cipher;

    public BackupExecutionService() {
        this("backups", new BackupFileCipher(""));
    }

    @Autowired
    public BackupExecutionService(@Value("${backup.dir:backups}") String backupDirectory, BackupFileCipher cipher) {
        this.backupDirectory = backupDirectory;
        this.cipher = cipher;
    }

    public String writeBackupFile(Long companyId, Map<String, Object> data) {
        try {
            File backupDir = new File(backupDirectory);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backupFile = new File(backupDir, "company_" + companyId + "_backup_" + timestamp + ".json");

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(backupFile, data);

            return seal(backupFile).getAbsolutePath();
        } catch (IOException | GeneralSecurityException e) {
            throw new BusinessRuleException("Falha ao criar backup de segurança: " + e.getMessage());
        }
    }

    /** Se falhar, a versão em claro fica para {@link BackupPlainFileSealer} a cifrar mais tarde. */
    private File seal(File plain) throws IOException, GeneralSecurityException {
        if (!cipher.isEnabled()) return plain;
        File encrypted = new File(plain.getPath() + BackupFileCipher.ENCRYPTED_SUFFIX);
        cipher.encrypt(plain.toPath(), encrypted.toPath());
        cipher.verify(encrypted.toPath());
        plain.delete();
        return encrypted;
    }
}
