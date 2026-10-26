package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição de RecordDetailsDialog (Fase 85)")
class RecordDetailsDialogDecompositionHarnessTest {

    private static final Path SOURCE_DIR = Path.of("src/main/java/mz/multicore/erp/gui/components");

    @Test
    @DisplayName("Garante limites físicos estritos para RecordDetailsDialog e colaboradores")
    void testPhysicalLineLimits() throws IOException {
        Path dialogFile = SOURCE_DIR.resolve("RecordDetailsDialog.java");
        Path propExtractorFile = SOURCE_DIR.resolve("RecordDetailsPropertyExtractor.java");
        Path exporterFile = SOURCE_DIR.resolve("RecordDetailsExporter.java");
        Path tabBuilderFile = SOURCE_DIR.resolve("RecordDetailsTabBuilder.java");

        assertTrue(Files.exists(dialogFile), "RecordDetailsDialog.java deve existir");
        assertTrue(Files.exists(propExtractorFile), "RecordDetailsPropertyExtractor.java deve existir");
        assertTrue(Files.exists(exporterFile), "RecordDetailsExporter.java deve existir");
        assertTrue(Files.exists(tabBuilderFile), "RecordDetailsTabBuilder.java deve existir");

        List<String> dialogLines = Files.readAllLines(dialogFile);
        List<String> extractorLines = Files.readAllLines(propExtractorFile);
        List<String> exporterLines = Files.readAllLines(exporterFile);
        List<String> tabBuilderLines = Files.readAllLines(tabBuilderFile);

        assertTrue(dialogLines.size() <= 225, "RecordDetailsDialog deve ter <= 225 linhas (atual: " + dialogLines.size() + ")");
        assertTrue(extractorLines.size() <= 170, "RecordDetailsPropertyExtractor deve ter <= 170 linhas (atual: " + extractorLines.size() + ")");
        assertTrue(exporterLines.size() <= 150, "RecordDetailsExporter deve ter <= 150 linhas (atual: " + exporterLines.size() + ")");
        assertTrue(tabBuilderLines.size() <= 95, "RecordDetailsTabBuilder deve ter <= 95 linhas (atual: " + tabBuilderLines.size() + ")");
    }

    @Test
    @DisplayName("Proíbe o uso de JOptionPane nos componentes do inspetor de registos")
    void testNoJOptionPaneUsage() throws IOException {
        Path dialogFile = SOURCE_DIR.resolve("RecordDetailsDialog.java");
        Path propExtractorFile = SOURCE_DIR.resolve("RecordDetailsPropertyExtractor.java");
        Path exporterFile = SOURCE_DIR.resolve("RecordDetailsExporter.java");
        Path tabBuilderFile = SOURCE_DIR.resolve("RecordDetailsTabBuilder.java");

        for (Path p : List.of(dialogFile, propExtractorFile, exporterFile, tabBuilderFile)) {
            String content = Files.readString(p);
            assertFalse(content.contains("JOptionPane"), "Ficheiro " + p.getFileName() + " não pode conter JOptionPane");
        }
    }

    @Test
    @DisplayName("Garante que contratos públicos essenciais de RecordDetailsDialog estão preservados")
    void testPublicContractsPreserved() {
        assertDoesNotThrow(() -> {
            Class<?> itemRecord = RecordDetailsDialog.PropertyItem.class;
            assertTrue(itemRecord.isRecord());

            RecordDetailsDialog.class.getMethod("show", JTable.class);
            RecordDetailsDialog.class.getMethod("buildPropertySheet", List.class);
            RecordDetailsDialog.class.getMethod("valueText", Object.class);
            RecordDetailsDialog.class.getMethod("isHidden", JTable.class, int.class);
            RecordDetailsDialog.class.getMethod("findProduct", JTable.class, int.class);
            RecordDetailsDialog.class.getMethod("findClient", JTable.class, int.class);
            RecordDetailsDialog.class.getMethod("findOrder", JTable.class, int.class);
        });
    }

    @Test
    @DisplayName("Garante funcionamento e sanitização do buildPropertySheet e valueText")
    void testPropertySheetAndValueText() {
        var items = List.of(
                new RecordDetailsDialog.PropertyItem("Código", "ART-001"),
                new RecordDetailsDialog.PropertyItem("Preço", "1.500,00 MT")
        );
        var component = RecordDetailsDialog.buildPropertySheet(items);
        assertNotNull(component);

        assertEquals("Texto", RecordDetailsDialog.valueText("Texto"));
        assertEquals("", RecordDetailsDialog.valueText(null));

        DefaultTableModel model = new DefaultTableModel(new String[]{"ColA"}, 0);
        JTable table = new JTable(model);
        assertFalse(RecordDetailsDialog.isHidden(table, 0));
    }
}
