package mz.multicore.erp.gui.monitoring;

import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.monitoring.dto.SystemAlertTestResultDTO;
import mz.multicore.erp.modules.monitoring.dto.SystemDiagnosticsExportDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;

/**
 * Operações de exportação de diagnósticos, cópia para área de transferência e prova de e-mail de alerta.
 */
public final class SystemMonitoringExporter {

    private SystemMonitoringExporter() {
    }

    public static void copyToClipboard(Window dialog, SystemDiagnosticsExportDTO lastDiagnosticsExport) {
        if (lastDiagnosticsExport == null || lastDiagnosticsExport.reportFormattedText() == null) {
            ToastManager.show(dialog, FeedbackType.WARNING, "Nenhum relatório de diagnóstico carregado ainda.");
            return;
        }
        try {
            StringSelection sel = new StringSelection(lastDiagnosticsExport.reportFormattedText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            ToastManager.success(dialog, "Relatório de diagnóstico copiado para a área de transferência.");
        } catch (Exception ex) {
            ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao copiar: " + ex.getMessage());
        }
    }

    public static void exportToFile(Window dialog, SystemDiagnosticsExportDTO lastDiagnosticsExport) {
        if (lastDiagnosticsExport == null || lastDiagnosticsExport.reportFormattedText() == null) {
            ToastManager.show(dialog, FeedbackType.WARNING, "Nenhum relatório de diagnóstico carregado ainda.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("diagnostico-multicore-" + System.currentTimeMillis() + ".txt"));
        int opt = chooser.showSaveDialog(dialog);
        if (opt == JFileChooser.APPROVE_OPTION) {
            try {
                File target = chooser.getSelectedFile();
                Files.writeString(target.toPath(), lastDiagnosticsExport.reportFormattedText());
                ToastManager.success(dialog, "Relatório exportado com sucesso para: " + target.getAbsolutePath());
            } catch (Exception ex) {
                ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao gravar ficheiro: " + ex.getMessage());
            }
        }
    }

    public static void handleTestEmail(Window dialog, SystemMonitoringApiClient monitoringClient) {
        String email = ModernMessageDialog.prompt(dialog, FeedbackType.INFO, "Teste de Alerta por E-mail",
                "Endereço de e-mail para receber a prova de alerta:", "admin@multicore.co.mz", false);
        if (email == null || email.isBlank()) return;

        SwingWorker<SystemAlertTestResultDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected SystemAlertTestResultDTO doInBackground() {
                return monitoringClient.testEmailAlert(email.trim());
            }

            @Override
            protected void done() {
                try {
                    SystemAlertTestResultDTO res = get();
                    if (res != null && res.success()) {
                        ToastManager.success(dialog, res.message());
                    } else {
                        ToastManager.show(dialog, FeedbackType.WARNING,
                                res != null ? res.message() : "Falha ao enviar alerta de teste.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao disparar teste: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
