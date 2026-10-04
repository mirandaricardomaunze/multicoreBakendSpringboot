package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Banner ergonómico de notificação e restauração de rascunhos de formulários.
 * Apresentado automaticamente no topo de diálogos quando existem dados salvos de uma sessão anterior.
 */
public class FormDraftBanner extends JPanel {

    private final String formKey;
    private final Consumer<Map<String, String>> onRestore;
    private FormDraft currentDraft;

    public FormDraftBanner(String formKey, Consumer<Map<String, String>> onRestore) {
        this.formKey = formKey;
        this.onRestore = onRestore;
        this.currentDraft = FormDraftManager.getInstance().getDraft(formKey);

        setLayout(new BorderLayout(10, 0));
        setOpaque(true);
        setBackground(new Color(40, 35, 20)); // Fundo âmbar suave compatível com tema escuro
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIHelper.PENDING_YELLOW),
                new EmptyBorder(6, 12, 6, 12)
        ));

        if (currentDraft == null || currentDraft.payload().isEmpty()) {
            setVisible(false);
            return;
        }

        buildUi();
    }

    private void buildUi() {
        removeAll();

        // Lado Esquerdo: Ícone + Mensagem Informativa
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel(UIHelper.icon("fas-history", 16, UIHelper.PENDING_YELLOW));
        JLabel msg = new JLabel("Existe um rascunho recuperável guardado "
                + currentDraft.formattedTimeAgo() + " (" + currentDraft.payload().size() + " campos).");
        msg.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        msg.setForeground(UIHelper.TEXT_LIGHT);

        left.add(icon);
        left.add(msg);
        add(left, BorderLayout.WEST);

        // Lado Direito: Ações de Restauração e Descarte
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        ModernButton restoreBtn = new ModernButton("Restaurar Rascunho", UIHelper.PENDING_YELLOW, UIHelper.PENDING_YELLOW.darker());
        restoreBtn.setForeground(Color.BLACK);
        restoreBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        restoreBtn.setPreferredSize(new Dimension(140, 28));
        restoreBtn.setIcon(UIHelper.icon("fas-undo", 12, Color.BLACK));
        restoreBtn.addActionListener(e -> {
            if (onRestore != null && currentDraft != null) {
                onRestore.accept(currentDraft.payload());
            }
            setVisible(false);
        });

        ModernButton discardBtn = UIHelper.createSecondaryButton("Descartar");
        discardBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        discardBtn.setPreferredSize(new Dimension(85, 28));
        discardBtn.setIcon(UIHelper.icon("fas-trash-alt", 11, UIHelper.REJECTED_RED));
        discardBtn.addActionListener(e -> {
            FormDraftManager.getInstance().clearDraft(formKey);
            setVisible(false);
        });

        right.add(restoreBtn);
        right.add(Box.createHorizontalStrut(4));
        right.add(discardBtn);
        add(right, BorderLayout.EAST);

        setVisible(true);
        revalidate();
        repaint();
    }

    public FormDraft getCurrentDraft() {
        return currentDraft;
    }

    public void checkAndRefresh() {
        this.currentDraft = FormDraftManager.getInstance().getDraft(formKey);
        if (currentDraft != null && !currentDraft.payload().isEmpty()) {
            buildUi();
        } else {
            setVisible(false);
        }
    }
}
