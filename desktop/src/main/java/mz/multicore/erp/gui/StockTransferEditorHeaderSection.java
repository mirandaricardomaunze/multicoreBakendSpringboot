package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Construtor do layout e campos do cabeçalho da transferência de stock. */
final class StockTransferEditorHeaderSection {

    private StockTransferEditorHeaderSection() {}

    static JComponent build(String title, String instructionText,
                            JComboBox<String> originCombo, JComboBox<String> destinationCombo,
                            JTextField driverField, JTextField plateField, JTextField responsibleField,
                            JTextField vehicleField, JTextField notesField, JCheckBox requiresReceiptCheck) {
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addHeaderCol(formCard, gbc, 0, 0, 1.1, 1, "Armazém de Origem *:", originCombo, null);
        addHeaderCol(formCard, gbc, 1, 0, 1.1, 1, "Armazém de Destino *:", destinationCombo, null);
        addHeaderCol(formCard, gbc, 2, 0, 1.0, 1, "Motorista *:", driverField, "Nome do motorista...");
        addHeaderCol(formCard, gbc, 3, 0, 0.8, 1, "Matrícula *:", plateField, "Ex: ABC-123-MC...");

        addHeaderCol(formCard, gbc, 0, 2, 1.1, 1, "Responsável (opcional):", responsibleField, "Responsável pelo envio...");
        addHeaderCol(formCard, gbc, 1, 2, 1.1, 1, "Viatura / Modelo:", vehicleField, "Ex: Caminhão Canter 4T...");
        addHeaderCol(formCard, gbc, 2, 2, 1.8, 2, "Observações:", notesField, "Notas adicionais de transporte...");

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 6, 4, 6);
        formCard.add(requiresReceiptCheck, gbc);

        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        JLabel instruction = new JLabel(instructionText);
        instruction.setForeground(UIHelper.TEXT_MUTED);
        section.add(UIHelper.tableCardTop(title, instruction), BorderLayout.NORTH);
        section.add(formCard, BorderLayout.CENTER);
        return section;
    }

    private static void addHeaderCol(JPanel panel, GridBagConstraints gbc, int x, int y, double wx, int gw,
                                     String label, JComponent comp, String placeholder) {
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = gw; gbc.weightx = wx;
        gbc.insets = new Insets(y == 0 ? 2 : 4, 6, 2, 6);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(UIHelper.TEXT_MUTED);
        panel.add(lbl, gbc);
        gbc.gridy = y + 1;
        gbc.insets = new Insets(2, 6, y == 0 ? 6 : 4, 6);
        if (placeholder != null) comp.putClientProperty("JTextField.placeholderText", placeholder);
        panel.add(comp, gbc);
    }
}
