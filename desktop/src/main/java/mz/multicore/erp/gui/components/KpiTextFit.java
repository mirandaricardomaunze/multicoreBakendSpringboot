package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.IntSupplier;

/** Reduz a letra de um texto de cartão até caber: um valor em dinheiro nunca fica cortado. */
final class KpiTextFit {

    /** Label property ({@link IntSupplier}): the width the text must fit, instead of card width minus reserve. */
    static final String FIT_AVAILABLE = "kpi.fit.available";
    private static final String MAX_FONT = "kpi.fit.max";
    private static final String FIT = "kpi.fit.run";
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
        int installedSize = label.getFont().getSize();
        Runnable fit = () -> {
            int maxSize = label.getClientProperty(MAX_FONT) instanceof Integer size ? size : installedSize;
            int available = label.getClientProperty(FIT_AVAILABLE) instanceof IntSupplier width
                    ? width.getAsInt() : card.getWidth() - reservedWidth.getAsInt();
            apply(label, available, maxSize, minSize);
        };
        label.putClientProperty(FIT, fit);
        label.addPropertyChangeListener("text", event -> fit.run());
        ComponentAdapter onResize = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { fit.run(); }
        };
        card.addComponentListener(onResize);
        label.addComponentListener(onResize);
    }

    /** Sets a new largest font size for a label (e.g. when its card turns compact) and fits it again. */
    static void refit(JLabel label, int maxSize) {
        label.putClientProperty(MAX_FONT, maxSize);
        refit(label);
    }

    static void refit(JLabel label) {
        if (label.getClientProperty(FIT) instanceof Runnable fit) fit.run();
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
