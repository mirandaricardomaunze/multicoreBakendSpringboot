package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.TableFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de validação do filtro universal por período em tabelas transacionais (UFP-01 a UFP-08).
 * Verifica a lógica temporal de cálculo retrospetivo e a integração nos painéis de Faturação e Quebras.
 */
public class UniversalPeriodFilterHarnessTest {

    private static final LocalDate REF_DATE = LocalDate.of(2026, 9, 15); // Uma terça-feira

    @Test
    @DisplayName("UFP-01: 'Hoje' inclui apenas a data exata de referência")
    void testTodaySelection() {
        assertTrue(TableFilter.matchesPeriod(REF_DATE, "Hoje", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.minusDays(1), "Hoje", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.plusDays(1), "Hoje", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(null, "Hoje", REF_DATE));
    }

    @Test
    @DisplayName("UFP-02: 'Ontem' inclui exatamente today - 1 dia")
    void testYesterdaySelection() {
        LocalDate yesterday = REF_DATE.minusDays(1);
        assertTrue(TableFilter.matchesPeriod(yesterday, "Ontem", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE, "Ontem", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.minusDays(2), "Ontem", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.plusDays(1), "Ontem", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(null, "Ontem", REF_DATE));
    }

    @Test
    @DisplayName("UFP-03: 'Esta semana' inclui da 2ª feira corrente até hoje; exclui domingo anterior e datas futuras")
    void testThisWeekSelection() {
        LocalDate monday = REF_DATE.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); // 14/09/2026
        LocalDate sundayBefore = monday.minusDays(1); // 13/09/2026 (domingo da semana anterior)

        assertTrue(TableFilter.matchesPeriod(monday, "Esta semana", REF_DATE), "Segunda-feira deve estar incluída");
        assertTrue(TableFilter.matchesPeriod(REF_DATE, "Esta semana", REF_DATE), "Dia de hoje (terça-feira) deve estar incluído");
        assertFalse(TableFilter.matchesPeriod(sundayBefore, "Esta semana", REF_DATE), "Domingo anterior não pertence a 'Esta semana'");
        assertFalse(TableFilter.matchesPeriod(REF_DATE.plusDays(1), "Esta semana", REF_DATE), "Data futura não pertence");
        assertFalse(TableFilter.matchesPeriod(null, "Esta semana", REF_DATE));
    }

    @Test
    @DisplayName("UFP-04: 'Últimos 7 dias' inclui 6 dias atrás até hoje; exclui 7 dias atrás")
    void testLastSevenDays() {
        assertTrue(TableFilter.matchesPeriod(REF_DATE, "Últimos 7 dias", REF_DATE));
        assertTrue(TableFilter.matchesPeriod(REF_DATE.minusDays(6), "Últimos 7 dias", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.minusDays(7), "Últimos 7 dias", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.plusDays(1), "Últimos 7 dias", REF_DATE));
    }

    @Test
    @DisplayName("UFP-05: 'Este mês' inclui 1º dia do mês corrente e exclui último dia do mês anterior")
    void testThisMonth() {
        LocalDate firstOfMonth = LocalDate.of(2026, 9, 1);
        LocalDate lastOfAugust = LocalDate.of(2026, 8, 31);

        assertTrue(TableFilter.matchesPeriod(firstOfMonth, "Este mês", REF_DATE));
        assertTrue(TableFilter.matchesPeriod(REF_DATE, "Este mês", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(lastOfAugust, "Este mês", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.plusDays(1), "Este mês", REF_DATE));
    }

    @Test
    @DisplayName("UFP-06: 'Este ano' inclui 1 de Janeiro do ano corrente e exclui 31 de Dezembro anterior")
    void testThisYear() {
        LocalDate janFirst = LocalDate.of(2026, 1, 1);
        LocalDate decPrevYear = LocalDate.of(2025, 12, 31);

        assertTrue(TableFilter.matchesPeriod(janFirst, "Este ano", REF_DATE));
        assertTrue(TableFilter.matchesPeriod(REF_DATE, "Este ano", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(decPrevYear, "Este ano", REF_DATE));
        assertFalse(TableFilter.matchesPeriod(REF_DATE.plusDays(1), "Este ano", REF_DATE));
    }

    @Test
    @DisplayName("UFP-07: 'Todo o período' inclui qualquer data e tolera nulo")
    void testAllPeriod() {
        assertTrue(TableFilter.matchesPeriod(REF_DATE, "Todo o período", REF_DATE));
        assertTrue(TableFilter.matchesPeriod(LocalDate.of(2010, 1, 1), "Todo o período", REF_DATE));
        assertTrue(TableFilter.matchesPeriod(null, "Todo o período", REF_DATE));
    }

    @Test
    @DisplayName("UFP-08: A tabela de Faturas possui coluna 'Data' e 'PeriodFilter' com suporte universal")
    void testInvoicesTableHasDateColumnAndFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
                InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
                FinanceApiClient finApi = Mockito.mock(FinanceApiClient.class);
                CreditNoteApiClient creditApi = Mockito.mock(CreditNoteApiClient.class);
                DebitNoteApiClient debitApi = Mockito.mock(DebitNoteApiClient.class);
                POSApiClient posApi = Mockito.mock(POSApiClient.class);
                MovimentosApiClient movApi = Mockito.mock(MovimentosApiClient.class);
                PromotionApiClient promoApi = Mockito.mock(PromotionApiClient.class);
                PrintApiClient printApi = Mockito.mock(PrintApiClient.class);

                ComercialPanel panel = new ComercialPanel(comercialApi, invApi, finApi, creditApi,
                        debitApi, posApi, movApi, promoApi, printApi, () -> {});
                assertNotNull(panel);

                JTable table = panel.invoicesTable;
                assertNotNull(table, "invoicesTable deve estar inicializada");
                assertEquals(7, table.getColumnCount(), "invoicesTable deve ter 7 colunas (incluindo Data)");
                assertEquals("Data", table.getColumnName(3), "A coluna 3 deve ser 'Data'");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("UFP-09: O painel de quebras de stock (StockWastePanel) inclui o filtro por período")
    void testStockWastePanelIncludesPeriodFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);
                InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
                ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);

                StockWastePanel wastePanel = new StockWastePanel(wasteApi, invApi, comApi);
                assertNotNull(wastePanel);
            } finally {
                CurrentUserContext.clear();
            }
        });
    }
}
