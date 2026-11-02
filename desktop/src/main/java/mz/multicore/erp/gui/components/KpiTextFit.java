package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.IntSupplier;

/** Reduz a letra de um texto de cartão até caber: um valor em dinheiro nunca fica cortado. */
final class KpiTextFit {

    private static final int CARD_PADDING = 36;
    private static final int ICON_WIDTH = 52;
    private static final int MIN_VALUE_SIZE = 11;
    private static final int MIN_NOTE_SIZE = 9;

    private KpiTextFit() {}

    /** Liga os textos do cartão ao ajuste e devolve o cartão, pronto a usar. */
    static ModernPanel wire(ModernPanel card, JLabel title, JLabel value, mz.multicore.erp.gui.components.TrendBadge trend,
                            JLabel note, boolean hasIcon) {
        int reserved = CARD_PADDING + (hasIcon ? ICON_WIDTH : 0);
        if (title != null) install(title, card, () -> reserved, MIN_NOTE_SIZE - 1);
        if (note != null) install(note, card, () -> reserved, MIN_NOTE_SIZE);
        if (value != null) {
            install(value, card, () -> reserved + (trend == null ? 0 : trend.getPreferredSize().width + 8), MIN_VALUE_SIZE);
        }
        return card;
    }

    static void install(JLabel label, JComponent card, IntSupplier reservedWidth, int minSize) {
        int maxSize = label.getFont().getSize();
        Runnable fit = () -> apply(label, card.getWidth() - reservedWidth.getAsInt(), maxSize, minSize);
        label.addPropertyChangeListener("text", event -> fit.run());
        card.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { fit.run(); }
        });
    }

    static void apply(JLabel label, int available, int maxSize, int minSize) {
        String text = label.getText();
        if (available <= 0 || text == null || text.startsWith("<html")) return;
        Font font = label.getFont();
        int size = maxSize;
        while (size > minSize && label.getFontMetrics(font.deriveFont((float) size)).stringWidth(text) > available) {
            size--;
        }
        if (size != font.getSize()) label.setFont(font.deriveFont((float) size));
        boolean stillCut = label.getFontMetrics(label.getFont()).stringWidth(text) > available;
        label.setToolTipText(stillCut ? text : null);
    }
}
