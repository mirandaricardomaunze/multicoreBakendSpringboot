package mz.multicore.erp.gui;

import javax.imageio.ImageIO;
import mz.multicore.erp.gui.components.UIHelper;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Painel de fundo do LoginDialog que renderiza a imagem com preservação de proporção ('cover')
 * e sobreposição elegante de profundidade e contraste.
 */
class LoginBackgroundPanel extends JPanel {

    private static final BufferedImage BG_IMAGE = loadBackgroundImage();

    LoginBackgroundPanel() {
        super();
        setOpaque(true);
    }

    private static BufferedImage loadBackgroundImage() {
        try (InputStream is = LoginBackgroundPanel.class.getResourceAsStream("/images/login-bg.jpg")) {
            if (is != null) return ImageIO.read(is);
        } catch (Exception ex) {
            System.err.println("[MULTICORE] Imagem de fundo não carregada: " + ex.getMessage());
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();

        if (BG_IMAGE != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double imgW = BG_IMAGE.getWidth(), imgH = BG_IMAGE.getHeight();
            double scale = Math.max((double) w / imgW, (double) h / imgH);
            int drawW = (int) Math.round(imgW * scale), drawH = (int) Math.round(imgH * scale);
            int x = (w - drawW) / 2, y = (h - drawH) / 2;

            g2.drawImage(BG_IMAGE, x, y, drawW, drawH, null);

            // Sobreposição translúcida luminosa clara para tema claro acolhedor
            g2.setColor(UIHelper.withAlpha(Color.WHITE, 80));
            g2.fillRect(0, 0, w, h);

            // Gradiente vertical subtil para claridade, suavidade e elegância visual
            GradientPaint gp = new GradientPaint(
                    0, 0, UIHelper.withAlpha(Color.WHITE, 50),
                    0, h, UIHelper.withAlpha(UIHelper.BG_DARK, 140)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        } else {
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, UIHelper.BG_CARD, 0, h, UIHelper.BG_DARK);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }
}
