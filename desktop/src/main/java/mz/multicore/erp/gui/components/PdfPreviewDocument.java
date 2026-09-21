package mz.multicore.erp.gui.components;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O PDF já gerado, aberto apenas para <b>ser visto</b>: número de páginas, forma de cada página e
 * imagem de uma página a uma largura pedida.
 *
 * <p>Não imprime (isso é do {@link PdfPrinter}) e não sabe desenhar janelas (isso é do
 * {@link PrintPreviewDialog}). Guarda em cache as últimas páginas desenhadas para que folhear o
 * documento não volte a pagar o custo do render.</p>
 *
 * <p><b>Uma thread de cada vez.</b> O {@code PDDocument} do PDFBox não é seguro para acesso
 * concorrente; o diálogo desenha sempre a partir de um único worker de fundo, e a impressão usa a
 * sua própria cópia do documento.</p>
 */
public final class PdfPreviewDocument implements AutoCloseable {

    private static final int CACHE_SIZE = 6;
    private static final int MAX_RENDER_WIDTH = 2200;

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final Map<String, BufferedImage> cache = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private PdfPreviewDocument(PDDocument document) {
        this.document = document;
        this.renderer = new PDFRenderer(document);
    }

    /** Abre os bytes de um PDF. Lança {@link IllegalArgumentException} se não for um PDF legível. */
    public static PdfPreviewDocument open(byte[] pdf) {
        if (pdf == null || pdf.length == 0) {
            throw new IllegalArgumentException("O documento veio vazio — nada para imprimir.");
        }
        try {
            return new PdfPreviewDocument(Loader.loadPDF(pdf));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler o PDF: " + ex.getMessage(), ex);
        }
    }

    public int pageCount() {
        return document.getNumberOfPages();
    }

    /** Dimensão da página em pontos, já com a rotação declarada no PDF aplicada. */
    public Dimension2D sizeInPoints(int pageIndex) {
        PDPage page = document.getPage(clamp(pageIndex));
        PDRectangle box = page.getCropBox();
        boolean quarterTurn = (page.getRotation() / 90) % 2 != 0;
        double width = quarterTurn ? box.getHeight() : box.getWidth();
        double height = quarterTurn ? box.getWidth() : box.getHeight();
        return new Size(width, height);
    }

    /** Proporção largura/altura da página, já rodada. */
    public double aspect(int pageIndex) {
        Dimension2D size = sizeInPoints(pageIndex);
        return size.getHeight() <= 0 ? PaperLayout.A4_ASPECT : size.getWidth() / size.getHeight();
    }

    /** Desenha a página a {@code targetWidth} pixéis de largura. Chamar fora do EDT. */
    public BufferedImage render(int pageIndex, int targetWidth) {
        int page = clamp(pageIndex);
        int width = Math.max(120, Math.min(MAX_RENDER_WIDTH, targetWidth));
        String key = page + "@" + width;
        BufferedImage cached = cache.get(key);
        if (cached != null) return cached;
        try {
            double points = sizeInPoints(page).getWidth();
            float scale = points <= 0 ? 1f : (float) (width / points);
            BufferedImage image = renderer.renderImage(page, scale);
            cache.put(key, image);
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível desenhar a página "
                    + (page + 1) + ": " + ex.getMessage(), ex);
        }
    }

    private int clamp(int pageIndex) {
        return Math.max(0, Math.min(pageCount() - 1, pageIndex));
    }

    @Override
    public void close() {
        cache.clear();
        try {
            document.close();
        } catch (IOException ignored) {
            // Fechar a pré-visualização nunca pode rebentar a UI.
        }
    }

    /** {@link Dimension2D} concreta — a JDK só traz a variante de inteiros pública. */
    private static final class Size extends Dimension2D {
        private double width;
        private double height;

        private Size(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override public double getWidth() { return width; }

        @Override public double getHeight() { return height; }

        @Override public void setSize(double w, double h) { this.width = w; this.height = h; }
    }
}
