package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Painel de resultado da reconciliação de passagem de turno entre operadores.
 */
final class PosShiftHandoverResultPanel {

    private final ModernPanel card;
    private final JLabel lblExpected;
    private final JLabel lblCounted;
    private final JLabel lblDiff;

    PosShiftHandoverResultPanel() {
        card = new ModernPanel(12);
        card.setLayout(new GridLayout(4, 1, 4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setVisible(false);

        JLabel lblTitle = new JLabel("Resultado da Reconciliação:");
        lblTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        lblTitle.setForeground(Color.WHITE);

        lblExpected = new JLabel("Esperado: —");
        lblExpected.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblExpected.setForeground(UIHelper.TEXT_LIGHT);

        lblCounted = new JLabel("Contado: —");
        lblCounted.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblCounted.setForeground(UIHelper.TEXT_LIGHT);

        lblDiff = new JLabel("Diferença: —");
        lblDiff.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        lblDiff.setForeground(UIHelper.TEXT_LIGHT);

        card.add(lblTitle);
        card.add(lblExpected);
        card.add(lblCounted);
        card.add(lblDiff);
    }

    JComponent component() {
        return card;
    }

    void showResult(ShiftReconciliationDTO result) {
        if (result == null) return;
        lblExpected.setText(String.format("Esperado: %,.2f MT", result.expectedCash() != null ? result.expectedCash() : BigDecimal.ZERO));
        lblCounted.setText(String.format("Contado: %,.2f MT", result.countedCash() != null ? result.countedCash() : BigDecimal.ZERO));

        BigDecimal diff = result.difference() != null ? result.difference() : BigDecimal.ZERO;
        String diffColor = diff.compareTo(BigDecimal.ZERO) == 0 ? "exacto"
                : (diff.compareTo(BigDecimal.ZERO) > 0 ? "sobra" : "quebra");
        lblDiff.setText(String.format("Diferença: %,.2f MT (%s)", diff, diffColor));
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            lblDiff.setForeground(diff.compareTo(BigDecimal.ZERO) > 0
                    ? UIHelper.PENDING_YELLOW : UIHelper.REJECTED_RED);
        } else {
            lblDiff.setForeground(UIHelper.APPROVED_GREEN);
        }
        card.setVisible(true);
    }
}
