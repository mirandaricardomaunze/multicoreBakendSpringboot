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
    private final ModernButton saveBtn;
    private final ModernButton cancelBtn;
    private final JPanel buttonRow;
    private Runnable onSave;
    private java.util.function.Supplier<java.util.concurrent.Callable<?>> asyncSaveFactory;
    private boolean saved = false;
    private static final int MIN_WIDTH = UIHelper.DIALOG_FORM_MIN_WIDTH + 70;
    private static final int MIN_HEIGHT = 320;

    public ModernFormDialog(Window parent, String title, JComponent content) {
        this(parent, title, null, null, content);
    }

    /**
     * Variante com ícone explícito no cabeçalho. {@code iconCode} é um código FontAwesome
     * (ex.: {@code "fas-file-invoice"}); se {@code null}, é deduzido do título.
     */
    public ModernFormDialog(Window parent, String title, String iconCode, JComponent content) {
        this(parent, title, iconCode, null, content);
    }

    /**
     * Variante premium completa: ícone (ou deduzido do título) + subtítulo opcional no cabeçalho.
     */
    public ModernFormDialog(Window parent, String title, String iconCode, String subtitle, JComponent content) {
        this.dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        String code = iconCode != null ? iconCode : UIHelper.iconForTitle(title);
        dialog.setIconImage(UIHelper.iconImage(code, 24, UIHelper.ACCENT_BLUE));

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(UIHelper.BG_DARK);
        main.setBorder(new EmptyBorder(22, 26, 20, 26));

        // Header premium — badge com ícone + título + subtítulo + divisória
        main.add(UIHelper.buildPremiumHeader(code, title, subtitle), BorderLayout.NORTH);

        // Content — sempre dentro de scroll vertical (modal responsivo: a tabela/campos
        // nunca empurram o diálogo para fora do ecrã; aparece scroll quando necessário).
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIHelper.BG_DARK);
        JScrollPane contentScroll = new JScrollPane(content);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        contentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentPanel.add(contentScroll, BorderLayout.CENTER);
        main.add(contentPanel, BorderLayout.CENTER);

        // Buttons
        cancelBtn = UIHelper.createSecondaryButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 14));
        cancelBtn.setPreferredSize(new Dimension(110, 38));
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn = UIHelper.createSuccessButton("Gravar");
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.setPreferredSize(new Dimension(150, 38));
        saveBtn.addActionListener(e -> attemptSave());

        buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(14, 0, 0, 0)));
        buttonRow.add(cancelBtn);
        buttonRow.add(saveBtn);
        main.add(buttonRow, BorderLayout.SOUTH);

        dialog.add(main);
        dialog.pack();
        // Responsivo: nunca maior que a janela principal; o scroll trata o excesso. O modal é
        // limitado e centrado dentro da janela principal — nunca "sai" para fora dela.
        Dimension area = UIHelper.mainArea().getSize();
        int maxW = (int) (area.width * 0.94);
        int maxH = (int) (area.height * 0.94);
        Dimension size = dialog.getSize();
        int w = Math.min(maxW, Math.max(MIN_WIDTH, size.width));
        int h = Math.min(maxH, Math.max(MIN_HEIGHT, size.height));
        dialog.setSize(w, h);
        dialog.setMinimumSize(new Dimension(Math.min(MIN_WIDTH, maxW), Math.min(MIN_HEIGHT, maxH)));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        UIHelper.containWithinMain(dialog);
    }

    public ModernFormDialog setOnSave(Runnable onSave) {
        this.onSave = onSave;
        this.asyncSaveFactory = null;
        return this;
    }

    /**
     * Grava fora do EDT. A fábrica corre no EDT para validar/capturar os campos; a tarefa devolvida
     * faz apenas a chamada remota. Em erro o modal permanece aberto e os botões são reactivados.
     */
    public ModernFormDialog setOnSaveAsync(
            java.util.function.Supplier<java.util.concurrent.Callable<?>> asyncSaveFactory) {
        this.asyncSaveFactory = asyncSaveFactory;
        this.onSave = null;
        return this;
    }

    /**
     * Converte o modal num **inspector só-leitura**: remove o botão "Gravar" e o "Cancelar" passa a
     * ser o único botão "Fechar". Para visualizar detalhes sem acção de gravação.
     */
    public ModernFormDialog asReadOnly(String closeLabel) {
        buttonRow.remove(saveBtn);
        cancelBtn.setText(closeLabel == null ? "Fechar" : closeLabel);
        cancelBtn.setIcon(UIHelper.icon("fas-check", 14));
        cancelBtn.setPreferredSize(new Dimension(130, 38));
        buttonRow.revalidate();
        buttonRow.repaint();
        return this;
    }

    /**
     * Acrescenta um botão de acção extra ao rodapé (à esquerda de Cancelar/Gravar). O caller liga
     * a acção e, se necessário, fecha o modal via {@link #close()}. Ex.: "Rejeitar" no modal de
     * decisão de aprovações.
     */
    public ModernFormDialog addActionButton(ModernButton btn) {
        btn.setPreferredSize(new Dimension(130, 38));
        buttonRow.add(btn, 0);
        buttonRow.revalidate();
        buttonRow.repaint();
        return this;
    }

    /** Fecha o modal por código (para acções extra que terminam o diálogo). */
    public void close() {
        dialog.dispose();
    }

    /** Personaliza o botão de confirmação (texto + ícone). Útil quando "Gravar" não é a acção certa. */
    public ModernFormDialog setConfirmButton(String label, String iconCode) {
        saveBtn.setText(label);
        if (iconCode != null) {
            saveBtn.setIcon(UIHelper.icon(iconCode, 14));
        }
        return this;
    }

    public ModernFormDialog setSize(int width, int height) {
        Dimension area = UIHelper.mainArea().getSize();
        int w = Math.min((int) (area.width * 0.94), Math.max(MIN_WIDTH, width));
        int h = Math.min((int) (area.height * 0.94), Math.max(MIN_HEIGHT, height));
        dialog.setSize(w, h);
        UIHelper.containWithinMain(dialog);
        return this;
    }

    /** Mostra o diálogo (bloqueia). Devolve true se Gravar foi acionado com sucesso. */
    public boolean showDialog() {
        dialog.setVisible(true);
        return saved;
    }

    private void attemptSave() {
        if (asyncSaveFactory != null) {
            try {
                java.util.concurrent.Callable<?> task = asyncSaveFactory.get();
                cancelBtn.setEnabled(false);
                UIHelper.submitAsync(saveBtn, task, ignored -> {
                    cancelBtn.setEnabled(true);
                    saved = true;
                    dialog.dispose();
                }, error -> {
                    cancelBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(dialog, error.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                });
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
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
