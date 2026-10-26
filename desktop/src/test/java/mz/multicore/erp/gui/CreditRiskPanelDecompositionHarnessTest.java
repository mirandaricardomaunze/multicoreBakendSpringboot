package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.table.TableCellRenderer;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HARNESS-CREDIT-RISK-PANEL-DECOMPOSITION-001: Decomposição do Painel de Risco de Crédito")
class CreditRiskPanelDecompositionHarnessTest {

    private CreditRiskApiClient creditRiskApiClient;

    @BeforeEach
    void setUp() {
        creditRiskApiClient = mock(CreditRiskApiClient.class);
    }

    @Test
    @DisplayName("CRD-01: CreditRiskPanel.java reduzido para <= 165 linhas")
    void testLineCount() throws IOException {
        Path path = Path.of("src/main/java/mz/multicore/erp/gui/CreditRiskPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/CreditRiskPanel.java");
        }
        assertTrue(Files.exists(path), "CreditRiskPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165, "CreditRiskPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CRD-02: Colaboradores desacoplados instanciáveis")
    void testCollaboratorsDecoupled() {
        CreditRiskCellRenderer renderer = new CreditRiskCellRenderer();
        assertNotNull(renderer, "CreditRiskCellRenderer deve instanciar");

        CreditRiskPanel panel = new CreditRiskPanel(creditRiskApiClient);
        TableCellRenderer tableRenderer = panel.table.getDefaultRenderer(Object.class);
        assertTrue(tableRenderer instanceof CreditRiskCellRenderer, "Tabela deve usar CreditRiskCellRenderer");
    }

    @Test
    @DisplayName("CRD-03: Preservação de campos públicos/package-private para testes")
    void testFieldsPreserved() {
        CreditRiskPanel panel = new CreditRiskPanel(creditRiskApiClient);

        assertNotNull(panel.kpiTotalReceivable);
        assertNotNull(panel.kpiTotalOverdue);
        assertNotNull(panel.kpiBlockedCount);
        assertNotNull(panel.kpiCriticalCount);
        assertNotNull(panel.table);
        assertNotNull(panel.tableModel);
        assertNotNull(panel.dateField);
        assertNotNull(panel.riskFilterCombo);
        assertNotNull(panel.searchField);
        assertNotNull(panel.allClientsRisk);
        assertNotNull(panel.visibleClientsRisk);

        // Atualização de KPIs
        CreditRiskSummaryDTO summary = new CreditRiskSummaryDTO(
                LocalDate.now(),
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("5000.00"),
                10,
                2,
                1,
                1,
                1,
                1
        );
        assertDoesNotThrow(() -> panel.updateKpis(summary));
        assertDoesNotThrow(panel::applyFilters);
    }

    @Test
    @DisplayName("CRD-04: Erradicação de JOptionPane em todos os ficheiros de risco de crédito")
    void testZeroJOptionPane() throws IOException {
        List<String> fileNames = List.of(
                "CreditRiskPanel.java",
                "CreditRiskViewCoordinator.java",
                "CreditExceptionDialog.java",
                "CreditRiskOperationsCoordinator.java",
                "CreditRiskCellRenderer.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : fileNames) {
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
    @DisplayName("CRD-05: Conformidade canónica de topo de card, QuickPeek e paginação")
    void testCanonicalCardStructure() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/CreditRiskPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/CreditRiskPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop(\"Matriz de Aging & Gestão de Risco\""),
                "Deve conter cabeçalho canónico tableCardTop");
        assertTrue(content.contains("TableQuickPeekController.install(table"),
                "Deve instalar TableQuickPeekController com tecla Espaço");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"),
                "Deve calibrar colunas com ensureHeadersFit");
        assertTrue(content.contains("ClientTablePagination.install(table)"),
                "Deve instalar paginação corporativa");
    }
}
