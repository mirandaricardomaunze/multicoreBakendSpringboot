package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nenhum tipo de falta pode chegar ao ecrã em inglês.
 *
 * <p>Chegavam: a tabela de Faltas mostrava <i>"PENDING_JUSTIFIC…"</i> — enum cru, em inglês e
 * truncado na mesma célula — e o diálogo de justificação dizia <i>"hoje PENDING_JUSTIFICATION"</i>.
 * Num módulo cujas mensagens são todas em PT-MZ, e num ecrã que existe para ser lido por quem faz
 * folhas de salário.
 *
 * <p>Esta guarda existe porque o custo de um tipo novo é assimétrico: acrescentar
 * {@code SUSPENSION} no backend não parte nada, não falha nenhum teste, e aparece no ecrã do
 * cliente em inglês.
 */
class AbsenceTypeLabelsTest {

    /**
     * Os tipos que o backend produz hoje. Duplicados aqui de propósito: o desktop não vê o modelo
     * do backend — é essa a fronteira dos módulos — pelo que a lista é um <b>contrato escrito</b>.
     * Um tipo novo do lado do backend obriga a mexer aqui, e é esse o aviso.
     */
    private static final List<String> ABSENCE_TYPES = List.of(
            "PENDING_JUSTIFICATION", "JUSTIFIED", "UNJUSTIFIED", "SICK", "MATERNITY", "UNPAID_LEAVE");

    @Test
    void everyAbsenceTypeHasAPortugueseLabel() {
        for (String type : ABSENCE_TYPES) {
            String label = UIHelper.humanStatus(type);
            assertFalse(label.equalsIgnoreCase(type.replace('_', ' ')),
                    "o tipo de falta \"" + type + "\" não tem tradução e chega ao ecrã em inglês");
            assertFalse(label.contains("_"), "\"" + label + "\" ainda parece um enum");
        }
    }

    /** Traduções distintas para tipos distintos: dois rótulos iguais tornam o filtro ambíguo. */
    @Test
    void labelsAreDistinctSoTheColumnFilterStaysUnambiguous() {
        Map<String, String> byLabel = new HashMap<>();
        for (String type : ABSENCE_TYPES) {
            String label = UIHelper.humanStatus(type);
            String clash = byLabel.put(label, type);
            assertTrue(clash == null,
                    "\"" + label + "\" serve " + clash + " e " + type + " — o filtro por tipo "
                            + "compara texto exacto e passaria a apanhar os dois");
        }
    }

    /**
     * Tipos de movimento de stock: apareciam em bruto — PURCHASE, SALE, ADJUSTMENT — na coluna
     * "Tipo Mov." da rastreabilidade. O renderer já os humanizava; faltavam os casos.
     */
    @Test
    void everyStockMovementTypeHasAPortugueseLabel() {
        for (String type : List.of("PURCHASE", "ENTRY", "SALE", "TRANSFER",
                                   "ADJUSTMENT", "RETURN", "REVERSAL")) {
            String label = UIHelper.humanStatus(type);
            assertFalse(label.equalsIgnoreCase(type),
                    "o tipo de movimento \"" + type + "\" chega ao ecrã em inglês");
        }
    }

    /** Um ano não leva separador de milhares. A Área Fiscal mostrava "2.026". */
    @Test
    void yearSpinnerHasNoThousandsSeparator() {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");
        javax.swing.JSpinner spinner = UIHelper.createYearSpinner(2026);
        javax.swing.JSpinner.NumberEditor editor = (javax.swing.JSpinner.NumberEditor) spinner.getEditor();
        assertEquals("2026", editor.getTextField().getText());
    }

    /**
     * O contrato que torna a correcção necessária: o filtro por coluna compara <b>texto exacto</b>
     * com o valor do modelo, não com o que o renderer desenha.
     *
     * <p>Foi isto que partiu quando a coluna passou a mostrar português e o dropdown continuou com
     * os enums: o filtro deixou de encontrar linha nenhuma. Quem humanizar uma coluna tem de
     * humanizar o filtro dela — e este teste di-lo por escrito.
     */
    @Test
    void columnFilterComparesAgainstTheModelValueNotTheRenderedText() {
        List<String> row = List.of("1", "Maria Santos", "Por justificar");

        assertTrue(TableFilter.rowMatches(row, "", Map.of(2, "Por justificar")),
                "o rótulo mostrado tem de casar com o filtro");
        assertFalse(TableFilter.rowMatches(row, "", Map.of(2, "PENDING_JUSTIFICATION")),
                "o enum cru NÃO casa com a coluna humanizada — é exactamente esta a armadilha");
    }
}
