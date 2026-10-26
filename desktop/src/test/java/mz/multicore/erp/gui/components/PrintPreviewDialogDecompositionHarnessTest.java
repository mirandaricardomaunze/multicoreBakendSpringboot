package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de testes e auditoria para a decomposição modular do PrintPreviewDialog.
 */
class PrintPreviewDialogDecompositionHarnessTest {

    private static final String COMPONENTS_PATH = "src/main/java/mz/multicore/erp/gui/components/";

    @Test
    @DisplayName("Garante limites estritos de linhas físicas em todos os componentes de impressão")
    void testLineCountThresholds() throws IOException {
        assertLineCount("PrintPreviewDialog.java", 330);
        assertLineCount("PrintPreviewPaperCanvas.java", 150);
        assertLineCount("PrintPreviewOptionsView.java", 220);
    }

    private void assertLineCount(String fileName, int maxLines) throws IOException {
        File file = new File(COMPONENTS_PATH + fileName);
        assertTrue(file.exists(), "Ficheiro não encontrado: " + file.getAbsolutePath());
        long lines = Files.lines(file.toPath()).count();
        assertTrue(lines <= maxLines,
                String.format("Ficheiro %s excedeu o teto estrito: %d linhas (máximo permitido: %d)", fileName, lines, maxLines));
    }

    @Test
    @DisplayName("Garante 0 chamadas a JOptionPane em todos os ficheiros de pré-visualização de impressão")
    void testZeroJOptionPaneUsage() throws IOException {
        String[] files = {"PrintPreviewDialog.java", "PrintPreviewPaperCanvas.java", "PrintPreviewOptionsView.java"};
        for (String f : files) {
            String content = Files.readString(Path.of(COMPONENTS_PATH, f));
            assertFalse(content.contains("JOptionPane"), "JOptionPane detectado em: " + f);
        }
    }

    @Test
    @DisplayName("Garante a preservação de todas as palavras-chave auditadas por PrintModalHarnessTest")
    void testCanonicalKeywordsPreserved() throws IOException {
        String source = Files.readString(Path.of(COMPONENTS_PATH, "PrintPreviewDialog.java"));
        String[] required = {
                "Impressora", "Nº de cópias", "Páginas", "Orientação", "Ajuste",
                "Escala de cinzentos", "Imprimir", "Guardar PDF", "Cancelar",
                "PaperLayout.paper(", "PdfPrinter.print("
        };
        for (String keyword : required) {
            assertTrue(source.contains(keyword), "Palavra-chave auditada em falta no PrintPreviewDialog: " + keyword);
        }
    }

    @Test
    @DisplayName("Valida instanciação e geometria do PrintPreviewPaperCanvas")
    void testPaperCanvasGeometry() {
        PrintPreviewPaperCanvas.GeometrySource source = new PrintPreviewPaperCanvas.GeometrySource() {
            @Override public double getPageAspect() { return 1.414; }
            @Override public PrintOptions.Orientation getOrientation() { return PrintOptions.Orientation.PORTRAIT; }
            @Override public PrintOptions.Fit getFit() { return PrintOptions.Fit.SHRINK_TO_FIT; }
            @Override public double getZoom() { return 1.0; }
            @Override public java.awt.image.BufferedImage getPageImage() { return null; }
        };

        PrintPreviewPaperCanvas canvas = new PrintPreviewPaperCanvas(source);
        assertNotNull(canvas.getPreferredSize());
        assertTrue(canvas.getPreferredSize().width > 0);
        assertTrue(canvas.getPreferredSize().height > 0);
    }
}
