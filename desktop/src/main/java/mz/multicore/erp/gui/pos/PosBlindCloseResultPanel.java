package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Painel de reconciliação e resultado pós-fecho da sessão POS (revelado após o fecho).
 */
final class PosBlindCloseResultPanel {

    private final ModernPanel card;
    private final JLabel lblStatusResult;
    private final JLabel lblExpected;
    private final JLabel lblCounted;
    private final JLabel lblDiff;
    private final JLabel lblAuditedNotes;

    PosBlindCloseResultPanel() {
        card = new ModernPanel(14);
        card.setLayout(new GridLayout(5, 1, 6, 6));
        card.setBackground(UIHelper.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setVisible(false);

        lblStatusResult = new JLabel("Sessão de Caixa Fechada com Sucesso");
        lblStatusResult.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lblStatusResult.setForeground(UIHelper.APPROVED_GREEN);
        lblStatusResult.setIcon(UIHelper.icon("fas-check-circle", 15, UIHelper.APPROVED_GREEN));
        lblStatusResult.setIconTextGap(8);

        lblExpected = new JLabel("Esperado pelo Sistema: —");
        lblExpected.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblExpected.setForeground(UIHelper.TEXT_LIGHT);

        lblCounted = new JLabel("Contado pelo Operador: —");
        lblCounted.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblCounted.setForeground(UIHelper.TEXT_LIGHT);

        lblDiff = new JLabel("Diferença: —");
        lblDiff.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lblDiff.setForeground(UIHelper.TEXT_LIGHT);

        lblAuditedNotes = new JLabel("");
        lblAuditedNotes.setFont(new Font(UIHelper.FONT, Font.ITALIC, 11));
        lblAuditedNotes.setForeground(UIHelper.TEXT_MUTED);

        card.add(lblStatusResult);
        card.add(lblExpected);
        card.add(lblCounted);
        card.add(lblDiff);
        card.add(lblAuditedNotes);
    }

    JComponent component() {
        return card;
    }

    void showResult(PosZReportDTO z, BigDecimal countedCash, String notes) {
        BigDecimal expected = z != null && z.expectedCash() != null ? z.expectedCash() : BigDecimal.ZERO;
        lblExpected.setText(String.format("Esperado pelo Sistema: %,.2f MT", expected));
        lblCounted.setText(String.format("Contado pelo Operador: %,.2f MT", countedCash != null ? countedCash : BigDecimal.ZERO));

        BigDecimal diff = z != null && z.difference() != null ? z.difference() : BigDecimal.ZERO;
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            lblDiff.setText("Diferença: 0.00 MT (Caixa Bate Certo)");
            lblDiff.setForeground(UIHelper.APPROVED_GREEN);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            lblDiff.setText(String.format("Diferença: %,.2f MT (Falta / Quebra na Gaveta)", diff));
            lblDiff.setForeground(UIHelper.REJECTED_RED);
        } else {
            lblDiff.setText(String.format("Diferença: +%,.2f MT (Sobra na Gaveta)", diff));
            lblDiff.setForeground(UIHelper.ACCENT_CYAN);
        }

        if (notes != null && !notes.isBlank()) {
            lblAuditedNotes.setText("Observações: " + notes.trim());
        } else {
            lblAuditedNotes.setText("");
        }

        card.setVisible(true);
    }
}
