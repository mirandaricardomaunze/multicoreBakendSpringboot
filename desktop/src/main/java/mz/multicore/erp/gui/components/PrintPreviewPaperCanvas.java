package mz.multicore.erp.gui.components;

import javax.swing.JPanel;
import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Área de desenho da folha de papel na pré-visualização de impressão.
 */
public class PrintPreviewPaperCanvas extends JPanel {

    public interface GeometrySource {
        double getPageAspect();
        PrintOptions.Orientation getOrientation();
        PrintOptions.Fit getFit();
        double getZoom();
        BufferedImage getPageImage();
    }

    private final GeometrySource source;

    public PrintPreviewPaperCanvas(GeometrySource source) {
        this.source = source;
        setBackground(UIHelper.BG_DARK);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension viewport = viewportSize();
        double zoom = source != null ? source.getZoom() : 1.0;
        return new Dimension((int) (viewport.width * zoom), (int) (viewport.height * zoom));
    }

    public int contentWidthHint() {
        Dimension size = getSize();
        if (size.width <= 0 || size.height <= 0) {
            size = getPreferredSize();
        }
        if (size.width <= 0 || size.height <= 0 || source == null) {
            return 0;
        }
        double aspect = source.getPageAspect();
        PrintOptions.Orientation orientation = source.getOrientation();
        Rectangle paper = PaperLayout.paper(size, aspect, orientation, 1.0);
        Rectangle content = PaperLayout.content(paper, PaperLayout.contentAspect(aspect, orientation), source.getFit(), 1.0);
        boolean turned = PaperLayout.rotates(aspect, orientation);
        return Math.max(1, turned ? content.height : content.width);
    }

    private Dimension viewportSize() {
        Component parent = getParent();
        if (parent instanceof JViewport viewport) {
            Dimension extent = viewport.getExtentSize();
            if (extent.width > 0 && extent.height > 0) {
                return extent;
            }
        }
        return new Dimension(560, 640);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (source == null) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double aspect = source.getPageAspect();
        PrintOptions.Orientation orientation = source.getOrientation();
        Rectangle paper = PaperLayout.paper(getSize(), aspect, orientation, 1.0);

        g.setColor(UIHelper.PAPER_SHADOW);
        g.fillRoundRect(paper.x + 4, paper.y + 5, paper.width, paper.height, 6, 6);
        g.setColor(UIHelper.PAPER);
        g.fillRect(paper.x, paper.y, paper.width, paper.height);
        g.setColor(UIHelper.BORDER);
        g.drawRect(paper.x, paper.y, paper.width, paper.height);

        Rectangle content = PaperLayout.content(paper, PaperLayout.contentAspect(aspect, orientation), source.getFit(), 1.0);
        BufferedImage pageImage = source.getPageImage();

        if (pageImage == null) {
            g.setColor(UIHelper.PAPER_INK);
            g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
            String message = "A preparar pré-visualização…";
            int width = g.getFontMetrics().stringWidth(message);
            g.drawString(message, paper.x + (paper.width - width) / 2, paper.y + paper.height / 2);
            g.dispose();
            return;
        }

        Graphics2D page = (Graphics2D) g.create();
        page.clip(paper);
        if (PaperLayout.rotates(aspect, orientation)) {
            AffineTransform transform = new AffineTransform();
            transform.translate(content.getCenterX(), content.getCenterY());
            transform.rotate(Math.PI / 2);
            transform.translate(-content.height / 2.0, -content.width / 2.0);
            page.transform(transform);
            page.drawImage(pageImage, 0, 0, content.height, content.width, null);
        } else {
            page.drawImage(pageImage, content.x, content.y, content.width, content.height, null);
        }
        page.dispose();
        g.dispose();
    }
}
