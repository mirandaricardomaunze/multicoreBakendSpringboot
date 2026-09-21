package mz.multicore.erp.gui;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import mz.multicore.erp.gui.components.PaperLayout;
import mz.multicore.erp.gui.components.PdfPreviewDocument;
import mz.multicore.erp.gui.components.PdfPrinter;
import mz.multicore.erp.gui.components.PrintOptions;
import mz.multicore.erp.gui.components.PrintOptionsStore;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Harness permanente do SPEC {@code docs/IMPRESSAO_MODAL_SPEC.md}.
 *
 * <p>A regra que este harness protege é uma só: <b>nenhum documento sai do ERP sem passar pelo
 * modal de impressão</b>. O resto — intervalos, cópias, posição no papel — é validado nas classes
 * que não precisam de janela para serem testadas.</p>
 */
class PrintModalHarnessTest {

    private static final Path GUI = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    /** Ecrãs que imprimem. Cada um tem de chamar o modal — se um deixar de o fazer, isto parte. */
    private static final List<String> PRINTING_SCREENS = List.of(
            "ComercialPanel.java",
            "commercial/CommercialNotesPanel.java",
            "commercial/DeliveryGuidesPanel.java",
            "commercial/OrderDetailsDialog.java",
            "commercial/QuotationsPanel.java",
            "CrmWorkSheetActions.java",
            "CustomerOrderFulfillmentActions.java",
            "FiscalPanel.java",
            "HRContractsPanel.java",
            "HRPanel.java",
            "HRTerminationsPanel.java",
            "PosCashSessionActions.java",
            "POSPanel.java",
            "PosSalesHistoryPanel.java",
            "StockBatchesPanel.java",
            "StockInventoryCountActions.java",
            "StockPanel.java",
            "StockTransferActions.java");

    // ── IM-01 / IM-02: o modal é o único caminho para o papel ─────────────────────────────────

    @Test
    void nenhumEcraGravaOPdfDirectamenteSemPassarPeloModal() throws IOException {
        try (var paths = Files.walk(GUI)) {
            List<Path> screens = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("components"))
                    .toList();
            for (Path screen : screens) {
                assertThat(Files.readString(screen))
                        .as("%s tem de imprimir pelo PrintPreviewDialog, não pelo PdfFileSaver", screen)
                        .doesNotContain("PdfFileSaver");
            }
        }
    }

    @Test
    void todosOsEcrasQueImprimemAbremOModal() throws IOException {
        for (String screen : PRINTING_SCREENS) {
            Path path = GUI.resolve(screen.replace("/", java.io.File.separator));
            assertThat(path).as("ecrã de impressão %s", screen).exists();
            String source = Files.readString(path);
            // Ou o ecrã abre o modal, ou delega numa peça que o abre. A exportação de listagens
            // vive toda no TableExportAction — que também passa pelo modal, e que o CE-05 prende —,
            // e um ecrã que só exporta listagens não tem de repetir a chamada.
            assertThat(source.contains("PrintPreviewDialog.show(")
                    || source.contains("TableExportAction.export("))
                    .as("%s deixou de abrir o modal de impressão", screen)
                    .isTrue();
        }
    }

    @Test
    void aExportacaoDeListagensAbreOModalNumSoSitio() throws IOException {
        String source = Files.readString(GUI.resolve(Path.of("components", "TableExportAction.java")));
        assertThat(source).as("a listagem exportada também passa pelo modal")
                .contains("PrintPreviewDialog.show(");
    }

    @Test
    void oModalOfereceImpressoraCopiasPosicaoEPreVisualizacao() throws IOException {
        String source = Files.readString(GUI.resolve(Path.of("components", "PrintPreviewDialog.java")));
        assertThat(source).contains("Impressora", "Nº de cópias", "Páginas", "Orientação", "Ajuste",
                "Escala de cinzentos", "Imprimir", "Guardar PDF", "Cancelar");
        assertThat(source).as("a pré-visualização desenha a folha").contains("PaperLayout.paper(");
        assertThat(source).as("o envio para a fila é do PdfPrinter").contains("PdfPrinter.print(");
    }

    // ── IM-03: cópias e intervalo de páginas ──────────────────────────────────────────────────

    @Test
    void asCopiasFicamSempreDentroDosLimites() {
        assertThat(PrintOptions.defaults("HP").copies()).isEqualTo(1);
        assertThat(withRange("").copies()).isEqualTo(1);
        assertThat(new PrintOptions("HP", 0, null, null, "", false).copies()).isEqualTo(1);
        assertThat(new PrintOptions("HP", 500, null, null, "", false).copies())
                .isEqualTo(PrintOptions.MAX_COPIES);
    }

    @Test
    void intervaloVazioImprimeODocumentoInteiro() {
        assertThat(withRange("").printsEveryPage()).isTrue();
        assertThat(withRange("").resolvePages(3)).containsExactly(0, 1, 2);
    }

    @Test
    void intervaloAceitaPaginasSoltasEGamas() {
        assertThat(withRange("1,3-5").resolvePages(6)).containsExactly(0, 2, 3, 4);
        assertThat(withRange(" 2 , 2 , 1 ").resolvePages(4)).containsExactly(0, 1);
    }

    @Test
    void intervaloRecusaOQueNaoExisteEExplicaPorque() {
        assertThatThrownBy(() -> withRange("7").resolvePages(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 página(s)");
        assertThatThrownBy(() -> withRange("abc").resolvePages(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1,3-5");
        assertThatThrownBy(() -> withRange("5-2").resolvePages(9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invertido");
        assertThatThrownBy(() -> withRange(",").resolvePages(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos uma página");
    }

    @Test
    void oResumoDizOQueVaiSairNoPapel() {
        PrintOptions options = new PrintOptions("HP", 2, PrintOptions.Orientation.LANDSCAPE,
                PrintOptions.Fit.SHRINK_TO_FIT, "", true);
        assertThat(options.summary(3)).isEqualTo("3 páginas · 2 cópias · Paisagem · Escala de cinzentos");
        assertThat(PrintOptions.defaults("HP").summary(1)).isEqualTo("1 página · 1 cópia · Automática");
    }

    // ── IM-04: posição do documento no papel ──────────────────────────────────────────────────

    @Test
    void aPosicaoViraAFolhaDoProprioDocumentoEmVezDeAForcarA4() {
        double a4 = PaperLayout.A4_ASPECT;          // retrato: 0,707
        double landscape = 1 / a4;                   // paisagem: 1,414

        assertThat(PaperLayout.paperAspect(a4, PrintOptions.Orientation.AUTO)).isEqualTo(a4);
        assertThat(PaperLayout.paperAspect(a4, PrintOptions.Orientation.PORTRAIT)).isEqualTo(a4);
        assertThat(PaperLayout.paperAspect(a4, PrintOptions.Orientation.LANDSCAPE))
                .isCloseTo(landscape, org.assertj.core.data.Offset.offset(1e-9));

        // Um recibo térmico (80 mm de largura, muito comprido) continua estreito em retrato.
        double receipt = 80.0 / 300.0;
        assertThat(PaperLayout.paperAspect(receipt, PrintOptions.Orientation.PORTRAIT)).isEqualTo(receipt);
    }

    @Test
    void aPaginaSoRodaQuandoAFolhaMudaDeLado() {
        double a4 = PaperLayout.A4_ASPECT;
        assertThat(PaperLayout.rotates(a4, PrintOptions.Orientation.AUTO)).isFalse();
        assertThat(PaperLayout.rotates(a4, PrintOptions.Orientation.PORTRAIT)).isFalse();
        assertThat(PaperLayout.rotates(a4, PrintOptions.Orientation.LANDSCAPE)).isTrue();
        assertThat(PaperLayout.contentAspect(a4, PrintOptions.Orientation.LANDSCAPE))
                .isCloseTo(1 / a4, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void aFolhaFicaCentradaEAPaginaNuncaSaiDaFolha() {
        Dimension area = new Dimension(800, 600);
        Rectangle paper = PaperLayout.paper(area, PaperLayout.A4_ASPECT,
                PrintOptions.Orientation.PORTRAIT, 1.0);

        assertThat(paper.width).isPositive();
        assertThat(paper.height).isLessThanOrEqualTo(area.height);
        assertThat(paper.x + paper.width).isLessThanOrEqualTo(area.width);
        assertThat(Math.abs((area.width - paper.width) / 2 - paper.x)).isLessThanOrEqualTo(1);

        Rectangle fitted = PaperLayout.content(paper, PaperLayout.A4_ASPECT,
                PrintOptions.Fit.SHRINK_TO_FIT, 1.0);
        assertThat(paper.contains(fitted)).as("ajustada à página, a página cabe na folha").isTrue();
        assertThat(fitted.width).isLessThan(paper.width);

        Rectangle actual = PaperLayout.content(paper, PaperLayout.A4_ASPECT,
                PrintOptions.Fit.ACTUAL_SIZE, 1.0);
        assertThat(actual.width).as("tamanho real ocupa a folha toda").isEqualTo(paper.width);
    }

    @Test
    void aFolhaCresceComOZoom() {
        Dimension area = new Dimension(800, 600);
        Rectangle normal = PaperLayout.paper(area, PaperLayout.A4_ASPECT,
                PrintOptions.Orientation.PORTRAIT, 1.0);
        Rectangle zoomed = PaperLayout.paper(area, PaperLayout.A4_ASPECT,
                PrintOptions.Orientation.PORTRAIT, 2.0);
        assertThat(zoomed.width).isGreaterThan(normal.width);
        assertThat(zoomed.height).isGreaterThan(normal.height);
    }

    // ── IM-05: pré-visualização ───────────────────────────────────────────────────────────────

    @Test
    void aPreVisualizacaoLeAsPaginasEConheceAFormaDeCadaUma() throws Exception {
        try (PdfPreviewDocument preview = PdfPreviewDocument.open(twoPagePdf())) {
            assertThat(preview.pageCount()).isEqualTo(2);
            assertThat(preview.aspect(0)).as("primeira página em retrato").isLessThan(1.0);
            assertThat(preview.aspect(1)).as("segunda página em paisagem").isGreaterThan(1.0);

            BufferedImage rendered = preview.render(0, 420);
            assertThat(rendered.getWidth()).isBetween(400, 440);
            assertThat(rendered.getHeight()).isGreaterThan(rendered.getWidth());
            assertThat(preview.render(0, 420)).as("segunda leitura vem da cache").isSameAs(rendered);
        }
    }

    @Test
    void umPdfIlegivelNaoFicaEmSilencio() {
        assertThatThrownBy(() -> PdfPreviewDocument.open(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
        assertThatThrownBy(() -> PdfPreviewDocument.open("isto não é um PDF".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── IM-06: impressoras e memória das escolhas ─────────────────────────────────────────────

    @Test
    void aListaDeImpressorasNuncaRebenta() {
        assertThat(PdfPrinter.printerNames()).isNotNull();
        assertThat(PdfPrinter.hasPrinters()).isEqualTo(!PdfPrinter.printerNames().isEmpty());
    }

    @Test
    void imprimirSemDocumentoERecusado() {
        assertThatThrownBy(() -> PdfPrinter.print(new byte[0], PrintOptions.defaults(null), "teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void asEscolhasSaoLembradasPorFamiliaDeDocumento() {
        assertThat(PrintOptionsStore.familyOf("fatura-FT2026-14")).isEqualTo("fatura");
        assertThat(PrintOptionsStore.familyOf("guia-remessa-2026/14")).isEqualTo("guia-remessa");
        assertThat(PrintOptionsStore.familyOf("recibo-salario-RS12")).isEqualTo("recibo-salario");
        assertThat(PrintOptionsStore.familyOf("etiquetas")).isEqualTo("etiquetas");
        assertThat(PrintOptionsStore.familyOf("")).isEqualTo("documento");
        assertThat(PrintOptionsStore.familyOf(null)).isEqualTo("documento");
    }

    @Test
    void semPreferenciasDisponiveisAImpressaoContinuaAFuncionar() {
        PrintOptionsStore store = PrintOptionsStore.inMemory();
        store.save("fatura-1", new PrintOptions("HP", 4, PrintOptions.Orientation.LANDSCAPE,
                PrintOptions.Fit.ACTUAL_SIZE, "", true));
        PrintOptions loaded = store.load("fatura-1", "Canon");
        assertThat(loaded.printerName()).isEqualTo("Canon");
        assertThat(loaded.copies()).isEqualTo(1);
        assertThat(loaded.orientation()).isEqualTo(PrintOptions.Orientation.AUTO);
    }

    @Test
    void oIntervaloDePaginasNaoEHerdadoDeOutroDocumento() {
        PrintOptions loaded = PrintOptionsStore.inMemory().load("fatura-1", "HP");
        assertThat(loaded.printsEveryPage())
                .as("o intervalo é do documento concreto, não da família")
                .isTrue();
    }

    // ── auxiliares ────────────────────────────────────────────────────────────────────────────

    private static PrintOptions withRange(String range) {
        return new PrintOptions("HP", 1, PrintOptions.Orientation.AUTO,
                PrintOptions.Fit.SHRINK_TO_FIT, range, false);
    }

    /** Duas páginas — a primeira em retrato, a segunda em paisagem. */
    private static byte[] twoPagePdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("Documento de teste — página 1"));
        document.setPageSize(PageSize.A4.rotate());
        document.newPage();
        document.add(new Paragraph("Documento de teste — página 2"));
        document.close();
        return output.toByteArray();
    }
}
