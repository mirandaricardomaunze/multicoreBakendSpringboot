package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.backup.dto.BackupStatusDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;

/**
 * Operações assíncronas de execução, auditoria e verificação de cópias de segurança.
 */
public final class ConfigBackupOperations {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ConfigBackupOperations() {}

    public static void appendLog(JTextArea logArea, String msg) {
        if (logArea != null) logArea.append(msg + "\n");
    }

    public static void applyAutoBackupStatus(JLabel statusLabel, BackupStatusDTO st) {
        if (statusLabel == null) return;
        String base = st.autoEnabled() ? "Backup automático: ACTIVO (diário)" : "Backup automático: desativado";
        if (st.lastTime() == null) {
            statusLabel.setText(base + " — ainda sem execução nesta sessão.");
            statusLabel.setForeground(UIHelper.TEXT_MUTED);
            return;
        }
        boolean ok = Boolean.TRUE.equals(st.lastSuccess());
        statusLabel.setText(base + " — última: " + st.lastTime().format(DATE_TIME) + (ok ? "  [OK]" : "  [FALHOU]"));
        statusLabel.setForeground(ok ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
    }

    public static void refreshAutoBackupStatus(ConfigPanel owner, JLabel statusLabel) {
        if (statusLabel == null) return;
        UIHelper.loadAsync(owner, owner.backupApiClient::status, st -> applyAutoBackupStatus(statusLabel, st),
                error -> owner.showConfigError("estado do backup", error));
    }

    public static void loadBackupFilesList(ConfigPanel owner, DefaultTableModel filesModel) {
        if (filesModel == null) return;
        String prefix = "company_" + CurrentUserContext.getCurrentCompanyId() + "_backup_";
        UIHelper.loadAsync(owner, owner.backupApiClient::files, files -> {
            filesModel.setRowCount(0);
            for (String name : files) {
                if (name.startsWith(prefix) && name.toLowerCase().matches(".*[.]json([.]enc)?")) {
                    filesModel.addRow(new Object[]{name, "—"});
                }
            }
        }, error -> owner.showConfigError("arquivo de backups", error));
    }

    public static void runAutoBackupNow(ConfigPanel owner, JTextArea logArea, JLabel statusLabel, DefaultTableModel filesModel) {
        if (!SignedInUser.isAdmin()) { ToastManager.show(owner, FeedbackType.WARNING, "Apenas administradores podem executar o backup."); return; }
        UIHelper.runWithProgress(owner, "A executar backup automático…", () -> owner.backupApiClient.runAuto(), res -> {
            appendLog(logArea, (res.success() ? "[OK] " : "[FALHA] ") + res.message());
            refreshAutoBackupStatus(owner, statusLabel);
            loadBackupFilesList(owner, filesModel);
        }, ex -> ToastManager.show(owner, FeedbackType.ERROR, ex.getMessage()));
    }

    public static void runManualBackup(ConfigPanel owner, JTextArea logArea, DefaultTableModel filesModel) {
        if (!SignedInUser.isAdmin()) { ToastManager.show(owner, FeedbackType.ERROR, "Apenas administradores podem iniciar cópias manuais."); return; }
        appendLog(logArea, ">> A iniciar backup manual (" + CurrentUserContext.getUsername() + ")...");
        UIHelper.runWithProgress(owner, "A efectuar cópia de segurança…", owner.backupApiClient::executeBackup, path -> {
            appendLog(logArea, ">> Backup concluído com sucesso!\n>> Destino: " + path);
            ToastManager.success(owner, "Cópia de segurança gravada com sucesso em: " + path);
            loadBackupFilesList(owner, filesModel);
            owner.loadAuditLogs();
        }, error -> {
            appendLog(logArea, ">> ERRO: " + error.getMessage());
            owner.showConfigError("backup", error);
        });
    }

    public static void runPhysicalBackup(ConfigPanel owner, JTextArea logArea) {
        if (!SignedInUser.isAdmin()) { ToastManager.show(owner, FeedbackType.ERROR, "Apenas administradores podem gerar backups físicos."); return; }
        appendLog(logArea, ">> A iniciar backup físico da base de dados (" + CurrentUserContext.getUsername() + ")...");
        UIHelper.runWithProgress(owner, "A gerar backup físico da base de dados…", owner.backupApiClient::executePhysical, result -> {
            appendLog(logArea, ">> Backup físico concluído!\n>> Destino: " + result.filePath() + "\n>> Base de dados: " + result.database() + " (" + (result.sizeBytes() / 1024) + " KB)");
            ToastManager.success(owner, "Backup físico restaurável gravado em: " + result.filePath());
            owner.loadAuditLogs();
        }, error -> {
            appendLog(logArea, ">> ERRO: " + error.getMessage());
            ToastManager.show(owner, FeedbackType.ERROR, "Erro ao gerar backup físico: " + error.getMessage());
        });
    }

    public static void verifySelectedBackup(ConfigPanel owner, JTextArea logArea, JTable table, DefaultTableModel model) {
        if (!SignedInUser.isAdmin()) { ToastManager.show(owner, FeedbackType.ERROR, "Apenas administradores podem verificar cópias."); return; }
        int row = table != null ? mz.multicore.erp.gui.components.TableFilter.selectedModelRow(table) : -1;
        if (row < 0) { ToastManager.show(owner, FeedbackType.WARNING, "Seleccione um ficheiro de backup no arquivo."); return; }
        String fileName = String.valueOf(model.getValueAt(row, 0));
        appendLog(logArea, ">> A verificar backup: " + fileName);
        UIHelper.runWithProgress(owner, "A verificar cópia de segurança…", () -> owner.backupApiClient.verify(fileName), verification -> {
            appendLog(logArea, ">> " + BackupFileLabels.verificationSummary(verification));
            ToastManager.success(owner, "Backup verificado com sucesso. Ficheiro: " + verification.fileName());
            owner.loadAuditLogs();
        }, error -> {
            appendLog(logArea, ">> ERRO DE VERIFICAÇÃO: " + error.getMessage());
            owner.showConfigError("verificação do backup", error);
        });
    }

    public static void renderQuickPeek(mz.multicore.erp.gui.components.QuickPeekPanel peek, int row,
                                       DefaultTableModel model, Runnable onVerify) {
        String nome = String.valueOf(model.getValueAt(row, 0));
        String tam = String.valueOf(model.getValueAt(row, 1));
        peek.setHeaderIcon("fas-file-archive", UIHelper.APPROVED_GREEN);
        peek.setTitle(nome);
        peek.setSubtitle("Tamanho: " + tam + " KB");
        peek.setStatus(BackupFileLabels.status(nome), BackupFileLabels.statusColor(nome));
        peek.setItems(java.util.List.of(
                new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Nome Ficheiro", nome, false),
                new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Tamanho", tam + " KB", true)
        ));
        peek.getOpenFullButton().setText("Verificar");
        peek.setOnOpenFullAction(v -> onVerify.run());
    }
}
