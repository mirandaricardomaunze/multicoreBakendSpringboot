package mz.multicore.erp.gui.components;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Assistente de acessibilidade e interatividade para cartões de KPI (hover e drilldown). */
final class KpiCardInteractiveHelper {

    private KpiCardInteractiveHelper() {}

    static ModernPanel makeInteractive(ModernPanel card, String tooltip, Runnable onClickAction) {
        if (card == null || onClickAction == null) return card;
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setFocusable(true);
        card.setToolTipText(tooltip != null && !tooltip.isBlank() ? tooltip : "Clique para filtrar ou ver detalhes deste indicador");

        Color normalBorder = (Color) card.getClientProperty("card.border");
        Color hoverBorder = normalBorder != null ? normalBorder.brighter() : new Color(255, 255, 255, 140);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.putClientProperty("card.border", hoverBorder);
                card.repaint();
            }

            @Override public void mouseExited(MouseEvent e) {
                card.putClientProperty("card.border", normalBorder);
                card.repaint();
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) onClickAction.run();
            }
        });

        card.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) onClickAction.run();
            }
        });

        return card;
    }
}
