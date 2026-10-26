package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HARNESS-ACCOUNTING-PANEL-DECOMPOSITION-001: Decomposição do Painel de Contabilidade")
class AccountingPanelDecompositionHarnessTest {

    private AccountingApiClient accountingApiClient;

    @BeforeEach
    void setUp() {
        accountingApiClient = mock(AccountingApiClient.class);
    }

    @Test
    @DisplayName("AD-01: AccountingPanel.java reduzido para <= 120 linhas")
    void testLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/accounting/AccountingPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/accounting/AccountingPanel.java");
        }
        assertTrue(Files.exists(path), "AccountingPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 120, "AccountingPanel.java deve possuir <= 120 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("AD-02: Abas especializadas desacopladas e dentro dos limites")
    void testTabsLineCounts() throws IOException {
        List<String> files = List.of(
                "AccountingChartTab.java",
                "AccountingJournalTab.java",
                "AccountingTrialBalanceTab.java",
                "AccountingLedgerTab.java",
                "AccountingStatementsTab.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/accounting");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/accounting");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> lines = Files.readAllLines(filePath);
            assertTrue(lines.size() <= 160, file + " deve possuir <= 160 linhas. Atual: " + lines.size());
        }
    }

    @Test
    @DisplayName("AD-03: Ausência total de JOptionPane no pacote de contabilidade")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "AccountingPanel.java",
                "AccountingChartTab.java",
                "AccountingJournalTab.java",
                "AccountingTrialBalanceTab.java",
                "AccountingLedgerTab.java",
                "AccountingStatementsTab.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/accounting");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/accounting");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> content = Files.readAllLines(filePath);
            for (int i = 0; i < content.size(); i++) {
                assertFalse(content.get(i).contains("JOptionPane"),
                        file + " contém JOptionPane na linha " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("AD-04: Construtor canónico e ciclo de vida preservados")
    void testConstructorsAndLifecycle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AccountingPanel panel = new AccountingPanel(accountingApiClient);
            assertNotNull(panel, "AccountingPanel deve instanciar com 1 argumento");
            assertDoesNotThrow(panel::onPanelSelected, "onPanelSelected() deve executar sem erros");
        });
    }

    @Test
    @DisplayName("AD-05: Conformidade de tableCardTop, ensureHeadersFit e sem emojis Unicode")
    void testCanonicalCardStructureAndNoUnicode() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/accounting/AccountingPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/accounting/AccountingPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop("), "AccountingPanel deve referenciar UIHelper.tableCardTop");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "AccountingPanel deve conter UIHelper.ensureHeadersFit(table)");

        assertFalse(content.matches(".*[\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF].*"),
                "Não deve conter emojis Unicode em AccountingPanel.java");
    }
}
