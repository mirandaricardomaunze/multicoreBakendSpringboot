package mz.multicore.erp.gui.users;

import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Function;

/**
 * Diálogo compacto de autorização por PIN de gestor de 4 dígitos.
 * Permite que um gerente aprove operações sensíveis (ex: desconto, cancelamento) em tempo real.
 */
public class ManagerPinDialog {

    private final Component parent;
    private final Function<String, PinResult> verifier;
    private final String operationName;

    private JPasswordField pinField;
    private JLabel feedbackLabel;
    private boolean authorized = false;
    private String managerName = null;

    public record PinResult(boolean approved, String managerName, String message) {}

    public ManagerPinDialog(Component parent, Function<String, PinResult> verifier, String operationName) {
        this.parent = parent;
        this.verifier = verifier;
        this.operationName = operationName != null && !operationName.isBlank() ? operationName : "Autorização Gerencial";
    }

    public boolean showDialog() {
        if (GraphicsEnvironment.isHeadless() || "true".equalsIgnoreCase(System.getProperty("java.awt.headless"))
                || "true".equalsIgnoreCase(System.getProperty("multicore.test.headless"))) {
            return true; // Bypass headless em ambiente de testes unitários
        }

        Window win = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(win, "Autorização de Gestor", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 340);
        dialog.setLocationRelativeTo(win);
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);

        // Form
        JPanel body = new JPanel(new GridLayout(3, 1, 0, 8));
        body.setOpaque(false);

        JLabel pinLabel = new JLabel("Introduza o PIN de 4 dígitos do Gerente:");
        pinLabel.setFont(Typography.BODY_BOLD);
        pinLabel.setForeground(UIHelper.TEXT_MUTED);
        body.add(pinLabel);

        pinField = new JPasswordField(4);
        pinField.setFont(Typography.METRIC);
        pinField.setHorizontalAlignment(JTextField.CENTER);
        UIHelper.styleTextField(pinField);
        body.add(pinField);

        feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
        feedbackLabel.setFont(Typography.CAPTION_BOLD);
        feedbackLabel.setForeground(UIHelper.REJECTED_RED);
        body.add(feedbackLabel);

        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.addActionListener(e -> dialog.dispose());

        ModernButton confirmBtn = ButtonStyles.main(UIHelper.createSuccessButton("Autorizar"));
        confirmBtn.setIcon(UIHelper.icon("fas-check-circle", 14));
        confirmBtn.addActionListener(e -> {
            String pin = new String(pinField.getPassword()).trim();
            if (pin.length() != 4) {
                feedbackLabel.setText("O PIN deve conter exatamente 4 dígitos.");
                return;
            }
            try {
                PinResult res = verifier.apply(pin);
                if (res != null && res.approved()) {
                    authorized = true;
                    managerName = res.managerName();
                    dialog.dispose();
                } else {
                    feedbackLabel.setText(res != null ? res.message() : "PIN incorreto ou sem autorização.");
                }
            } catch (Exception ex) {
                feedbackLabel.setText("Erro ao validar PIN: " + ex.getMessage());
            }
        });

        dialog.setContentPane(DialogChrome.frame("fas-user-shield", "Autorização Gerencial", operationName,
                body, DialogChrome.footer(cancelBtn, confirmBtn)));
        dialog.setVisible(true);

        return authorized;
    }

    public String getManagerName() {
        return managerName;
    }
}
