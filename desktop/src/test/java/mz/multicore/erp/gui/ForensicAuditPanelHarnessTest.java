package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de testes de conformidade do painel ForensicAuditPanel (AFF-07).
 */
public class ForensicAuditPanelHarnessTest {

    @Test
    @DisplayName("AFF-07.1: Inicialização de KPIs, filtros e tabela de 8 colunas operacionais")
    void testPanelInitializationAndColumns() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ForensicAuditApiClient client = Mockito.mock(ForensicAuditApiClient.class);
            ForensicAuditPanel panel = new ForensicAuditPanel(client);

            assertNotNull(panel);
            assertNotNull(panel.kpiTotalRisk);
            assertNotNull(panel.kpiCritical);
            assertNotNull(panel.kpiSuspicious);
            assertNotNull(panel.kpiComplianceScore);

            assertNotNull(panel.startDateField);
            assertNotNull(panel.endDateField);
            assertNotNull(panel.severityCombo);
            assertNotNull(panel.categoryCombo);
            assertNotNull(panel.operatorField);

            assertNotNull(panel.table);
            assertEquals(8, panel.table.getColumnCount(), "A tabela de anomalias forenses deve possuir 8 colunas");
            assertEquals("Data/Hora", panel.table.getColumnName(0));
            assertEquals("Severidade", panel.table.getColumnName(1));
            assertEquals("Categoria", panel.table.getColumnName(2));
            assertEquals("Referência", panel.table.getColumnName(3));
            assertEquals("Operador", panel.table.getColumnName(4));
            assertEquals("Impacto (MT)", panel.table.getColumnName(5));
            assertEquals("Detalhes / Ocorrência", panel.table.getColumnName(6));
            assertEquals("Recomendação Preventiva", panel.table.getColumnName(7));
        });
    }

    @Test
    @DisplayName("AFF-07.2: Atualização de KPIs e linhas da tabela a partir do DTO de sumário")
    void testRenderSummary() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ForensicAuditApiClient client = Mockito.mock(ForensicAuditApiClient.class);
            ForensicAuditPanel panel = new ForensicAuditPanel(client);

            ForensicAnomalyDTO a1 = new ForensicAnomalyDTO(
                    1L,
                    LocalDateTime.of(2026, 9, 15, 10, 30),
                    "operador_caixa",
                    ForensicCategory.DOC_CANCELLATION,
                    "Cancelamento de Documentos",
                    ForensicSeverity.CRITICAL,
                    "FT-2026/001",
                    new BigDecimal("15000.00"),
                    "Fatura cancelada sem justificação",
                    "Sem justificação",
                    "Abertura de inquérito imediato"
            );

            ForensicAnomalyDTO a2 = new ForensicAnomalyDTO(
                    2L,
                    LocalDateTime.of(2026, 9, 16, 11, 0),
                    "vendedor_01",
                    ForensicCategory.EXCESSIVE_DISCOUNT,
                    "Desconto Excessivo",
                    ForensicSeverity.SUSPICIOUS,
                    "FT-2026/050",
                    new BigDecimal("3500.00"),
                    "Desconto de 25% concedido",
                    "Desconto manual em venda",
                    "Validar alçada de desconto"
            );

            ForensicAuditSummaryDTO summary = new ForensicAuditSummaryDTO(
                    2,
                    1,
                    1,
                    0,
                    new BigDecimal("18500.00"),
                    "68% — Atenção / Risco Moderado",
                    List.of(a1, a2)
            );

            panel.renderSummary(summary);

            assertTrue(panel.kpiTotalRisk.getText().contains("18") && panel.kpiTotalRisk.getText().contains("500,00"),
                    "Risco total deve formatar 18.500,00 MT: " + panel.kpiTotalRisk.getText());
            assertEquals("1", panel.kpiCritical.getText());
            assertEquals("1", panel.kpiSuspicious.getText());
            assertEquals("68% — Atenção / Risco Moderado", panel.kpiComplianceScore.getText());

            assertEquals(2, panel.tableModel.getRowCount());
            assertEquals("CRITICAL", panel.tableModel.getValueAt(0, 1));
            assertEquals("FT-2026/001", panel.tableModel.getValueAt(0, 3));
            assertEquals("SUSPICIOUS", panel.tableModel.getValueAt(1, 1));
            assertEquals("FT-2026/050", panel.tableModel.getValueAt(1, 3));
        });
    }

    @Test
    @DisplayName("AFF-07.3: Conformidade de Linhas (<= 1000) e Ausência de Cores Locais ou Palavras Proibidas")
    void testCodeQualityAndStandards() throws IOException {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "ForensicAuditPanel.java");
        assertTrue(Files.exists(path), "ForensicAuditPanel.java deve existir");

        long lineCount = Files.lines(path).count();
        assertTrue(lineCount <= 1000, "ForensicAuditPanel.java deve ter <= 1000 linhas, mas tem: " + lineCount);

        List<String> lines = Files.readAllLines(path);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            assertFalse(line.contains("new Color("),
                    "Linha " + (i + 1) + " não pode instanciar new Color(...). Use tokens do UIHelper: " + line);
            assertFalse(line.toLowerCase().contains("atualizar"),
                    "Linha " + (i + 1) + " não pode usar a palavra proibida 'atualizar'. Em Moçambique deve ser 'Actualizar': " + line);
        }
    }
}
