package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Filtragem Temporal e Estado de Tabelas do ERP")
class TableFilterHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("parseCellDate extrai datas no formato dd/MM/yyyy mesmo com sufixos ou horas")
    void parseCellDate_extractsDatesCorrectly() {
        assertEquals(LocalDate.of(2026, 9, 15), TableFilter.parseCellDate("15/09/2026"));
        assertEquals(LocalDate.of(2026, 9, 15), TableFilter.parseCellDate("15/09/2026 14:30"));
        assertEquals(LocalDate.of(2026, 9, 15), TableFilter.parseCellDate("15/09/2026 (em atraso)"));
        assertNull(TableFilter.parseCellDate("—"));
        assertNull(TableFilter.parseCellDate(null));
        assertNull(TableFilter.parseCellDate("invalido"));
    }

    @Test
    @DisplayName("matchesPeriod valida correctamente Hoje, Últimos 7 dias, Últimos 30 dias e Este mês")
    void matchesPeriod_evaluatesTemporalBoundaries() {
        LocalDate today = LocalDate.of(2026, 9, 15);

        // Todo o período / Nulo
        assertTrue(TableFilter.matchesPeriod(today.minusYears(2), "Todo o período", today));
        assertTrue(TableFilter.matchesPeriod(null, "Todo o período", today));
        assertTrue(TableFilter.matchesPeriod(today, null, today));

        // Hoje
        assertTrue(TableFilter.matchesPeriod(today, "Hoje", today));
        assertFalse(TableFilter.matchesPeriod(today.minusDays(1), "Hoje", today));
        assertFalse(TableFilter.matchesPeriod(today.plusDays(1), "Hoje", today));

        // Últimos 7 dias
        assertTrue(TableFilter.matchesPeriod(today, "Últimos 7 dias", today));
        assertTrue(TableFilter.matchesPeriod(today.minusDays(6), "Últimos 7 dias", today));
        assertFalse(TableFilter.matchesPeriod(today.minusDays(7), "Últimos 7 dias", today));
        assertFalse(TableFilter.matchesPeriod(today.plusDays(1), "Últimos 7 dias", today));

        // Últimos 30 dias
        assertTrue(TableFilter.matchesPeriod(today, "Últimos 30 dias", today));
        assertTrue(TableFilter.matchesPeriod(today.minusDays(29), "Últimos 30 dias", today));
        assertFalse(TableFilter.matchesPeriod(today.minusDays(30), "Últimos 30 dias", today));

        // Este mês
        assertTrue(TableFilter.matchesPeriod(LocalDate.of(2026, 9, 1), "Este mês", today));
        assertTrue(TableFilter.matchesPeriod(LocalDate.of(2026, 9, 1), "Este mês", today));
        assertFalse(TableFilter.matchesPeriod(LocalDate.of(2026, 9, 30), "Este mês", today));
        assertFalse(TableFilter.matchesPeriod(LocalDate.of(2026, 8, 31), "Este mês", today));
        assertFalse(TableFilter.matchesPeriod(LocalDate.of(2026, 10, 1), "Este mês", today));
    }

    @Test
    @DisplayName("periodCombo instancia os períodos canónicos com altura padrão")
    void periodCombo_hasCanonicalOptionsAndStyle() {
        JComboBox<String> combo = TableFilter.periodCombo();
        assertNotNull(combo);
        assertEquals(8, combo.getItemCount());
        assertEquals("Todo o período", combo.getItemAt(0));
        assertEquals("Hoje", combo.getItemAt(1));
        assertEquals("Ontem", combo.getItemAt(2));
        assertEquals("Esta semana", combo.getItemAt(3));
        assertEquals("Últimos 7 dias", combo.getItemAt(4));
        assertEquals("Este mês", combo.getItemAt(5));
        assertEquals("Últimos 30 dias", combo.getItemAt(6));
        assertEquals("Este ano", combo.getItemAt(7));
    }

    @Test
    @DisplayName("Tabelas prioritárias com datas expõem PeriodFilter obrigatório")
    void priorityTemporalPanelsDeclarePeriodFilter() throws IOException {
        List<String> priorityPanels = List.of(
                "BankReconciliationPanel.java",
                "CommercialOrdersView.java",
                "PurchaseOrdersPanel.java",
                "StockPanel.java",
                "FinanceiroPanel.java",
                Path.of("commercial", "QuotationsPanel.java").toString(),
                Path.of("commercial", "CommercialNotesPanel.java").toString(),
                Path.of("commercial", "DeliveryGuidesPanel.java").toString()
        );

        for (String relPath : priorityPanels) {
            Path file = GUI_ROOT.resolve(relPath);
            assertThat(file).as("Arquivo %s deve existir", relPath).exists();
            String content = Files.readString(file);
            assertThat(content)
                    .as("Painel %s deve conter PeriodFilter para navegação temporal", relPath)
                    .contains("PeriodFilter");
            assertThat(content)
                    .as("Painel %s deve conter TableFilter.periodCombo", relPath)
                    .contains("periodCombo");
        }
    }

    @Test
    @DisplayName("TableFilter.bar e toolbar mantêm estritamente uma única linha sem wrap para trás da tabela")
    void tableFilterBars_enforceStrictSingleRowLayout() {
        javax.swing.JPanel bar = TableFilter.bar(
                new javax.swing.JTextField(20),
                new javax.swing.JComboBox<>(new String[]{"A", "B"}),
                new javax.swing.JButton("Btn 1"),
                new javax.swing.JButton("Btn 2")
        );
        bar.setSize(200, 36);
        bar.doLayout();
        for (int i = 0; i < bar.getComponentCount(); i++) {
            assertTrue(bar.getComponent(i).getY() < 36, "Componente na bar não deve quebrar para linha 2 (y deve estar na 1ª linha)");
        }

        javax.swing.JPanel toolbar = TableFilter.toolbar(
                null,
                new javax.swing.JComponent[]{
                        new javax.swing.JButton("Action 1"),
                        new javax.swing.JButton("Action 2"),
                        new javax.swing.JButton("Action 3")
                }
        );
        toolbar.setSize(200, 36);
        toolbar.doLayout();
        javax.swing.JPanel right = (javax.swing.JPanel) toolbar.getComponent(0);
        right.setSize(100, 36);
        right.doLayout();
        for (int i = 0; i < right.getComponentCount(); i++) {
            assertTrue(right.getComponent(i).getY() < 36, "Acções na toolbar não devem quebrar para linha 2 (y deve estar na 1ª linha)");
        }
    }
}
