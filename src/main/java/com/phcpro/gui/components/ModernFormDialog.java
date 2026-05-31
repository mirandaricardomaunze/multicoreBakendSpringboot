package com.phcpro.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal de formulário reutilizável. Esqueleto: header com título + área de conteúdo
 * fornecida pelo caller + botões Cancelar/Gravar.
 *
 * Uso típico:
 * <pre>
 *   ModernFormDialog dlg = new ModernFormDialog(parent, "Novo Fornecedor", buildForm());
 *   dlg.setOnSave(() -> {
 *       // valida + persiste; se lançar, o diálogo permanece aberto
 *       service.create(...);
 *   });
 *   if (dlg.showDialog()) {
 *       reloadTable();
 *   }
 * </pre>
 */
public class ModernFormDialog {

    private final JDialog dialog;
    private final JPanel contentPanel;
    private Runnable onSave;
    private boolean saved = false;
    private static final int MIN_WIDTH = UIHelper.DIALOG_FORM_MIN_WIDTH + 70;
    private static final int MIN_HEIGHT = 320;

    public ModernFormDialog(Window parent, String title, JComponent content) {
        this.dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel main = new JPanel(new BorderLayout(0, 15));
        main.setBackground(UIHelper.BG_DARK);
        main.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        titleLbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        main.add(titleLbl, BorderLayout.NORTH);

        // Content
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIHelper.BG_DARK);
        contentPanel.add(content, BorderLayout.CENTER);
        main.add(contentPanel, BorderLayout.CENTER);

        // Buttons
        ModernButton cancelBtn = new ModernButton("Cancelar", UIHelper.BG_CARD, new Color(55, 65, 81));
        cancelBtn.setPreferredSize(new Dimension(110, 38));
        cancelBtn.addActionListener(e -> dialog.dispose());

        ModernButton saveBtn = new ModernButton("💾  Gravar", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.brighter());
        saveBtn.setPreferredSize(new Dimension(150, 38));
        saveBtn.addActionListener(e -> attemptSave());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelBtn);
        buttonRow.add(saveBtn);
        main.add(buttonRow, BorderLayout.SOUTH);

        dialog.add(main);
        dialog.pack();
        Dimension size = dialog.getSize();
        int w = Math.max(MIN_WIDTH, size.width);
        int h = Math.max(MIN_HEIGHT, size.height);
        dialog.setSize(w, h);
        dialog.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public ModernFormDialog setOnSave(Runnable onSave) {
        this.onSave = onSave;
        return this;
    }

    public ModernFormDialog setSize(int width, int height) {
        dialog.setSize(Math.max(MIN_WIDTH, width), Math.max(MIN_HEIGHT, height));
        dialog.setLocationRelativeTo(dialog.getParent());
        return this;
    }

    /** Mostra o diálogo (bloqueia). Devolve true se Gravar foi acionado com sucesso. */
    public boolean showDialog() {
        dialog.setVisible(true);
        return saved;
    }

    private void attemptSave() {
        if (onSave == null) {
            saved = true;
            dialog.dispose();
            return;
        }
        try {
            onSave.run();
            saved = true;
            dialog.dispose();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(dialog, ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
