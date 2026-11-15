package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

/**
 * Emblema 3D da aplicação no topo da barra lateral (Sidebar).
 */
public class SidebarLogoBadge extends JComponent {

    private final Image iconImage;

    public SidebarLogoBadge(int size) {
        setPreferredSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        this.iconImage = UIHelper.getAppIcon(size - 6);
        setOpaque(false);
        setToolTipText("Multicore ERP");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            Color bgTop = ThemeColors.subtleFill();
            Color bgBot = UIHelper.TABLE_HEADER_BG;
            g2.setPaint(new GradientPaint(0, 0, bgTop, 0, h, bgBot));
            g2.fillRoundRect(0, 0, w, h, 8, 8);

            g2.setColor(ThemeColors.hairline());
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            if (iconImage != null) {
                int ix = (w - iconImage.getWidth(null)) / 2;
                int iy = (h - iconImage.getHeight(null)) / 2;
                g2.drawImage(iconImage, ix, iy, null);
            }
        } finally {
            g2.dispose();
        }
    }
}
