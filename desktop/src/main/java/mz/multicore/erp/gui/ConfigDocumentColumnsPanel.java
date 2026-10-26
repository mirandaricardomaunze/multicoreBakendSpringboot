package mz.multicore.erp.gui;

import mz.multicore.erp.gui.ConfigDocumentColumnsOperations.ColumnBoxGroup;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.documents.model.DocumentType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Gestão das colunas visíveis nos documentos oficiais (Fatura, Encomenda,
 * Guia de Remessa, etc.) e texto de rodapé de recibos POS.
 */
final class ConfigDocumentColumnsPanel {

    private final ConfigPanel owner;
    private JComboBox<String> docTypeCombo;
    private ColumnBoxGroup group;

    ConfigDocumentColumnsPanel(ConfigPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(UIHelper.createHeading("Colunas Visíveis nos Documentos"), BorderLayout.NORTH);
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typeRow.setOpaque(false);
        JLabel typeLbl = new JLabel("Tipo de documento:");
        typeLbl.setForeground(UIHelper.TEXT_MUTED);
        docTypeCombo = new JComboBox<>();
        for (var t : DocumentType.values()) docTypeCombo.addItem(t.label());
        UIHelper.styleComboBox(docTypeCombo);
        docTypeCombo.addActionListener(e -> loadDocumentColumns());
        typeRow.add(typeLbl);
        typeRow.add(docTypeCombo);
        north.add(typeRow, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 15));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topCard = new JPanel(new BorderLayout(0, 10));
        topCard.setOpaque(false);
        JLabel desc = new JLabel("<html><body>Escolha que colunas aparecem na tabela de linhas da <b>Fatura</b>, "
                + "<b>Encomenda</b>, <b>Nota de Crédito</b> e <b>Guia de Remessa</b>. A configuração é por empresa "
                + "e não altera totais nem IVA — apenas a presença visual das colunas.</body></html>");
        desc.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        desc.setForeground(UIHelper.TEXT_MUTED);
        topCard.add(desc, BorderLayout.NORTH);
        topCard.add(ConfigDocumentColumnsOperations.buildPresetsRow(
                () -> ConfigDocumentColumnsOperations.setCols(group, false, true, true, false, true, false, false, false, true, true, true),
                () -> ConfigDocumentColumnsOperations.setCols(group, true, true, true, true, true, true, true, true, false, false, true),
                () -> ConfigDocumentColumnsOperations.setCols(group, true, true, true, true, true, true, true, true, true, true, true)
        ), BorderLayout.SOUTH);
        card.add(topCard, BorderLayout.NORTH);

        JPanel checks = new JPanel(new GridLayout(0, 2, 12, 8));
        checks.setOpaque(false);
        JCheckBox[] bx = {
                columnCheckBox("Código de Barras"), columnCheckBox("Referência"), columnCheckBox("Descrição"),
                columnCheckBox("Validade"), columnCheckBox("Quantidade"), columnCheckBox("Embalagens"),
                columnCheckBox("Caixas"), columnCheckBox("% da Caixa"), columnCheckBox("Preço Unitário"),
                columnCheckBox("IVA"), columnCheckBox("Subtotal")
        };
        for (JCheckBox b : bx) checks.add(b);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(checks, BorderLayout.NORTH);
        JPanel footerRow = new JPanel(new BorderLayout(0, 4));
        footerRow.setOpaque(false);
        JLabel footerLbl = new JLabel("Comentário do recibo (rodapé) — só para Recibo POS:");
        footerLbl.setForeground(UIHelper.TEXT_MUTED);
        JTextField footerField = new JTextField();
        UIHelper.styleTextField(footerField);
        footerRow.add(footerLbl, BorderLayout.NORTH);
        footerRow.add(footerField, BorderLayout.CENTER);
        center.add(footerRow, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        group = new ColumnBoxGroup(bx[0], bx[1], bx[2], bx[3], bx[4], bx[5], bx[6], bx[7], bx[8], bx[9], bx[10], footerField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        ModernButton saveBtn = UIHelper.createSuccessButton("Guardar");
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.addActionListener(e -> ConfigDocumentColumnsOperations.saveColumns(owner, selectedDocType(), group));
        actions.add(saveBtn);
        card.add(actions, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() { loadDocumentColumns(); }

    private JCheckBox columnCheckBox(String label) {
        JCheckBox box = new JCheckBox(label);
        box.setOpaque(false);
        box.setForeground(UIHelper.TEXT_LIGHT);
        return box;
    }

    private DocumentType selectedDocType() {
        int idx = docTypeCombo == null ? 0 : Math.max(0, docTypeCombo.getSelectedIndex());
        return DocumentType.values()[idx];
    }

    void loadDocumentColumns() {
        ConfigDocumentColumnsOperations.loadColumns(owner, selectedDocType(), group);
    }
}
