package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Inspector canónico executivo, só-leitura, de qualquer linha de tabela do Multicore ERP.
 * Utiliza o {@link ExecutiveDetailDialog} como motor universal de apresentação.
 */
public final class RecordDetailsDialog {
    private RecordDetailsDialog() {}

    public static void show(JTable table) {
        if (table == null || table.getSelectedRow() < 0) return;

        int selectedRow = table.getSelectedRow();
        String mainTitle = "Detalhes do Registo";
        String mainSubtitle = "Inspeção de dados da tabela (" + (selectedRow + 1) + "ª linha de " + table.getRowCount() + ")";

        // Extrai título a partir da primeira ou segunda coluna visível (ex: Código, Nome, Descrição)
        if (table.getColumnCount() > 0) {
            Object val0 = table.getValueAt(selectedRow, 0);
            if (val0 != null && !val0.toString().isBlank()) {
                mainTitle = val0.toString();
            }
            if (table.getColumnCount() > 1) {
                Object val1 = table.getValueAt(selectedRow, 1);
                if (val1 != null && !val1.toString().isBlank()) {
                    if ("Detalhes do Registo".equals(mainTitle)) {
                        mainTitle = val1.toString();
                    } else {
                        mainSubtitle = val1.toString() + " • Linha #" + (selectedRow + 1);
                    }
                }
            }
        }

        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(table, "Ficha de Detalhes — " + mainTitle);
        dialog.setTitle(mainTitle)
                .setSubtitle(mainSubtitle)
                .setHeaderIcon("fas-receipt", 28, UIHelper.ACCENT_CYAN);

        // 1. Extrai KPIs e Estado
        String statusText = null;
        ExecutiveDetailDialog.StatusSeverity severity = ExecutiveDetailDialog.StatusSeverity.INFO;
        int kpiCount = 0;

        for (int col = 0; col < table.getColumnCount(); col++) {
            if (isHidden(table, col)) continue;
            String colName = table.getColumnName(col);
            String valStr = valueText(table.getValueAt(selectedRow, col));

            if (colName == null) continue;
            String colLower = colName.toLowerCase();

            // Identifica estado/status para o Badge Pill
            if (colLower.contains("estado") || colLower.contains("status") || colLower.contains("activo") || colLower.contains("ativo")) {
                statusText = valStr;
                if (valStr.toUpperCase().contains("OK") || valStr.toUpperCase().contains("ACTIV") || valStr.toUpperCase().contains("APROV") || valStr.toUpperCase().contains("CONCLU")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.SUCCESS;
                } else if (valStr.toUpperCase().contains("PEND") || valStr.toUpperCase().contains("ALERTA") || valStr.toUpperCase().contains("SUSP")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.WARNING;
                } else if (valStr.toUpperCase().contains("CANCEL") || valStr.toUpperCase().contains("ERRO") || valStr.toUpperCase().contains("INACT") || valStr.toUpperCase().contains("BLOQ")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.DANGER;
                }
            }

            // Identifica KPIs (Valores, Totais, Saldos, Quantidades)
            if (kpiCount < 4 && (colLower.contains("total") || colLower.contains("valor") || colLower.contains("saldo") || colLower.contains("preço") || colLower.contains("preco") || colLower.contains("qtd") || colLower.contains("quantidade"))) {
                Color accent = colLower.contains("total") || colLower.contains("valor") ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE;
                dialog.addKpi(colName, valStr, "Atributo destacado", accent, colLower.contains("qtd") ? "fas-boxes" : "fas-calculator");
                kpiCount++;
            }
        }

        dialog.setStatusBadge(statusText != null ? statusText : "REGISTO", severity);

        // 2. Aba 1: Atributos Gerais
        dialog.addTab("Atributos & Dados", "fas-id-card", UIHelper.ACCENT_BLUE, buildAttributesTab(table, selectedRow));

        // 3. Aba 2: Rastreabilidade
        dialog.addTab("Rastreabilidade", "fas-info-circle", UIHelper.ACCENT_ORANGE, buildMetadataTab(table, selectedRow));

        // 4. Ação à esquerda: Copiar Registo
        ModernButton copyBtn = new ModernButton("Copiar Dados", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        copyBtn.setIcon(UIHelper.icon("fas-copy", 14, Color.WHITE));
        copyBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < table.getColumnCount(); col++) {
                if (isHidden(table, col)) continue;
                sb.append(table.getColumnName(col)).append(": ").append(valueText(table.getValueAt(selectedRow, col))).append("\n");
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            ToastManager.success(dialog.getDialog(), "Dados do registo copiados para a área de transferência.");
        });
        dialog.addLeftAction(copyBtn);

        dialog.showDialog();
    }

    private static JComponent buildAttributesTab(JTable table, int row) {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(UIHelper.BG_DARK);
        container.setBorder(new EmptyBorder(14, 14, 14, 14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int detailRow = 0;
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (isHidden(table, col)) continue;

            String labelStr = table.getColumnName(col);
            String valueStr = valueText(table.getValueAt(row, col));

            gbc.gridx = 0;
            gbc.gridy = detailRow;
            gbc.weightx = 0.30;
            JLabel label = new JLabel(labelStr);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setForeground(UIHelper.TEXT_MUTED);
            container.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.70;
            container.add(valueComponent(valueStr), gbc);
            detailRow++;
        }

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private static JComponent buildMetadataTab(JTable table, int row) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 14, 14));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        panel.add(buildCard("Posição na Tabela", "fas-list-ol", UIHelper.ACCENT_BLUE, "Linha " + (row + 1) + " de " + table.getRowCount()));
        panel.add(buildCard("Total de Colunas", "fas-columns", UIHelper.APPROVED_GREEN, table.getColumnCount() + " campos registados"));
        panel.add(buildCard("Estado do Inspector", "fas-shield-alt", UIHelper.ACCENT_CYAN, "Só-leitura (Seguro)"));
        panel.add(buildCard("Motor Visual", "fas-cube", UIHelper.ACCENT_ORANGE, "ExecutiveDetailDialog Canónico"));

        return panel;
    }

    private static ModernPanel buildCard(String title, String icon, Color iconColor, String content) {
        ModernPanel p = new ModernPanel(12);
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(new JLabel(UIHelper.icon(icon, 15, iconColor)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(UIHelper.TEXT_MUTED);
        top.add(t);

        JLabel c = new JLabel(content);
        c.setFont(new Font("Segoe UI", Font.BOLD, 13));
        c.setForeground(UIHelper.TEXT_LIGHT);

        p.add(top, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    static boolean isHidden(JTable table, int viewColumn) {
        var column = table.getColumnModel().getColumn(viewColumn);
        return column.getWidth() == 0 && column.getMaxWidth() == 0;
    }

    static String valueText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static JComponent valueComponent(String value) {
        if (value.length() > 50 || value.contains("\n")) {
            JTextArea area = new JTextArea(value);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            UIHelper.styleTextArea(area);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(360, 70));
            scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
            return scroll;
        }
        JTextField field = new JTextField(value);
        field.setEditable(false);
        UIHelper.styleTextField(field);
        return field;
    }
}
