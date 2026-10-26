package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: HRTimeSheetPanel")
class HRTimeSheetPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("TIMESHEET-01: HRTimeSheetPanel.java deve possuir <= 165 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("HRTimeSheetPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165,
                () -> "HRTimeSheetPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("TIMESHEET-02: Diálogos especializados dentro dos limites de linhas")
    void testCollaboratorLineCounts() throws IOException {
        Path entry = ROOT.resolve("HrTimeEntryDialog.java");
        List<String> entryLines = Files.readAllLines(entry);
        assertTrue(entryLines.size() <= 90,
                () -> "HrTimeEntryDialog.java deve possuir <= 90 linhas. Atual: " + entryLines.size());

        Path overtime = ROOT.resolve("HrOvertimeRatesDialog.java");
        List<String> overtimeLines = Files.readAllLines(overtime);
        assertTrue(overtimeLines.size() <= 110,
                () -> "HrOvertimeRatesDialog.java deve possuir <= 110 linhas. Atual: " + overtimeLines.size());
    }

    @Test
    @DisplayName("TIMESHEET-03: Contratos canónicos literais preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("HRTimeSheetPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Folha de Ponto\", filters,\n                moreBtn, closeBtn, entryBtn)"),
                "Falta UIHelper.tableCardTop com formatação exata para Folha de Ponto");
        assertTrue(content.contains("TableFilter.bar(search, refreshBtn)"), "Falta TableFilter.bar(search, refreshBtn)");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
        assertTrue(content.contains("TableQuickPeekController.install(table, card"),
                "Falta TableQuickPeekController.install(table, card");
    }

    @Test
    @DisplayName("TIMESHEET-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("HRTimeSheetPanel.java", "HrTimeEntryDialog.java", "HrOvertimeRatesDialog.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("TIMESHEET-05: Construtor package-private e métodos preservados")
    void testPublicApiPreserved() throws NoSuchMethodException {
        HRTimeSheetPanel.class.getDeclaredConstructor(HRPanel.class);
        HRTimeSheetPanel.class.getDeclaredMethod("buildPanel");
        HRTimeSheetPanel.class.getDeclaredMethod("load");
    }
}
