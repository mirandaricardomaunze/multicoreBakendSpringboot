package mz.multicore.erp.gui;

import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupFileLabelsTest {

    @Test
    void distingueBackupFisicoDoLogico() {
        assertEquals("BACKUP FÍSICO", BackupFileLabels.status("multicore_x.dump.enc"));
        assertEquals("BACKUP LÓGICO", BackupFileLabels.status("backup_1.json.enc"));
    }

    @Test
    void resumoDoBackupFisicoNaoMostraEmpresaVazia() {
        var physical = new BackupVerificationDTO("m.dump.enc", null, "2026-11-03 18:29:02 UTC", 2,
                Map.of("tabelas", 2, "indices", 1, "sequencias", 1));
        assertEquals("Backup físico legível, criado em 2026-11-03 18:29:02 UTC: 2 tabelas, 1 índices, 1 sequências.",
                BackupFileLabels.verificationSummary(physical));
    }
}
