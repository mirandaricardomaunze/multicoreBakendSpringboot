package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Barra visual de atalhos rápidos posicionada no rodapé do POS.
 * Permite ao operador visualizar instantaneamente as teclas de função disponíveis
 * e clicar directamente com rato ou touchscreen.
 */
public class PosShortcutBar extends ModernPanel {

    private final POSPanel posPanel;

    public PosShortcutBar(POSPanel posPanel) {
        super(12);
        this.posPanel = posPanel;
        initLayout();
    }

    private void initLayout() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 6, 4));
        setBackground(UIHelper.ROW_ALT);
        setBorder(new EmptyBorder(2, 6, 2, 6));

        add(createChip("F1", "Ajuda", "fas-keyboard", UIHelper.ACCENT_CYAN, () -> PosKeyboardShortcutsHandler.showHelp(posPanel)));
        add(createChip("F2", "Artigo", "fas-search", UIHelper.ACCENT_BLUE, () -> PosKeyboardShortcutsHandler.focusProductSearch(posPanel)));
        add(createChip("F3", "Barras", "fas-barcode", UIHelper.ACCENT_BLUE, () -> PosKeyboardShortcutsHandler.focusBarcode(posPanel)));
        add(createChip("F4", "Cliente", "fas-user", UIHelper.ACCENT_BLUE, () -> PosKeyboardShortcutsHandler.focusClientSearch(posPanel)));
        add(createChip("F5", "Desconto", "fas-percent", UIHelper.PENDING_YELLOW, () -> PosKeyboardShortcutsHandler.applyDiscountPrompt(posPanel)));
        add(createChip("F6", "Qtd", "fas-sort-numeric-up", UIHelper.ACCENT_ORANGE, () -> PosKeyboardShortcutsHandler.editQuantity(posPanel)));
        add(createChip("F7", "Cotação", "fas-file-import", UIHelper.ACCENT_CYAN, () -> PosKeyboardShortcutsHandler.openQuotationImport(posPanel)));
        add(createChip("F8", "Devolução", "fas-undo", UIHelper.ACCENT_SKY, () -> PosKeyboardShortcutsHandler.openReturn(posPanel)));
        add(createChip("F9", "Caixa", "fas-cash-register", UIHelper.PENDING_YELLOW, () -> PosKeyboardShortcutsHandler.manageCash(posPanel)));
        add(createChip("F10", "Pagar", "fas-check-circle", UIHelper.APPROVED_GREEN, () -> PosKeyboardShortcutsHandler.checkout(posPanel), true));
        add(createChip("F11", "Histórico", "fas-history", UIHelper.ACCENT, () -> PosKeyboardShortcutsHandler.toggleView(posPanel)));
        add(createChip("F12", "Fecho", "fas-lock", UIHelper.REJECTED_RED, () -> PosKeyboardShortcutsHandler.closeSession(posPanel)));
        add(createChip("ESC", "Limpar", "fas-times-circle", UIHelper.TEXT_MUTED, () -> PosKeyboardShortcutsHandler.cancelOrClear(posPanel)));
    }

    private JPanel createChip(String key, String label, String iconCode, Color accent, Runnable action) {
        return createChip(key, label, iconCode, accent, action, false);
    }

    private JPanel createChip(String key, String label, String iconCode, Color accent, Runnable action, boolean highlight) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        chip.setOpaque(true);
        chip.setBackground(highlight ? UIHelper.APPROVED_GREEN : UIHelper.BG_CARD);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(highlight ? UIHelper.APPROVED_GREEN_HOVER : UIHelper.BORDER, 1),
                new EmptyBorder(1, 4, 1, 6)
        ));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel keyLabel = new JLabel(key, SwingConstants.CENTER);
        keyLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        keyLabel.setForeground(highlight ? UIHelper.APPROVED_GREEN : Color.WHITE);
        keyLabel.setBackground(highlight ? Color.WHITE : accent);
        keyLabel.setOpaque(true);
        keyLabel.setPreferredSize(new Dimension(32, 18));
        keyLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 11, highlight ? Color.WHITE : accent));
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        textLabel.setForeground(highlight ? Color.WHITE : UIHelper.TEXT_LIGHT);

        chip.add(keyLabel);
        chip.add(iconLabel);
        chip.add(textLabel);

        chip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (action != null) action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!highlight) {
                    chip.setBackground(UIHelper.SELECTION_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!highlight) {
                    chip.setBackground(UIHelper.BG_CARD);
                }
            }
        });

        return chip;
    }
}
