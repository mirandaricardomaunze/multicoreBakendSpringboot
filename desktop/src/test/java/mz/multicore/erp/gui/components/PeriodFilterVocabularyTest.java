package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os dois vocabulários de período — e porque é que não podem ser um só.
 *
 * <p>Uma coluna de <b>emissão</b> olha para trás: "Últimos 30 dias". Uma coluna de <b>validade</b>
 * olha para a frente: "Vence em 30 dias". Usar o primeiro na segunda não é uma imprecisão — é a
 * pergunta ao contrário. Numa tabela de lotes mostraria o que <i>já</i> venceu e esconderia
 * exactamente aquilo que quem gere validades procura.
 *
 * <p>A lógica é pura ({@code matchesPeriod}), por isso testa-se sem abrir um ecrã. É a parte onde
 * um engano custa caro: um lote a vencer que não aparece na lista é um lote que se perde.
 */
class PeriodFilterVocabularyTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 30);

    // ─── Passado: emissão, pagamento, movimento ──────────────────────────────

    @Test
    void backwardVocabularyLooksBackwards() {
        assertTrue(TableFilter.matchesPeriod(HOJE, "Hoje", HOJE));
        assertTrue(TableFilter.matchesPeriod(HOJE.minusDays(6), "Últimos 7 dias", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE.minusDays(7), "Últimos 7 dias", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE.plusDays(1), "Últimos 7 dias", HOJE),
                "uma data futura não pertence aos últimos 7 dias");
    }

    // ─── Futuro: validade ────────────────────────────────────────────────────

    @Test
    void expiryVocabularyLooksForwards() {
        assertTrue(TableFilter.matchesPeriod(HOJE, "Vence em 7 dias", HOJE),
                "o que vence hoje é o mais urgente de todos");
        assertTrue(TableFilter.matchesPeriod(HOJE.plusDays(7), "Vence em 7 dias", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE.plusDays(8), "Vence em 7 dias", HOJE));
        assertTrue(TableFilter.matchesPeriod(HOJE.plusDays(89), "Vence em 90 dias", HOJE));
    }

    /** Já vencido é outra pergunta e outra urgência — por isso tem opção própria. */
    @Test
    void alreadyExpiredIsItsOwnQuestion() {
        assertTrue(TableFilter.matchesPeriod(HOJE.minusDays(1), "Já vencidos", HOJE));
        assertFalse(TableFilter.matchesPeriod(HOJE, "Já vencidos", HOJE),
                "o que vence hoje ainda não venceu");
        assertFalse(TableFilter.matchesPeriod(HOJE.minusDays(1), "Vence em 30 dias", HOJE),
                "o que já venceu não conta como 'a vencer'");
    }

    /**
     * O erro que este vocabulário existe para evitar: um lote que vence daqui a uma semana
     * <b>desaparecia</b> de um filtro construído para o passado.
     */
    @Test
    void theBackwardVocabularyWouldHideABatchAboutToExpire() {
        LocalDate venceDaquiAUmaSemana = HOJE.plusDays(7);

        assertFalse(TableFilter.matchesPeriod(venceDaquiAUmaSemana, "Últimos 30 dias", HOJE),
                "com o vocabulário do passado, um lote a vencer sai da lista — e é o que se procura");
        assertTrue(TableFilter.matchesPeriod(venceDaquiAUmaSemana, "Vence em 7 dias", HOJE));
    }
}
