package mz.multicore.erp.gui.components;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IM-23 a IM-25 do harness de {@code docs/IMPRESSAO_MODAL_SPEC.md}.
 *
 * <p><b>A folha no ecrã e a folha na bandeja têm de ser a mesma.</b> O {@code PDFPrintable} não
 * roda páginas: mandar um documento em pé para uma folha deitada dá um documento pequeno ao meio
 * com margens enormes — nada parecido com a pré-visualização. Aqui prova-se que o
 * {@code PdfPrinter} vira as páginas antes de as enviar, e que decide virar exactamente com a
 * mesma regra que a pré-visualização usa para desenhar.</p>
 */
class PdfPrinterOrientationTest {

    @Test // IM-23
    void paisagemViraUmaPaginaEmPeAntesDeAEnviar() throws Exception {
        try (PDDocument document = Loader.loadPDF(portraitPdf())) {
            assertThat(PdfPrinter.aspectOf(document.getPage(0))).isLessThan(1.0);

            PdfPrinter.turnPages(document, PrintOptions.Orientation.LANDSCAPE);

            assertThat(document.getPage(0).getRotation()).isEqualTo(90);
            assertThat(PdfPrinter.aspectOf(document.getPage(0)))
                    .as("depois de virada, a página é mais larga do que alta")
                    .isGreaterThan(1.0);
        }
    }

    @Test // IM-24
    void retratoNaoMexeNumaPaginaQueJaEstaEmPeEAutomaticoNuncaMexe() throws Exception {
        try (PDDocument document = Loader.loadPDF(portraitPdf())) {
            PdfPrinter.turnPages(document, PrintOptions.Orientation.PORTRAIT);
            assertThat(document.getPage(0).getRotation()).isZero();

            PdfPrinter.turnPages(document, PrintOptions.Orientation.AUTO);
            assertThat(document.getPage(0).getRotation()).isZero();
        }
    }

    @Test // IM-25
    void oImpressorEAPreVisualizacaoDecidemVirarComAMesmaRegra() throws Exception {
        try (PDDocument document = Loader.loadPDF(portraitPdf())) {
            double aspect = PdfPrinter.aspectOf(document.getPage(0));

            for (PrintOptions.Orientation orientation : PrintOptions.Orientation.values()) {
                boolean previewTurns = PaperLayout.rotates(aspect, orientation);

                try (PDDocument copy = Loader.loadPDF(portraitPdf())) {
                    PdfPrinter.turnPages(copy, orientation);
                    boolean printerTurned = copy.getPage(0).getRotation() != 0;
                    assertThat(printerTurned)
                            .as("%s: o que se vê e o que sai têm de concordar", orientation)
                            .isEqualTo(previewTurns && orientation != PrintOptions.Orientation.AUTO);
                }
            }
        }
    }

    private static byte[] portraitPdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("Documento em pé"));
        document.close();
        return output.toByteArray();
    }
}
