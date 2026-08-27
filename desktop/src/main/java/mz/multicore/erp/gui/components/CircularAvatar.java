package mz.multicore.erp.gui.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/** Avatar circular reutilizável, com recorte central proporcional e fallback por iniciais. */
public final class CircularAvatar extends JComponent {
    private final int diameter;
    private BufferedImage photo;
    private String initials;
    private boolean cameraOverlay;

    public CircularAvatar(byte[] photoData, String initials, int diameter) {
        this.diameter = diameter;
        this.initials = initials == null || initials.isBlank() ? "?" : initials;
        setPhoto(photoData);
        setPreferredSize(new Dimension(diameter, diameter));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setToolTipText("Fotografia do trabalhador");
    }

    /** Extrai até duas iniciais para o fallback consistente em qualquer perfil. */
    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.trim().split("\\s+");
        String first = words[0].substring(0, 1);
        String last = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public void setPhoto(byte[] photoData) {
        photo = null;
        if (photoData != null && photoData.length > 0) {
            try {
                photo = ImageIO.read(new ByteArrayInputStream(photoData));
            } catch (Exception ignored) {
                // Imagem inválida mantém o fallback por iniciais; não quebra a ficha.
            }
        }
        repaint();
    }

    public void setCameraOverlay(boolean cameraOverlay) {
        this.cameraOverlay = cameraOverlay;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int size = Math.min(getWidth(), getHeight());
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            Shape circle = new Ellipse2D.Float(x + 1, y + 1, size - 2, size - 2);

            g.setClip(circle);
            if (photo != null) {
                double scale = Math.max((double) size / photo.getWidth(), (double) size / photo.getHeight());
                int width = (int) Math.ceil(photo.getWidth() * scale);
                int height = (int) Math.ceil(photo.getHeight() * scale);
                g.drawImage(photo, x + (size - width) / 2, y + (size - height) / 2, width, height, null);
            } else {
                g.setColor(UIHelper.ACCENT_BLUE);
                g.fill(circle);
                g.setColor(Color.WHITE);
                g.setFont(getFont().deriveFont(Font.BOLD, Math.max(18f, size * .28f)));
                FontMetrics metrics = g.getFontMetrics();
                int textX = x + (size - metrics.stringWidth(initials)) / 2;
                int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
                g.drawString(initials, textX, textY);
            }
            g.setClip(null);
            g.setStroke(new BasicStroke(2f));
            g.setColor(UIHelper.BORDER);
            g.draw(circle);

            if (cameraOverlay) {
                paintCamera(g, x + size - 28, y + size - 28);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintCamera(Graphics2D g, int x, int y) {
        g.setColor(new Color(15, 23, 42, 225));
        g.fillOval(x, y, 27, 27);
        g.setColor(Color.WHITE);
        g.fillRoundRect(x + 6, y + 9, 16, 11, 3, 3);
        g.fillRect(x + 10, y + 6, 8, 5);
        g.setColor(new Color(15, 23, 42));
        g.fillOval(x + 11, y + 11, 6, 6);
    }
}
