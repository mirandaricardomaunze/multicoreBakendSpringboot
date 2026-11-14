package mz.multicore.erp.gui.components;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

final class UiFormComponentStyler {

    private UiFormComponentStyler() {}

    static ModernButton createButton(String text, Icon icon, Color baseColor, ActionListener listener) {
        ModernButton btn = new ModernButton(text, baseColor, baseColor.brighter());
        if (icon != null) btn.setIcon(icon);
        if (listener != null) btn.addActionListener(listener);
        return btn;
    }

    static ModernButton createRefreshButton(Runnable refreshAction) {
        Objects.requireNonNull(refreshAction, "A acção de actualização é obrigatória.");
        ModernButton button = new ModernButton("Actualizar", UIHelper.SECONDARY, UIHelper.SECONDARY_HOVER);
        button.setIcon(UIHelper.icon("fas-sync-alt", 14, Color.WHITE));
        button.setForeground(Color.WHITE);
        button.setToolTipText("Carregar os dados mais recentes");
        button.getAccessibleContext().setAccessibleName("Actualizar dados");
        button.addActionListener(event -> refreshAction.run());
        return button;
    }

    static ModernButton createExportButton(JTable table, String defaultBaseName) {
        Objects.requireNonNull(table, "A tabela para exportação é obrigatória.");
        ModernButton button = new ModernButton("Exportar", UIHelper.SECONDARY, UIHelper.SECONDARY_HOVER);
        button.setIcon(UIHelper.icon("fas-file-excel", 14, UIHelper.APPROVED_GREEN));
        button.setForeground(Color.WHITE);
        button.setToolTipText("Exportar dados da tabela para Excel (.xls) ou CSV");
        button.getAccessibleContext().setAccessibleName("Exportar dados");
        button.addActionListener(event -> TableDataExporter.exportToExcelWithDialog(button, table, defaultBaseName));
        return button;
    }

    static JPanel tableCardTop(String title, JComponent filters, JComponent... actions) {
        return TableCardTop.build(title, filters, actions);
    }

    static ModernButton createIconButton(String accessibleName, String iconCode) {
        if (accessibleName == null || accessibleName.isBlank()) {
            throw new IllegalArgumentException("O botão com ícone deve ter um nome acessível.");
        }
        ModernButton button = UIHelper.createSecondaryButton("");
        button.setIcon(UIHelper.icon(iconCode, 14));
        button.setToolTipText(accessibleName);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        Dimension d = new Dimension(UIHelper.FORM_CONTROL_HEIGHT, UIHelper.FORM_CONTROL_HEIGHT);
        button.setPreferredSize(d);
        button.setMinimumSize(d);
        return button;
    }

    static JButton createIconButton(String code, int size, String tooltip, Runnable action) {
        JButton btn = new JButton(UIHelper.icon(code, size));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (tooltip != null && !tooltip.isBlank()) {
            btn.setToolTipText(tooltip);
            btn.getAccessibleContext().setAccessibleName(tooltip);
        }
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    static ModernButton createAddLineButton() {
        ModernButton button = UIHelper.createSuccessButton("Adicionar Linha");
        button.setIcon(UIHelper.icon("fas-plus", 14));
        Dimension d = new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT);
        button.setPreferredSize(d);
        button.setMinimumSize(d);
        return button;
    }

    static ModernButton createProductLookupButton(Runnable onLookup) {
        ModernButton btn = UIHelper.createSecondaryButton("Pesquisar Catálogo (F3)");
        btn.setIcon(UIHelper.icon("fas-search", 14, UIHelper.ACCENT_BLUE));
        btn.setToolTipText("Abrir catálogo paginado para pesquisar e seleccionar artigos (F3)");
        if (onLookup != null) btn.addActionListener(e -> onLookup.run());
        return btn;
    }

    static JPanel createFilterGroup(String labelText, JComponent component) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        group.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        label.setForeground(UIHelper.TEXT_LIGHT);
        group.add(label);
        group.add(component);
        return group;
    }

    static JComponent makeDialogScrollable(JPanel content, int maxHeight) {
        Dimension contentSize = content.getPreferredSize();
        if (contentSize.height <= maxHeight) return content;
        int width = Math.max(contentSize.width, UIHelper.DIALOG_FORM_MIN_WIDTH) + 24;
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(width, maxHeight));
        return scroll;
    }

    static void styleTextField(JTextField field) {
        field.setBackground(UIHelper.FIELD_BG);
        field.setForeground(UIHelper.TEXT_LIGHT);
        field.setCaretColor(UIHelper.TEXT_LIGHT);
        field.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        field.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        field.putClientProperty("Component.arc", UIHelper.CONTROL_ARC);
        installFocusBorder(field);
        applyFormControlHeight(field);
    }

    static void markFieldInvalid(JComponent component, String message) {
        component.putClientProperty("validationError", message);
        component.setBorder(fieldBorder(UIHelper.REJECTED_RED, 2));
        component.getAccessibleContext().setAccessibleDescription(message);
    }

    static void clearFieldInvalid(JComponent component) {
        component.putClientProperty("validationError", null);
        component.setBorder(fieldBorder(UIHelper.BORDER, 1));
        component.getAccessibleContext().setAccessibleDescription(null);
    }

    static void setReadOnly(JTextComponent component, boolean readOnly) {
        component.setEditable(!readOnly);
        component.putClientProperty("readOnly", readOnly);
        component.setBackground(readOnly ? UIHelper.BG_CARD : UIHelper.FIELD_BG);
        component.setForeground(readOnly ? UIHelper.TEXT_MUTED : UIHelper.TEXT_LIGHT);
        component.setFocusable(!readOnly);
    }

    static Border fieldBorder(Color line, int thickness) {
        Insets insets = new Insets(5 - (thickness - 1), 10, 5 - (thickness - 1), 10);
        if (UIManager.getLookAndFeel() instanceof FlatLaf) {
            return new FlatLineBorder(insets, line, (float) thickness, UIHelper.CONTROL_ARC);
        }
        return new RoundedFallbackBorder(line, thickness, UIHelper.CONTROL_ARC, insets);
    }

    private static class RoundedFallbackBorder implements Border {
        private final Color color;
        private final int thickness;
        private final int arc;
        private final Insets insets;

        RoundedFallbackBorder(Color color, int thickness, int arc, Insets insets) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
            this.insets = insets != null ? insets : new Insets(5 - (thickness - 1), 10, 5 - (thickness - 1), 10);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if (thickness > 1) g2.setStroke(new BasicStroke(thickness));
            int offset = thickness / 2;
            g2.drawRoundRect(x + offset, y + offset, width - thickness - 1, height - thickness - 1, arc, arc);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return (Insets) insets.clone(); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    static void installFocusBorder(JComponent component) {
        component.setBorder(fieldBorder(UIHelper.BORDER, 1));
        if (Boolean.TRUE.equals(component.getClientProperty("focusBorderWired"))) return;
        component.putClientProperty("focusBorderWired", Boolean.TRUE);
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (Boolean.TRUE.equals(component.getClientProperty("noFocusBorder"))) return;
                component.setBorder(fieldBorder(UIHelper.ACCENT, 2));
            }
            @Override public void focusLost(FocusEvent e) {
                if (Boolean.TRUE.equals(component.getClientProperty("noFocusBorder"))) return;
                component.setBorder(fieldBorder(UIHelper.BORDER, 1));
            }
        });
    }

    static void onTextChange(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        });
    }

    static String humanRole(String role) {
        if (role == null || role.isBlank()) return "—";
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> "Administrador";
            case "MANAGER" -> "Gestor";
            case "SELLER" -> "Vendedor";
            case "EMPLOYEE" -> "Funcionário";
            case "SUPERADMIN" -> "Administrador da Plataforma";
            default -> role;
        };
    }

    static void humanizeRoleCombo(JComboBox<String> combo) {
        @SuppressWarnings("unchecked")
        ListCellRenderer<? super String> original = (ListCellRenderer<? super String>) combo.getRenderer();
        combo.setRenderer((list, value, index, selected, focus) -> {
            Component rendered = original.getListCellRendererComponent(list, value, index, selected, focus);
            if (rendered instanceof JLabel label) label.setText(humanRole(value));
            return rendered;
        });
    }

    static void stylePasswordField(JPasswordField field) {
        field.setBackground(UIHelper.FIELD_BG);
        field.setForeground(UIHelper.TEXT_LIGHT);
        field.setCaretColor(UIHelper.TEXT_LIGHT);
        field.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        field.putClientProperty("Component.arc", UIHelper.CONTROL_ARC);
        installFocusBorder(field);
        applyFormControlHeight(field);
    }

    static void styleTextArea(JTextArea area) {
        area.setBackground(UIHelper.FIELD_BG);
        area.setForeground(UIHelper.TEXT_LIGHT);
        area.setCaretColor(UIHelper.TEXT_LIGHT);
        area.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        area.putClientProperty("Component.arc", UIHelper.CONTROL_ARC);
        installFocusBorder(area);
    }

    static <T> ListCellRenderer<T> labelRenderer(Function<T, String> label) {
        return (list, value, index, selected, focus) -> {
            JLabel rendered = new JLabel(value == null ? "" : label.apply(value));
            rendered.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
            rendered.setOpaque(true);
            return rendered;
        };
    }

    static void styleComboBox(JComboBox<?> combo) {
        ListCellRenderer<?> originalRenderer = combo.getRenderer();
        combo.setBackground(UIHelper.FIELD_BG);
        combo.setForeground(UIHelper.TEXT_LIGHT);
        combo.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        combo.putClientProperty("Component.arc", UIHelper.CONTROL_ARC);
        installFocusBorder(combo);
        flattenComboArrow(combo);
        combo.setRenderer(new ListCellRenderer<Object>() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                @SuppressWarnings("unchecked")
                ListCellRenderer<Object> delegate = (ListCellRenderer<Object>) originalRenderer;
                Component rendered = delegate == null
                        ? new DefaultListCellRenderer().getListCellRendererComponent((JList<Object>) list, value, index, isSelected, cellHasFocus)
                        : delegate.getListCellRendererComponent((JList<Object>) list, value, index, isSelected, cellHasFocus);
                rendered.setBackground(isSelected ? UIHelper.SELECTION_BG : UIHelper.FIELD_BG);
                rendered.setForeground(UIHelper.TEXT_LIGHT);
                if (rendered instanceof JComponent component) {
                    component.setBorder(new EmptyBorder(5, 8, 5, 8));
                    component.setOpaque(index >= 0);
                }
                return rendered;
            }
        });
        applyFormControlHeight(combo);
    }

    static <T> JComboBox<T> createComboBox(T[] items, Function<T, String> labelExtractor) {
        JComboBox<T> combo = new JComboBox<>(items);
        if (labelExtractor != null) combo.setRenderer(labelRenderer(labelExtractor));
        styleComboBox(combo);
        return combo;
    }

    static void styleSpinner(JSpinner spinner) {
        spinner.setBackground(UIHelper.FIELD_BG);
        spinner.setForeground(UIHelper.TEXT_LIGHT);
        spinner.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        spinner.putClientProperty("Component.arc", UIHelper.CONTROL_ARC);
        installFocusBorder(spinner);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor de) {
            JTextField tf = de.getTextField();
            tf.setBackground(UIHelper.FIELD_BG);
            tf.setForeground(UIHelper.TEXT_LIGHT);
            tf.setCaretColor(UIHelper.TEXT_LIGHT);
            tf.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
            tf.setBorder(new EmptyBorder(0, 4, 0, 4));
            tf.setOpaque(false);
        }
        applyFormControlHeight(spinner);
    }

    static JLabel createWarningBanner(String message) {
        JLabel banner = new JLabel("<html><body style=\"width:900px\">" + message + "</body></html>");
        banner.setOpaque(true);
        banner.setBackground(UIHelper.PENDING_YELLOW);
        banner.setForeground(UIHelper.BG_DARK);
        banner.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        banner.setBorder(new EmptyBorder(10, 14, 10, 14));
        banner.setVisible(false);
        return banner;
    }

    static JSpinner createYearSpinner(int year) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(year, 2000, 2100, 1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
        return spinner;
    }

    static String humanStatus(String status) {
        String taxType = UiStatusLabels.taxType(status);
        if (taxType != null) return taxType;
        if (status == null || status.isBlank()) return "—";
        String s = status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "ACTIVE", "ACTIVO" -> "Activo";
            case "ACTIVA" -> "Activa";
            case "INACTIVE", "INACTIVO" -> "Inactivo";
            case "INACTIVA" -> "Inactiva";
            case "OPEN", "ABERTO" -> "Aberto";
            case "IN_PROGRESS", "EM_CURSO", "EM CURSO" -> "Em curso";
            case "RESOLVED", "RESOLVIDO" -> "Resolvido";
            case "CLOSED", "FECHADO" -> "Fechado";
            case "DRAFT", "RASCUNHO" -> "Rascunho";
            case "PENDING", "PENDENTE" -> "Pendente";
            case "PENDING_APPROVAL", "PENDENTE_APROVACAO", "PENDENTE DE APROVAÇÃO", "PENDENTE DE APROVACAO" -> "Pendente de aprovação";
            case "PENDING_DISCOUNT_APPROVAL" -> "Aprovação de desconto";
            case "POR_FACTURAR", "POR_FATURAR", "POR FACTURAR", "POR FATURAR" -> "Por faturar";
            case "AWAITING_SEPARATION", "AGUARDA_SEPARACAO", "AGUARDA SEPARAÇÃO", "AGUARDA SEPARACAO" -> "Aguarda separação";
            case "IN_SEPARATION", "EM_SEPARACAO", "EM SEPARAÇÃO", "EM SEPARACAO" -> "Em separação";
            case "SEPARATED", "SEPARADO" -> "Separado";
            case "GUIDE_PENDING", "GUIA_POR_APROVAR", "GUIA POR APROVAR" -> "Guia por aprovar";
            case "GUIDED", "EXPEDIDO_POR_GUIA", "EXPEDIDO POR GUIA" -> "Expedido por guia";
            case "TRANSFER_PENDING", "TRANSFERENCIA_POR_APROVAR", "TRANSFERÊNCIA POR APROVAR" -> "Transferência por aprovar";
            case "TRANSFERRED", "TRANSFERIDO", "TRANSFERIDO PARA A LOJA" -> "Transferido para a loja";
            case "BILLED", "INVOICED", "FACTURADO", "FACTURADA", "FATURADO", "FATURADA" -> "Faturado";
            case "APPROVED", "APROVADO", "APROVADA" -> "Aprovado";
            case "REJECTED", "REJEITADO", "REJEITADA" -> "Rejeitado";
            case "CANCELLED", "CANCELADO", "CANCELADA", "ANULADO", "ANULADA" -> "Cancelado";
            case "PAID", "PAGA", "PAGO" -> "Paga";
            case "PARTIALLY_PAID", "PARCIALMENTE_PAGA", "PARCIALMENTE PAGA" -> "Parcialmente paga";
            case "OVERDUE", "EM_ATRASO", "EM ATRASO" -> "Em atraso";
            case "ORDERED", "ENCOMENDADO" -> "Encomendado";
            case "PARTIALLY_RECEIVED", "PARCIALMENTE_RECEBIDO", "PARCIALMENTE RECEBIDO" -> "Parcialmente recebido";
            case "RECEIVED", "RECEBIDO" -> "Recebido";
            case "RECEIVED_WITH_DISCREPANCY", "RECEBIDO C/ DIVERGÊNCIA", "RECEBIDO C/ DIVERGENCIA" -> "Recebido c/ divergência";
            case "COMPLETED", "CONCLUIDO", "CONCLUÍDO", "CONCLUIDA", "CONCLUÍDA" -> "Concluído";
            case "SUSPENDED", "SUSPENSO", "SUSPENSA" -> "Suspenso";
            case "EXPIRED", "EXPIRADO", "EXPIRADA" -> "Expirado";
            case "OPERATIONAL", "OPERACIONAL" -> "Operacional";
            case "MAINTENANCE", "MANUTENCAO", "MANUTENÇÃO" -> "Manutenção";
            case "INSPECTION", "INSPECCAO", "INSPECÇÃO" -> "Inspecção";
            case "OUT_OF_SERVICE", "FORA_DE_SERVICO", "FORA DE SERVIÇO" -> "Fora de serviço";
            case "SOLD", "VENDIDO" -> "Vendido";
            case "PLANEADA" -> "Planeada";
            case "EM_PRODUCAO", "EM PRODUÇÃO", "EM PRODUCAO" -> "Em produção";
            case "ISSUED", "EMITIDA", "EMITIDO" -> "Emitida";
            case "SHIPPED", "ENVIADO", "EM TRANSITO", "EM TRÂNSITO", "IN_TRANSIT" -> "Em trânsito";
            case "DELIVERED", "ENTREGUE" -> "Entregue";
            case "MATCHED", "CONCILIADO" -> "Conciliado";
            case "UNMATCHED", "POR_CONCILIAR", "POR CONCILIAR" -> "Por conciliar";
            case "PENDING_JUSTIFICATION", "POR JUSTIFICAR" -> "Por justificar";
            case "JUSTIFIED", "JUSTIFICADA" -> "Justificada";
            case "UNJUSTIFIED", "INJUSTIFICADA" -> "Injustificada";
            case "SICK", "BAIXA MEDICA", "BAIXA MÉDICA" -> "Baixa médica";
            case "MATERNITY", "MATERNIDADE" -> "Maternidade";
            case "UNPAID_LEAVE", "LICENCA SEM VENCIMENTO", "LICENÇA SEM VENCIMENTO" -> "Licença sem vencimento";
            case "PURCHASE", "COMPRA" -> "Compra";
            case "ENTRY", "ENTRADA" -> "Entrada";
            case "SALE", "VENDA" -> "Venda";
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERÊNCIA" -> "Transferência";
            case "ADJUSTMENT", "AJUSTE" -> "Ajuste";
            case "RETURN", "DEVOLUCAO", "DEVOLUÇÃO" -> "Devolução";
            case "REVERSAL", "ESTORNO" -> "Estorno";
            case "ESGOTADO" -> "Esgotado";
            case "CRITICO", "CRÍTICO" -> "Crítico";
            case "BAIXO" -> "Baixo";
            case "TERMINATED", "RESCINDIDO", "DEMITIDO" -> "Rescindido";
            case "SUPERADMIN" -> "SuperAdmin";
            case "DEBIT", "DEBITO", "DÉBITO" -> "Débito";
            case "CREDIT", "CREDITO", "CRÉDITO" -> "Crédito";
            case "CASH", "DINHEIRO" -> "Dinheiro";
            case "BANK_TRANSFER" -> "Transferência";
            case "CARD", "CARTAO", "CARTÃO" -> "Cartão";
            case "MPESA", "M-PESA" -> "M-Pesa";
            case "EMOLA", "E-MOLA" -> "E-Mola";
            default -> status.trim().replace('_', ' ');
        };
    }

    private static void flattenComboArrow(JComboBox<?> combo) {
        for (Component child : combo.getComponents()) {
            if (child instanceof AbstractButton arrow) {
                arrow.setBorder(new EmptyBorder(0, 6, 0, 6));
                arrow.setBorderPainted(false);
                arrow.setContentAreaFilled(false);
                arrow.setFocusable(false);
                arrow.setOpaque(false);
            }
        }
    }

    private static void applyFormControlHeight(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        int width = Math.max(preferred.width, component.getMinimumSize().width);
        component.setPreferredSize(new Dimension(width, UIHelper.FORM_CONTROL_HEIGHT));
        component.setMinimumSize(new Dimension(0, UIHelper.FORM_CONTROL_HEIGHT));
    }

    static JPanel createDialogForm(int columns, Object... labelsAndComponents) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0 / columns;
        gbc.insets = new Insets(6, 7, 6, 7);

        int pair = 0;
        for (int i = 0; i + 1 < labelsAndComponents.length; i += 2, pair++) {
            Object labelObj = labelsAndComponents[i];
            Object compObj = labelsAndComponents[i + 1];
            if (compObj instanceof Component c) {
                if (c instanceof JPasswordField pf) stylePasswordField(pf);
                else if (c instanceof JTextField tf) styleTextField(tf);
                else if (c instanceof JComboBox<?> cb) styleComboBox(cb);
                else if (c instanceof JSpinner sp) styleSpinner(sp);
                else if (c instanceof JTextArea ta) styleTextArea(ta);
            }
            JPanel cell = new JPanel(new BorderLayout(0, 4));
            cell.setOpaque(false);
            if (labelObj instanceof String rawLabel && compObj instanceof JComponent input) {
                boolean required = rawLabel.contains("*");
                cell.add(new FormField(rawLabel.replace("*", "").trim(), input, required, null), BorderLayout.CENTER);
            } else {
                if (labelObj instanceof Component l) cell.add(l, BorderLayout.NORTH);
                if (compObj instanceof Component comp) cell.add(comp, BorderLayout.CENTER);
            }
            gbc.gridx = pair % columns;
            gbc.gridy = pair / columns;
            panel.add(cell, gbc);
        }
        Dimension preferred = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.max(UIHelper.DIALOG_FORM_MIN_WIDTH, preferred.width), preferred.height));
        panel.setMinimumSize(new Dimension(UIHelper.DIALOG_FORM_MIN_WIDTH, preferred.height));
        return panel;
    }

}
