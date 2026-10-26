package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilitário de exportação, serialização JSON e partilha da ficha de detalhes de registos.
 */
public final class RecordDetailsExporter {

    private RecordDetailsExporter() {}

    public static JComponent buildSharePreviewTab(String title, List<RecordDetailsDialog.PropertyItem> props) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel info = new JLabel("Formato de texto tabulado para conferência, partilha ou envio oficial:");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(UIHelper.TEXT_MUTED);
        panel.add(info, BorderLayout.NORTH);

        String summary = buildExecutiveSummary(title, props);
        JTextArea area = new JTextArea(summary);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setForeground(UIHelper.TEXT_LIGHT);
        area.setBackground(UIHelper.BG_CARD);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public static String buildExecutiveSummary(String title, List<RecordDetailsDialog.PropertyItem> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("MULTICORE ERP | FICHA CORPORATIVA DE REGISTO\n");
        sb.append("Identificador: ").append(title).append("\n");
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        sb.append("Data da Consulta: ").append(nowStr).append("\n");
        String username = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        sb.append("Operador: ").append(username).append(" | Empresa: ").append(companyId != null ? "#" + companyId : "Padrão").append("\n");
        sb.append("====================================================\n");
        for (RecordDetailsDialog.PropertyItem item : properties) {
            sb.append(String.format("%-28s : %s\n", item.label(), item.value()));
        }
        sb.append("====================================================\n");
        sb.append("Documento emitido para conferência oficial interna.\n");
        sb.append("Multicore Enterprise ERP • Sistema Certificado\n");
        return sb.toString();
    }

    public static String buildJson(List<RecordDetailsDialog.PropertyItem> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (int i = 0; i < properties.size(); i++) {
            RecordDetailsDialog.PropertyItem p = properties.get(i);
            sb.append("  \"").append(escapeJson(p.label())).append("\": \"").append(escapeJson(p.value())).append("\"");
            if (i < properties.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static void copyToClipboard(String content) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
        } catch (Exception ignored) {}
    }

    public static void exportToFile(Component parent, String content, String defaultFileName) {
        if (GraphicsEnvironment.isHeadless()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Registo");
        chooser.setSelectedFile(new File(defaultFileName));
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
                ToastManager.success(parent, "Ficheiro exportado para " + file.getName());
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gravar: " + ex.getMessage());
            }
        }
    }

    public static String sanitizeFileName(String name) {
        if (name == null) return "registo";
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static void setupFooterActions(ExecutiveDetailDialog dialog, JTable table, int row, String finalTitle, List<RecordDetailsDialog.PropertyItem> properties) {
        ModernButton copySummaryBtn = new ModernButton("Copiar Ficha", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        copySummaryBtn.setIcon(UIHelper.icon("fas-copy", 14, Color.WHITE));
        copySummaryBtn.setToolTipText("Copiar ficha completa tabulada para WhatsApp, E-mail ou Relatório");
        copySummaryBtn.addActionListener(e -> {
            copyToClipboard(buildExecutiveSummary(finalTitle, properties));
            ToastManager.success(dialog.getDialog(), "Ficha de registo copiada com sucesso.");
        });
        dialog.addLeftAction(copySummaryBtn);

        ModernButton copyJsonBtn = UIHelper.createSecondaryButton("Copiar JSON");
        copyJsonBtn.setIcon(UIHelper.icon("fas-code", 13));
        copyJsonBtn.setToolTipText("Copiar atributos estruturados em formato JSON");
        copyJsonBtn.addActionListener(e -> {
            copyToClipboard(buildJson(properties));
            ToastManager.success(dialog.getDialog(), "JSON copiado para a área de transferência.");
        });
        dialog.addLeftAction(copyJsonBtn);

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar TXT");
        exportBtn.setIcon(UIHelper.icon("fas-file-download", 13));
        exportBtn.setToolTipText("Gravar resumo em ficheiro de texto");
        exportBtn.addActionListener(e -> {
            String summary = buildExecutiveSummary(finalTitle, properties);
            exportToFile(dialog.getDialog(), summary, "registo-" + sanitizeFileName(finalTitle) + ".txt");
        });
        dialog.addLeftAction(exportBtn);
    }
}
