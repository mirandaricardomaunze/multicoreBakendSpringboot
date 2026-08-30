package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O campo de data tem de <b>ler</b> o que a aplicação lhe <b>escreve</b>.
 *
 * <p>Não lia. O Balancete e o Razão da Contabilidade pré-preenchiam os campos em dd/MM/yyyy e o
 * {@code value()} só aceita ISO — por contrato. Resultado: "Calcular" falhava <b>à primeira</b>,
 * com o ecrã acabado de abrir e o utilizador sem ter tocado em nada. O defeito era o painel, não
 * o campo.
 *
 * <p>Só se descobriu depois de corrigir a largura do campo: enquanto ele tinha 20 px, ninguém via
 * que data lá estava. Um defeito escondia o outro.
 */
class DateFieldTest {

    @Test
    void readsIsoFormat() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");
        DateField field = new DateField();
        field.setText("2026-08-01");
        assertEquals(LocalDate.of(2026, 8, 1), field.value());
    }

    /**
     * O formato humano continua a ser RECUSADO — é contrato, e o {@code CanonicalFormFieldsTest}
     * já o carregava. Quase o mudei ao corrigir o Balancete; o teste dele apanhou-me. O defeito
     * era o painel escrever no formato errado, não o campo ser exigente.
     */
    @Test
    void rejectsTheHumanFormatBecauseTheContractIsIso() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");
        DateField field = new DateField();
        field.setText("01/08/2026");
        assertThrows(IllegalArgumentException.class, field::value);
    }

    /** Largura medida antes de haver texto era o defeito irmão: o campo nascia do tamanho do vazio. */
    @Test
    void isWideEnoughForADateEvenWhenBuiltEmpty() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");
        DateField field = new DateField();
        int needed = field.getFontMetrics(field.getFont()).stringWidth("01/08/2026");
        assertTrue(field.getPreferredSize().width >= needed,
                String.format("campo com %d px não mostra uma data de %d px",
                        field.getPreferredSize().width, needed));
    }

    @Test
    void rejectsRubbishAndSaysBothFormats() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");
        DateField field = new DateField();
        field.setText("ontem");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, field::value);
        assertTrue(error.getMessage().contains("yyyy-MM-dd"));
    }
}
