package mz.multicore.erp.modules.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Gestor de persistência e gravação física de ficheiros de cópia de segurança em disco.
 */
@Service
public class BackupExecutionService {

    public String writeBackupFile(Long companyId, Map<String, Object> data) {
        try {
            File backupDir = new File("backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backupFile = new File(backupDir, "company_" + companyId + "_backup_" + timestamp + ".json");

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(backupFile, data);

            return backupFile.getAbsolutePath();
        } catch (IOException e) {
            throw new BusinessRuleException("Falha ao criar backup de segurança: " + e.getMessage());
        }
    }
}
