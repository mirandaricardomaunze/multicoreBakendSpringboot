package mz.multicore.erp.gui.components;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parte visual do harness de {@code docs/IMPRESSAO_MODAL_SPEC.md} (IM-20 a IM-22).
 *
 * <p>Prova o que só se descobre ao construir e pintar a janela: que os códigos de ícone existem —
 * {@code UIHelper.icon} rebenta com um código errado e o modal nem chegaria a abrir —, que o
 * esqueleto monta, e que a folha aparece <b>como folha</b>: um rectângulo branco, com a proporção
 * de um A4, recortado contra o fundo da área de pré-visualização. Em tema claro também, onde o
 * cartão por baixo é igualmente branco.</p>
 */
class PrintPreviewDialogPaintTest {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 660;
    /** Linhas de leitura candidatas: o troço branco mais largo de todas é a folha. Assim a
     *  medição não depende de onde caem o texto de espera ou as linhas do documento. */
    private static final double[] SCAN_ROWS = {0.30, 0.36, 0.42, 0.48, 0.54, 0.60, 0.66, 0.72};

    @Test // IM-20
    void oModalMontaComTodosOsIconesEControlos() throws Exception {
        JDialog dialog = PrintPreviewDialog.buildForHarness(onePagePdf(), "fatura-FT2026-14");
        try {
            assertThat(dialog.getTitle()).isEqualTo("Imprimir — Fatura FT2026 14");
            assertThat(dialog.getContentPane().getComponentCount()).isPositive();
            assertThat(dialog.getWidth()).isPositive();
            assertThat(dialog.getHeight()).isPositive();
        } finally {
            dialog.dispose();
        }
    }

    @Test // IM-21
    void aFolhaEDesenhadaComoUmA4RecortadoContraOFundo() throws Exception {
        assertPaperLooksLikeA4(paint(onePagePdf()));
    }

    @Test // IM-22
    void aFolhaContinuaBrancaERecortadaNoTemaClaro() throws Exception {
        Theme original = UIHelper.currentTheme();
        try {
            UIHelper.applyTheme(Theme.LIGHT);
            assertPaperLooksLikeA4(paint(onePagePdf()));
        } finally {
            UIHelper.applyTheme(original);
        }
    }

    // ── verificações ──────────────────────────────────────────────────────────────────────────

    private static void assertPaperLooksLikeA4(BufferedImage painted) {
        Measurement measurement = measurePaper(painted);
        Rectangle paper = measurement.paper();

        assertThat(paper.width).as("largura da folha").isGreaterThan(150);
        assertThat(paper.height).as("altura da folha").isGreaterThan(200);
        assertThat(paper.x).as("a folha não encosta à esquerda — há fundo à volta").isPositive();
        // A branco medido começa dentro do contorno: à esquerda vem a linha da folha e, a seguir,
        // o fundo da área de pré-visualização. É isso que faz a folha parecer uma folha.
        assertThat(painted.getRGB(paper.x - 1, measurement.row()))
                .as("a folha é contornada")
                .isEqualTo(UIHelper.BORDER.getRGB());
        assertThat(painted.getRGB(paper.x - 2, measurement.row()))
                .as("à esquerda da folha está o fundo da área de pré-visualização")
                .isEqualTo(UIHelper.BG_DARK.getRGB());

        double aspect = (double) paper.width / paper.height;
        assertThat(aspect).as("proporção da folha desenhada (A4 em retrato)")
                .isCloseTo(PaperLayout.A4_ASPECT, org.assertj.core.data.Offset.offset(0.06));
    }

    /** A folha medida na imagem e a linha em que foi encontrada. */
    private record Measurement(Rectangle paper, int row) {
    }

    /** Mede a folha na imagem: o maior troço branco das linhas de leitura, e a sua altura. */
    private static Measurement measurePaper(BufferedImage image) {
        int paper = UIHelper.PAPER.getRGB();
        int limit = (int) (WIDTH * 0.62); // metade esquerda: a área de pré-visualização

        int bestRow = -1;
        int bestStart = -1;
        int bestLength = 0;
        for (double fraction : SCAN_ROWS) {
            int row = (int) (HEIGHT * fraction);
            int runStart = -1;
            for (int x = 0; x < limit; x++) {
                boolean white = image.getRGB(x, row) == paper;
                if (white && runStart < 0) {
                    runStart = x;
                }
                if ((!white || x == limit - 1) && runStart >= 0) {
                    int length = (white ? x + 1 : x) - runStart;
                    if (length > bestLength) {
                        bestLength = length;
                        bestStart = runStart;
                        bestRow = row;
                    }
                    runStart = -1;
                }
            }
        }
        if (bestStart < 0) {
            throw new AssertionError("Nenhuma folha branca encontrada na pré-visualização.");
        }

        int column = bestStart + 8; // junto à margem esquerda da folha: sem texto pelo meio
        int top = bestRow;
        while (top > 0 && image.getRGB(column, top - 1) == paper) {
            top--;
        }
        int bottom = bestRow;
        while (bottom < HEIGHT - 1 && image.getRGB(column, bottom + 1) == paper) {
            bottom++;
        }
        return new Measurement(new Rectangle(bestStart, top, bestLength, bottom - top + 1), bestRow);
    }

    // ── auxiliares ────────────────────────────────────────────────────────────────────────────

    private static BufferedImage paint(byte[] pdf) {
        JDialog dialog = PrintPreviewDialog.buildForHarness(pdf, "fatura-FT2026-14");
        try {
            JComponent content = (JComponent) dialog.getContentPane();
            content.setSize(WIDTH, HEIGHT);
            layoutTree(content);

            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            content.paint(graphics);
            graphics.dispose();
            return image;
        } finally {
            dialog.dispose();
        }
    }

    /** Sem janela visível, o Swing não distribui o espaço: força-se a disposição em cascata. */
    private static void layoutTree(Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutTree(nested);
            }
        }
    }

    private static byte[] onePagePdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("Factura de teste"));
        document.close();
        return output.toByteArray();
    }
}
