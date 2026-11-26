package mz.multicore.erp.gui;

import mz.multicore.erp.gui.ConfigDocumentTemplateOperations.TemplateForm;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.documents.model.DocumentType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Cartão do modelo de documento em Configurações › Documentos (MODELOS_DOCUMENTO_SPEC §3.4): cor com
 * amostra, posição do logótipo, notas, contas bancárias, QR e pré-visualização gerada pelo backend.
 */
final class ConfigDocumentTemplatePanel {

    private final ConfigPanel owner;
    private final Supplier<DocumentType> documentType;
    private final TemplateForm form = ConfigDocumentTemplateOperations.newForm();
    private JTable banksTable;

    ConfigDocumentTemplatePanel(ConfigPanel owner, Supplier<DocumentType> documentType) {
        this.owner = owner;
        this.documentType = documentType;
    }

    JPanel buildCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.MD));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));
        card.add(UIHelper.createHeading("Modelo do Documento"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(Spacing.XS, 0, Spacing.XS, Spacing.MD);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        addRow(fields, c, 0, "Cor de destaque (#RRGGBB):", colorRow());
        addRow(fields, c, 1, "Posição do logótipo:", form.logoCombo());
        addRow(fields, c, 2, "Notas (até 500 caracteres):", scroll(form.notesArea()));
        banksTable = new JTable(form.banksModel());
        UIHelper.styleTable(banksTable);
        JScrollPane banksScroll = scroll(banksTable);
        banksScroll.setPreferredSize(new Dimension(520, 110));
        addRow(fields, c, 3, "Dados bancários (até 3 contas):", banksScroll);
        addRow(fields, c, 4, "QR code:", form.qrBox());
        card.add(fields, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, 0));
        actions.setOpaque(false);
        ModernButton previewBtn = UIHelper.createSecondaryButton("Pré-visualizar");
        previewBtn.setIcon(UIHelper.icon("fas-eye", 14));
        previewBtn.addActionListener(e -> {
            commitEdits();
            ConfigDocumentTemplateOperations.preview(owner, form);
        });
        ModernButton saveBtn = UIHelper.createSuccessButton("Guardar modelo");
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.addActionListener(e -> {
            commitEdits();
            ConfigDocumentTemplateOperations.save(owner, documentType.get(), form);
        });
        actions.add(previewBtn);
        actions.add(saveBtn);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    /** Cartão das colunas em cima e o do modelo por baixo, num só ecrã com scroll (§3.4). */
    JScrollPane stackBelow(JComponent columnsCard) {
        JPanel stack = new JPanel(new BorderLayout(0, Spacing.LG));
        stack.setOpaque(false);
        stack.add(columnsCard, BorderLayout.NORTH);
        stack.add(buildCard(), BorderLayout.CENTER);
        return scroll(stack);
    }

    void refresh() {
        ConfigDocumentTemplateOperations.load(owner, documentType.get(), form);
    }

    private JPanel colorRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        row.setOpaque(false);
        row.add(form.colorField());
        row.add(form.swatch());
        JLabel hint = new JLabel("Vazio = cor do tema. O texto branco tem de se ler (contraste 4,5:1).");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);
        row.add(hint);
        return row;
    }

    private static void addRow(JPanel grid, GridBagConstraints c, int row, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(Typography.BODY_BOLD);
        lbl.setForeground(UIHelper.TEXT_MUTED);
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        grid.add(lbl, c);
        c.gridx = 1; c.weightx = 1;
        grid.add(field, c);
    }

    private static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        UIHelper.styleScrollPane(scroll);
        return scroll;
    }

    private void commitEdits() {
        if (banksTable != null && banksTable.isEditing()) banksTable.getCellEditor().stopCellEditing();
    }
}
