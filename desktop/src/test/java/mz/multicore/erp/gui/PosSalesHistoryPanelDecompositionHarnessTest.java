package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: PosSalesHistoryPanel")
class PosSalesHistoryPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("POS-HIST-01: PosSalesHistoryPanel.java deve possuir <= 165 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("PosSalesHistoryPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165,
                () -> "PosSalesHistoryPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("POS-HIST-02: PosSalesHistoryViewCoordinator.java deve possuir <= 135 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path coord = ROOT.resolve("PosSalesHistoryViewCoordinator.java");
        List<String> lines = Files.readAllLines(coord);
        assertTrue(lines.size() <= 135,
                () -> "PosSalesHistoryViewCoordinator.java deve possuir <= 135 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("POS-HIST-03: Contratos canónicos COMM-03 preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("PosSalesHistoryPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("TableFilter.bar("), "Falta TableFilter.bar(");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(owner.salesHistoryTable)"),
                "Falta UIHelper.ensureHeadersFit(owner.salesHistoryTable)");
        assertTrue(content.contains("new TablePager("), "Falta new TablePager(");
        assertFalse(content.contains("shBar = new JPanel(new GridBagLayout())"),
                "Não deve conter GridBagLayout em shBar");
    }

    @Test
    @DisplayName("POS-HIST-07: Resumo em cartões KpiCard e acções no cabeçalho do card (padrão do sistema)")
    void testSystemStandardHeader() throws IOException {
        String panel = Files.readString(ROOT.resolve("PosSalesHistoryPanel.java"));
        String view = Files.readString(ROOT.resolve("PosSalesHistoryViewCoordinator.java"));
        assertTrue(panel.contains("UIHelper.tableCardTop(\"Vendas Registadas\", shBar,"),
                "Título, Actualizar e acções devem estar no cabeçalho do card, como em Pedidos");
        assertTrue(view.contains("KpiCard.createGrid(4)"), "O resumo deve usar a grelha padrão de KpiCard");
        assertFalse(view.contains("createSummaryChip") || view.contains("<html>"),
                "Sem chips nem texto HTML solto por cima da tabela");
    }

    @Test
    @DisplayName("POS-HIST-04: Conformidade com BrightButtonsHarnessTest")
    void testBrightButtonsCompliance() throws IOException {
        for (String file : List.of("PosSalesHistoryPanel.java", "PosSalesHistoryViewCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("createSecondaryButton"),
                    "Ficheiro " + file + " não deve usar createSecondaryButton.");
            assertFalse(content.contains("BUTTON_NEUTRAL"),
                    "Ficheiro " + file + " não deve usar BUTTON_NEUTRAL.");
        }
    }

    @Test
    @DisplayName("POS-HIST-05: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("PosSalesHistoryPanel.java", "PosSalesHistoryViewCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("POS-HIST-06: Construtor package-private e métodos públicos preservados")
    void testPublicApiPreserved() throws NoSuchMethodException {
        PosSalesHistoryPanel.class.getDeclaredConstructor(POSPanel.class);
        PosSalesHistoryPanel.class.getMethod("buildPanel");
        PosSalesHistoryPanel.class.getMethod("refresh");
    }
}
