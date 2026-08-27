package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Testes da lógica pura de {@link TableFilter#rowMatches}. */
class TableFilterTest {

    private final List<String> row = List.of("SKU1", "Arroz Agulha", "Loja", "Aberto");

    @Test
    void ft01_pesquisaEmQualquerColuna_caseInsensitive() {
        assertTrue(TableFilter.rowMatches(row, "arroz", Map.of()));
        assertTrue(TableFilter.rowMatches(row, "SKU1", Map.of()));
        assertFalse(TableFilter.rowMatches(row, "feijao", Map.of()));
    }

    @Test
    void ft02_pesquisaVazia_passaTudo() {
        assertTrue(TableFilter.rowMatches(row, "", Map.of()));
        assertTrue(TableFilter.rowMatches(row, null, Map.of()));
    }

    @Test
    void ft03_filtroDeColuna_exactoEIgnoraCaso() {
        assertTrue(TableFilter.rowMatches(row, "", Map.of(3, "Aberto")));
        assertTrue(TableFilter.rowMatches(row, "", Map.of(3, "aberto")));
        assertFalse(TableFilter.rowMatches(row, "", Map.of(3, "Fechado")));
    }

    @Test
    void ft04_pesquisaEColunaCombinam() {
        assertTrue(TableFilter.rowMatches(row, "arroz", Map.of(3, "Aberto")));
        assertFalse(TableFilter.rowMatches(row, "arroz", Map.of(3, "Fechado")));
        assertFalse(TableFilter.rowMatches(row, "feijao", Map.of(3, "Aberto")));
    }

    @Test
    void ft05_valorDeColunaVazio_naoFiltra() {
        assertTrue(TableFilter.rowMatches(row, "", java.util.Collections.singletonMap(3, "")));
    }

    @Test
    void ft06_parseCellDate_aceitaDataComOuSemHora() {
        assertEquals(java.time.LocalDate.of(2026, 7, 6), TableFilter.parseCellDate("06/07/2026"));
        assertEquals(java.time.LocalDate.of(2026, 7, 6), TableFilter.parseCellDate("06/07/2026 14:30"));
        assertNull(TableFilter.parseCellDate("sem data"));
        assertNull(TableFilter.parseCellDate(null));
    }

    @Test
    void ft07_matchesPeriod_hojeUltimosDiasEMes() {
        java.time.LocalDate hoje = java.time.LocalDate.of(2026, 7, 6);
        assertTrue(TableFilter.matchesPeriod(hoje, "Todo o período", hoje));
        assertTrue(TableFilter.matchesPeriod(hoje, "Hoje", hoje));
        assertFalse(TableFilter.matchesPeriod(hoje.minusDays(1), "Hoje", hoje));
        assertTrue(TableFilter.matchesPeriod(hoje.minusDays(6), "Últimos 7 dias", hoje));
        assertFalse(TableFilter.matchesPeriod(hoje.minusDays(7), "Últimos 7 dias", hoje));
        assertTrue(TableFilter.matchesPeriod(hoje.minusDays(3), "Este mês", hoje)); // 03/07 — mesmo mês
        assertFalse(TableFilter.matchesPeriod(hoje.minusDays(20), "Este mês", hoje)); // 16/06 — mês anterior
        assertFalse(TableFilter.matchesPeriod(hoje.minusMonths(2), "Este mês", hoje));
        assertFalse(TableFilter.matchesPeriod(null, "Hoje", hoje)); // sem data e com filtro ⇒ fora
    }
}
