package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
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
                BorderFactory.createEmptyBorder(Spacing.MD, Spacing.LG, Spacing.MD, Spacing.LG)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setVisible(false);

        JLabel lblTitle = new JLabel("Resultado da Reconciliação:");
        lblTitle.setFont(Typography.EMPHASIS_BOLD);
        lblTitle.setForeground(Color.WHITE);

        lblExpected = new JLabel("Esperado: —");
        lblExpected.setFont(Typography.BODY);
        lblExpected.setForeground(UIHelper.TEXT_LIGHT);

        lblCounted = new JLabel("Contado: —");
        lblCounted.setFont(Typography.BODY);
        lblCounted.setForeground(UIHelper.TEXT_LIGHT);

        lblDiff = new JLabel("Diferença: —");
        lblDiff.setFont(Typography.EMPHASIS_BOLD);
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
