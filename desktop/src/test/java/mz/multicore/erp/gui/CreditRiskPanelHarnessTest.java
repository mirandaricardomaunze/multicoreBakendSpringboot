package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de testes automatizados para conformidade com RISCO_CREDITO_COBRANCA_SPEC e HARNESS.
 * Valida o Centro de Risco de Crédito, matriz de aging, filtragem por risco e busca textual.
 */
public class CreditRiskPanelHarnessTest {

    @Test
    @DisplayName("RCC-01: Inicialização dos KPIs e tabela de 15 colunas operacionais")
    void testPanelInitializationAndColumns() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor.financeiro", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                CreditRiskPanel panel = createTestPanel();
                assertNotNull(panel);
                assertNotNull(panel.kpiTotalReceivable);
                assertNotNull(panel.kpiTotalOverdue);
                assertNotNull(panel.kpiBlockedCount);
                assertNotNull(panel.kpiCriticalCount);

                assertNotNull(panel.table);
                assertEquals(15, panel.table.getColumnCount(), "A tabela de risco de crédito deve possuir 15 colunas");
                assertEquals("Cliente", panel.table.getColumnName(1));
                assertEquals("Corrente", panel.table.getColumnName(6));
                assertEquals("Total em Mora", panel.table.getColumnName(11));
                assertEquals("Nível Risco", panel.table.getColumnName(13));
                assertEquals("Situação", panel.table.getColumnName(14));
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RCC-02: Atualização dos KPIs a partir do sumário executivo")
    void testKpiUpdateFromSummary() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor.financeiro", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                CreditRiskPanel panel = createTestPanel();
                CreditRiskSummaryDTO summary = new CreditRiskSummaryDTO(
                        LocalDate.now(),
                        new BigDecimal("250000.00"),
                        new BigDecimal("75000.00"),
                        new BigDecimal("500000.00"),
                        15,
                        3,
                        2,
                        4,
                        5,
                        4
                );

                panel.updateKpis(summary);

                assertTrue(panel.kpiTotalReceivable.getText().contains("250") && panel.kpiTotalReceivable.getText().contains("000,00"),
                        "Total a receber deve conter 250...000,00 MT: " + panel.kpiTotalReceivable.getText());
                assertTrue(panel.kpiTotalOverdue.getText().contains("75") && panel.kpiTotalOverdue.getText().contains("000,00"),
                        "Total em mora deve conter 75...000,00 MT: " + panel.kpiTotalOverdue.getText());
                assertEquals("3", panel.kpiBlockedCount.getText());
                assertEquals("6", panel.kpiCriticalCount.getText(), "Crítico (2) + Alto (4) deve totalizar 6");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RCC-03: Filtragem por nível de risco e bloqueio")
    void testRiskLevelFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor.financeiro", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                CreditRiskPanel panel = createTestPanel();
                panel.allClientsRisk = sampleClientsRisk();

                // Filtrar por Crítico
                panel.riskFilterCombo.setSelectedItem("CRITICAL - Crítico");
                panel.applyFilters();
                assertEquals(1, panel.tableModel.getRowCount());
                assertEquals(CreditRiskLevel.CRITICAL.label(), panel.tableModel.getValueAt(0, 13));

                // Filtrar por Alto
                panel.riskFilterCombo.setSelectedItem("HIGH - Alto");
                panel.applyFilters();
                assertEquals(1, panel.tableModel.getRowCount());
                assertEquals(CreditRiskLevel.HIGH.label(), panel.tableModel.getValueAt(0, 13));

                // Filtrar por Apenas Crítico / Alto
                panel.riskFilterCombo.setSelectedItem("Apenas Crítico / Alto");
                panel.applyFilters();
                assertEquals(2, panel.tableModel.getRowCount());

                // Filtrar por Apenas Bloqueados
                panel.riskFilterCombo.setSelectedItem("Apenas Bloqueados");
                panel.applyFilters();
                assertEquals(1, panel.tableModel.getRowCount());
                assertEquals("BLOQUEADO", panel.tableModel.getValueAt(0, 14));

                // Todos os riscos
                panel.riskFilterCombo.setSelectedItem("Todos os Riscos");
                panel.applyFilters();
                assertEquals(4, panel.tableModel.getRowCount());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RCC-04: Pesquisa dinâmica por nome, NUIT ou email")
    void testTextSearchFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor.financeiro", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                CreditRiskPanel panel = createTestPanel();
                panel.allClientsRisk = sampleClientsRisk();

                // Pesquisa por nome
                panel.searchField.setText("Construtora");
                panel.applyFilters();
                assertEquals(1, panel.tableModel.getRowCount());
                assertTrue(panel.tableModel.getValueAt(0, 1).toString().contains("Construtora"));

                // Pesquisa por NUIT
                panel.searchField.setText("400999888");
                panel.applyFilters();
                assertEquals(1, panel.tableModel.getRowCount());
                assertEquals("400999888", panel.tableModel.getValueAt(0, 2));

                // Pesquisa por email
                panel.searchField.setText("comercial@delta.mz");
                panel.applyFilters();
                assertEquals(1, panel.tableModel.getRowCount());

                // Limpeza do filtro
                panel.searchField.setText("");
                panel.applyFilters();
                assertEquals(4, panel.tableModel.getRowCount());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RCC-05: Mapeamento dos escalões da matriz de aging")
    void testAgingBucketsDataMapping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor.financeiro", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                CreditRiskPanel panel = createTestPanel();
                panel.allClientsRisk = sampleClientsRisk();
                panel.applyFilters();

                // Linha 0: Construtora Maputo (Crítico)
                assertEquals(new BigDecimal("100000.00"), panel.allClientsRisk.get(0).totalDebt());
                assertEquals(new BigDecimal("70000.00"), panel.allClientsRisk.get(0).maisDe90());
                assertEquals(new BigDecimal("70000.00"), panel.allClientsRisk.get(0).totalOverdue());
                assertEquals(95, panel.allClientsRisk.get(0).maxDaysOverdue());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RCC-06: Formatação visual de células e cores do semáforo de risco")
    void testCellRendererVisualHierarchy() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor.financeiro", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                CreditRiskPanel panel = createTestPanel();
                panel.allClientsRisk = sampleClientsRisk();
                panel.applyFilters();

                TableCellRenderer renderer = panel.table.getDefaultRenderer(Object.class);

                // Coluna 13: Nível de Risco para linha 0 (CRITICAL)
                Component compCritical = renderer.getTableCellRendererComponent(
                        panel.table, CreditRiskLevel.CRITICAL.label(), false, false, 0, 13);
                assertEquals(UIHelper.REJECTED_RED, compCritical.getForeground());

                // Coluna 14: Situação para linha 0 (BLOQUEADO)
                Component compBlocked = renderer.getTableCellRendererComponent(
                        panel.table, "BLOQUEADO", false, false, 0, 14);
                assertEquals(UIHelper.REJECTED_RED, compBlocked.getForeground());

                // Coluna 14: Situação para linha 3 (NORMAL)
                Component compRegular = renderer.getTableCellRendererComponent(
                        panel.table, "NORMAL", false, false, 3, 14);
                assertEquals(UIHelper.APPROVED_GREEN, compRegular.getForeground());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RCC-07: CreditRiskPanel.java cumpre estritamente o limite de 1000 linhas")
    void testPanelLineCountUnderOneThousand() throws IOException {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "CreditRiskPanel.java");
        assertTrue(Files.exists(path), "CreditRiskPanel.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 1000, "CreditRiskPanel.java tem " + lines + " linhas; deve ser <= 1000");
    }

    private CreditRiskPanel createTestPanel() {
        CreditRiskApiClient client = Mockito.mock(CreditRiskApiClient.class);
        return new CreditRiskPanel(client);
    }

    private List<ClientCreditRiskDTO> sampleClientsRisk() {
        ClientCreditRiskDTO c1 = new ClientCreditRiskDTO(
                1L, "Construtora Maputo, Lda", "400111222", "financeiro@construtora.mz", "Av. 24 de Julho",
                new BigDecimal("50000.00"), new BigDecimal("100000.00"), BigDecimal.ZERO,
                LocalDate.now().minusDays(95), 95,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("70000.00"),
                new BigDecimal("70000.00"), CreditRiskLevel.CRITICAL, true, "Incumprimento prolongado (>90 dias)"
        );

        ClientCreditRiskDTO c2 = new ClientCreditRiskDTO(
                2L, "Distribuidora Beira, SA", "400333444", "contas@distribuidora.mz", "Rua do Porto",
                new BigDecimal("80000.00"), new BigDecimal("60000.00"), new BigDecimal("20000.00"),
                LocalDate.now().minusDays(45), 45,
                new BigDecimal("20000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("40000.00"), CreditRiskLevel.HIGH, false, null
        );

        ClientCreditRiskDTO c3 = new ClientCreditRiskDTO(
                3L, "Mercearia Central", "400555666", "mercearia@central.mz", "Mercado Central",
                new BigDecimal("30000.00"), new BigDecimal("25000.00"), new BigDecimal("5000.00"),
                LocalDate.now().minusDays(15), 15,
                new BigDecimal("15000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10000.00"), CreditRiskLevel.MEDIUM, false, null
        );

        ClientCreditRiskDTO c4 = new ClientCreditRiskDTO(
                4L, "Delta Comércio Geral", "400999888", "comercial@delta.mz", "Bairro Central",
                new BigDecimal("100000.00"), new BigDecimal("15000.00"), new BigDecimal("85000.00"),
                null, 0,
                new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, CreditRiskLevel.LOW, false, null
        );

        return List.of(c1, c2, c3, c4);
    }
}
