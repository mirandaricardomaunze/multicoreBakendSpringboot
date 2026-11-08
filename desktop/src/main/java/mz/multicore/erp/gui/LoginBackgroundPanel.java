package mz.multicore.erp.gui;

import javax.imageio.ImageIO;
import mz.multicore.erp.gui.components.UIHelper;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Painel de fundo do LoginDialog que renderiza a imagem com preservação de proporção ('cover')
 * e sobreposição escura (azul-marinho) para contraste com o cartão de login.
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

            // Camada escura na cor da marca: dá contraste ao cartão branco e ao rodapé
            GradientPaint gp = new GradientPaint(
                    0, 0, UIHelper.withAlpha(UIHelper.KPI_NEUTRAL_DARK, 175),
                    w, h, UIHelper.withAlpha(UIHelper.KPI_INFO_DARK, 200)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        } else {
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, UIHelper.KPI_NEUTRAL_DARK, w, h, UIHelper.KPI_INFO_DARK);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }
}
