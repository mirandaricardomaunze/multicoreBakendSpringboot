package mz.multicore.erp.gui.commercial;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: CommercialNotesPanel")
class CommercialNotesPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui/commercial");

    @Test
    @DisplayName("COM-01: CommercialNotesPanel.java deve possuir <= 370 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("CommercialNotesPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 370,
                () -> "CommercialNotesPanel.java deve possuir <= 370 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("COM-02: Colaboradores devem possuir limites de tamanho estritos")
    void testCollaboratorsLineCounts() throws IOException {
        Path debit = ROOT.resolve("DebitNoteFormDialog.java");
        List<String> debitLines = Files.readAllLines(debit);
        assertTrue(debitLines.size() <= 95,
                () -> "DebitNoteFormDialog.java deve possuir <= 95 linhas. Atual: " + debitLines.size());

        Path ops = ROOT.resolve("CommercialNotesOperationsCoordinator.java");
        List<String> opsLines = Files.readAllLines(ops);
        assertTrue(opsLines.size() <= 100,
                () -> "CommercialNotesOperationsCoordinator.java deve possuir <= 100 linhas. Atual: " + opsLines.size());
    }

    @Test
    @DisplayName("COM-03: Strings canónicas e contratos contratuais devem ser preservados")
    void testCanonicalStringsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("CommercialNotesPanel.java"));

        assertTrue(content.contains("createRefreshButton(this::loadCredits)"), "Falta createRefreshButton loadCredits");
        assertTrue(content.contains("createRefreshButton(this::loadDebits)"), "Falta createRefreshButton loadDebits");
        assertTrue(content.contains("ActionMenuButton"), "Falta ActionMenuButton");
        assertTrue(content.contains("\"Aprovar\""), "Falta acção Aprovar");
        assertTrue(content.contains("\"Rejeitar\""), "Falta acção Rejeitar");
        assertTrue(content.contains("return column == 6"), "Falta return column == 6 para edição da grelha de crédito");
        assertTrue(content.contains("new CreateCreditNoteLineRequest"), "Falta new CreateCreditNoteLineRequest");
        assertTrue(content.contains("TableFilter.bar("), "Falta TableFilter.bar(");
        assertTrue(content.contains("UIHelper.tableCardTop("), "Falta UIHelper.tableCardTop(");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(creditTable)"), "Falta ensureHeadersFit creditTable");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(debitTable)"), "Falta ensureHeadersFit debitTable");
    }

    @Test
    @DisplayName("COM-04: Eliminação de diálogos legados JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("CommercialNotesPanel.java", "DebitNoteFormDialog.java", "CommercialNotesOperationsCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("COM-05: Construtores e métodos públicos preservados")
    void testPublicApiAndConstructorsPreserved() throws NoSuchMethodException {
        // Construtor sem printApiClient
        CommercialNotesPanel.class.getConstructor(
                mz.multicore.erp.desktop.client.ComercialApiClient.class,
                mz.multicore.erp.desktop.client.CreditNoteApiClient.class,
                mz.multicore.erp.desktop.client.DebitNoteApiClient.class,
                java.util.function.Supplier.class
        );

        // Construtor com printApiClient
        CommercialNotesPanel.class.getConstructor(
                mz.multicore.erp.desktop.client.ComercialApiClient.class,
                mz.multicore.erp.desktop.client.CreditNoteApiClient.class,
                mz.multicore.erp.desktop.client.DebitNoteApiClient.class,
                java.util.function.Supplier.class,
                mz.multicore.erp.desktop.client.PrintApiClient.class
        );

        // Métodos públicos
        CommercialNotesPanel.class.getMethod("creditTab");
        CommercialNotesPanel.class.getMethod("debitTab");
        CommercialNotesPanel.class.getMethod("refresh");
    }
}
