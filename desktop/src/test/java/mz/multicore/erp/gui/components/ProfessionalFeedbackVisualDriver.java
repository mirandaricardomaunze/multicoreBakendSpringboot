package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** Driver interactivo dos casos M-01..M-08 do harness de feedback profissional. */
public final class ProfessionalFeedbackVisualDriver {

    private ProfessionalFeedbackVisualDriver() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProfessionalFeedbackVisualDriver::show);
    }

    private static void show() {
        UIHelper.applyTheme(Theme.DARK);
        JFrame frame = new JFrame("Harness visual — Feedback profissional");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        UIHelper.registerMainWindow(frame);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        root.add(UIHelper.buildPremiumHeader("fas-check-circle", "Feedback profissional",
                "M-01..M-08 · componentes reais, sem dependência da API"), BorderLayout.NORTH);

        InlineFeedbackPanel feedback = new InlineFeedbackPanel();
        JPanel centre = new JPanel(new BorderLayout(0, 14));
        centre.setOpaque(false);
        centre.add(feedback, BorderLayout.NORTH);
        JLabel state = new JLabel("Estado do documento: PENDENTE");
        state.setForeground(UIHelper.TEXT_LIGHT);
        state.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        centre.add(state, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        actions.setOpaque(false);
        ModernButton approve = UIHelper.createSuccessButton("M-01 Aprovar");
        approve.addActionListener(e -> {
            state.setText("Estado do documento: APROVADO");
            ToastManager.success(frame, "Documento aprovado com sucesso.");
        });
        ModernButton reject = UIHelper.createDangerButton("M-02 Rejeitar");
        reject.addActionListener(e -> {
            state.setText("Estado do documento: REJEITADO");
            ToastManager.show(frame, FeedbackType.WARNING, "Documento rejeitado; lista actualizada.");
        });
        ModernButton unavailable = UIHelper.createSecondaryButton("M-03 API indisponível");
        unavailable.addActionListener(e -> feedback.show(FeedbackType.ERROR,
                "Não foi possível carregar as aprovações", "Confirme a ligação e tente novamente.",
                "Tentar novamente", () -> feedback.show(FeedbackType.SUCCESS,
                        "Ligação restabelecida", "As aprovações foram actualizadas.", null, null)));
        ModernButton invalidForm = UIHelper.createSecondaryButton("M-04 Formulário inválido");
        invalidForm.addActionListener(e -> {
            JPanel form = new JPanel(new BorderLayout(0, 8));
            form.setOpaque(false);
            JLabel label = new JLabel("Referência obrigatória");
            label.setForeground(UIHelper.TEXT_LIGHT);
            form.add(label, BorderLayout.NORTH);
            form.add(new JTextField(), BorderLayout.CENTER);
            new ModernFormDialog(frame, "Novo documento", "fas-file", form)
                    .setOnSave(() -> { throw new IllegalArgumentException("Preencha a referência antes de gravar."); })
                    .showDialog();
        });
        ModernButton queue = UIHelper.createPrimaryButton("M-05 Três toasts");
        queue.addActionListener(e -> {
            ToastManager.show(frame, FeedbackType.INFO, "1 de 3 — informação");
            ToastManager.show(frame, FeedbackType.SUCCESS, "2 de 3 — sucesso");
            ToastManager.show(frame, FeedbackType.WARNING, "3 de 3 — aviso");
        });
        ModernButton theme = UIHelper.createSecondaryButton("M-06 Alternar tema");
        theme.addActionListener(e -> {
            UIHelper.setTheme(UIHelper.currentTheme() == Theme.DARK ? Theme.LIGHT : Theme.DARK);
            SwingUtilities.updateComponentTreeUI(frame);
            frame.repaint();
        });
        ModernButton confirm = UIHelper.createPrimaryButton("M-08 Confirmação");
        confirm.addActionListener(e -> {
            boolean accepted = ModernMessageDialog.confirm(frame, FeedbackType.WARNING,
                    "Confirmar operação", "Use Esc para cancelar ou Enter para confirmar.", "Confirmar");
            state.setText("Resultado do teclado: " + (accepted ? "CONFIRMADO" : "CANCELADO"));
        });
        ModernButton clear = UIHelper.createSecondaryButton("Limpar mensagem");
        clear.addActionListener(e -> feedback.clear());
        actions.add(approve);
        actions.add(reject);
        actions.add(unavailable);
        actions.add(invalidForm);
        actions.add(queue);
        actions.add(theme);
        actions.add(confirm);
        actions.add(clear);
        centre.add(actions, BorderLayout.SOUTH);
        root.add(centre, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        JLabel scale = new JLabel("Escala JVM: " + System.getProperty("sun.java2d.uiScale", "automática"));
        scale.setForeground(UIHelper.TEXT_MUTED);
        footer.add(scale);
        root.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setSize(780, 540);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
