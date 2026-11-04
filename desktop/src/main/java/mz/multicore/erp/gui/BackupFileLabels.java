package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;

import java.awt.Color;
import java.util.Locale;
import java.util.Map;

/** Textos do arquivo de backups: distingue o backup físico (base inteira) do lógico (uma empresa). */
final class BackupFileLabels {

    private BackupFileLabels() {
    }

    static boolean isPhysical(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        return name.endsWith(".dump") || name.endsWith(".dump.enc");
    }

    static String status(String fileName) {
        return isPhysical(fileName) ? "BACKUP FÍSICO" : "BACKUP LÓGICO";
    }

    static Color statusColor(String fileName) {
        return isPhysical(fileName) ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE;
    }

    static String verificationSummary(BackupVerificationDTO verification) {
        if (verification.companyId() == null) {
            Map<String, Integer> counts = verification.itemCounts();
            return "Backup físico legível, criado em " + verification.generatedAt() + ": "
                    + counts.getOrDefault("tabelas", 0) + " tabelas, "
                    + counts.getOrDefault("indices", 0) + " índices, "
                    + counts.getOrDefault("sequencias", 0) + " sequências.";
        }
        return "Backup lógico válido da empresa " + verification.companyId()
                + ", gerado em " + verification.generatedAt() + ".";
    }
}
