package mz.multicore.erp.gui.components;

import javax.swing.Icon;
import javax.swing.JTextField;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * Campo de leitura rápida e contínua para leitor de código de barras ou SKU (Bipador).
 * Suporta Look&Feel nativo e FlatLaf, com ícone vetorial de código de barras e atalho F2.
 */
public class BarcodeScanField extends JTextField {

    private static final int ICON_SIZE = 14;
    private static final int PAD_LEFT = 26;

    private final Icon icon = UIHelper.icon("fas-barcode", ICON_SIZE, UIHelper.ACCENT_BLUE);
    private final String hint;

    public BarcodeScanField(String hint, Consumer<String> onBarcodeScanned) {
        this.hint = hint == null ? "Bipar código / SKU (F2)..." : ModernButton.stripEllipsis(hint);
        UIHelper.styleTextField(this);
        putClientProperty("JTextField.placeholderText", this.hint);
        putClientProperty("JTextField.leadingIcon", icon);
        putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        if (!(javax.swing.UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLaf)) {
            Insets m = getMargin();
            setMargin(new Insets(m == null ? 0 : m.top, PAD_LEFT, m == null ? 0 : m.bottom, m == null ? 8 : m.right));
        }
        setColumns(16);
        setToolTipText("Bipar ou introduzir código de barras/SKU e pressionar Enter. Atalho rápido: F2.");

        addActionListener(e -> {
            String code = getText() == null ? "" : getText().trim();
            if (!code.isEmpty() && onBarcodeScanned != null) {
                onBarcodeScanned.accept(code);
                setText("");
                requestFocusInWindow();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (javax.swing.UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLaf) {
            return;
        }
        int iconY = (getHeight() - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g, 7, iconY);
        if (getText().isEmpty() && !hint.isEmpty()) {
            g.setColor(UIHelper.TEXT_MUTED);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(hint, PAD_LEFT, ty);
        }
    }
}
