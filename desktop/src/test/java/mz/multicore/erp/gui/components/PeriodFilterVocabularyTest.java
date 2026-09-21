package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O vocabulário de período olha para <b>trás</b> — e é preciso que se saiba.
 *
 * <p>{@code periodCombo()} serve colunas de datas passadas: emissão, pagamento, movimento. Aplicá-lo
 * a uma coluna de <b>validade</b> seria pior do que não ter filtro: "Últimos 30 dias" mostraria o
 * que <i>já</i> venceu no mês passado e esconderia exactamente o que quem gere lotes procura.
 *
 * <p>É por isso que o separador <i>Lotes &amp; Validades</i> tem filtro próprio, com vocabulário
 * para a frente ({@code Vence em ≤ 30 dias}, {@code Válidos (> 90 dias)}) — ver
 * docs/FILTRO_DATA_TABELAS_SPEC.md §4.
 *
 * <p>A lógica é pura, por isso testa-se sem abrir um ecrã. É onde um engano custa caro: uma linha
 * que devia aparecer e não aparece não dá erro nenhum.
 */
class PeriodFilterVocabularyTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 30);

    @Test
    void backwardVocabularyLooksBackwards() {
        assertTrue(TableFilter.matchesPeriod(HOJE, "Hoje", HOJE));
        assertTrue(TableFilter.matchesPeriod(HOJE.minusDays(6), "Últimos 7 dias", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE.minusDays(7), "Últimos 7 dias", HOJE));
    }

    /** Um documento pós-datado não pode aparecer como recente. */
    @Test
    void futureDatesDoNotBelongToPastRanges() {
        assertFalse(TableFilter.matchesPeriod(HOJE.plusDays(1), "Últimos 7 dias", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE.plusDays(1), "Últimos 30 dias", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE.plusDays(1), "Hoje", HOJE));
    }

    @Test
    void thisMonthIsCalendarMonthAndNotThirtyDays() {
        LocalDate primeiroDoMes = LocalDate.of(2026, 8, 1);
        assertTrue(TableFilter.matchesPeriod(primeiroDoMes, "Este mês", HOJE));
        assertFalse(TableFilter.matchesPeriod(LocalDate.of(2026, 7, 31), "Este mês", HOJE),
                "31 de Julho está dentro dos últimos 30 dias, mas não é 'este mês'");
    }

    /** "Todo o período" não filtra nada — nem sequer linhas com data ilegível. */
    @Test
    void everythingPassesWhenNoPeriodIsChosen() {
        assertTrue(TableFilter.matchesPeriod(null, "Todo o período", HOJE));
        assertTrue(TableFilter.matchesPeriod(HOJE.minusYears(5), "Todo o período", HOJE));
    }

    /** Mas com período escolhido, uma data que não se leu não pode passar por engano. */
    @Test
    void unreadableDateIsExcludedWhenAPeriodIsChosen() {
        assertFalse(TableFilter.matchesPeriod(null, "Hoje", HOJE));
    }

    /**
     * A razão de o separador de lotes não usar este vocabulário: um lote a vencer daqui a uma
     * semana <b>desapareceria</b> da lista.
     */
    @Test
    void theBackwardVocabularyWouldHideABatchAboutToExpire() {
        assertFalse(TableFilter.matchesPeriod(HOJE.plusDays(7), "Últimos 30 dias", HOJE),
                "por isso as validades têm filtro próprio, e não este");
    }
}
