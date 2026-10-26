package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Coordenador de operações assíncronas, emissão em PDF e Quick Peek da Auditoria Forense. */
public final class ForensicAuditOperationsCoordinator {

    private ForensicAuditOperationsCoordinator() {}

    public static void emitForensicPdf(Component parent, ForensicAuditApiClient apiClient, JProgressBar busyBar,
                                      LocalDate start, LocalDate end, ForensicSeverity sev, ForensicCategory cat, String op) {
        if (busyBar != null) {
            busyBar.setVisible(true);
            busyBar.setIndeterminate(true);
        }

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return apiClient.getPdf(start, end, sev, cat, op);
            }

            @Override
            protected void done() {
                if (busyBar != null) {
                    busyBar.setIndeterminate(false);
                    busyBar.setVisible(false);
                }
                try {
                    byte[] pdf = get();
                    if (pdf != null && pdf.length > 0) {
                        PrintPreviewDialog.show(parent, pdf, "dossie-auditoria-forense");
                    } else {
                        ToastManager.show(parent, FeedbackType.WARNING, "Dossiê PDF vazio devolvido pelo servidor.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar PDF forense: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel tableModel, int row, Consumer<Integer> onOpenFull) {
        if (tableModel == null || row < 0 || row >= tableModel.getRowCount()) return;
        peek.setHeaderIcon("fas-shield-alt", UIHelper.REJECTED_RED);
        peek.setTitle(String.valueOf(tableModel.getValueAt(row, 2)));
        peek.setSubtitle("Ref: " + tableModel.getValueAt(row, 3) + " • " + tableModel.getValueAt(row, 0));
        peek.setStatus(String.valueOf(tableModel.getValueAt(row, 1)), UIHelper.REJECTED_RED);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Operador", String.valueOf(tableModel.getValueAt(row, 4)), false));
        items.add(new QuickPeekPanel.PeekItem("Impacto Financeiro", String.valueOf(tableModel.getValueAt(row, 5)), true));
        items.add(new QuickPeekPanel.PeekItem("Detalhes da Ocorrência", String.valueOf(tableModel.getValueAt(row, 6)), false));
        items.add(new QuickPeekPanel.PeekItem("Recomendação Preventiva", String.valueOf(tableModel.getValueAt(row, 7)), false));
        peek.setItems(items);
        peek.getOpenFullButton().setText("Ver Ficha Completa");
        peek.setOnOpenFullAction(v -> {
            if (onOpenFull != null) onOpenFull.accept(row);
        });
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static ForensicSeverity parseSeverity(int idx) {
        if (idx == 1) return ForensicSeverity.CRITICAL;
        if (idx == 2) return ForensicSeverity.SUSPICIOUS;
        if (idx == 3) return ForensicSeverity.INFO;
        return null;
    }

    public static ForensicCategory parseCategory(int idx) {
        return switch (idx) {
            case 1 -> ForensicCategory.DOC_CANCELLATION;
            case 2 -> ForensicCategory.EXCESSIVE_DISCOUNT;
            case 3 -> ForensicCategory.STOCK_SHRINKAGE;
            case 4 -> ForensicCategory.CREDIT_OVERRIDE;
            case 5 -> ForensicCategory.PAYMENT_VOID;
            case 6 -> ForensicCategory.AUDIT_SECURITY;
            default -> null;
        };
    }
}
