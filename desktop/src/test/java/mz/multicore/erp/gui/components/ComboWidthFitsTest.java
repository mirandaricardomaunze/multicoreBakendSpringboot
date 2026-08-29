package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O valor escolhido de um selector tem de <b>caber</b> no selector.
 *
 * <p>Parece óbvio e esteve errado em toda a aplicação. O {@code styleComboBox} fixava a largura
 * <b>antes</b> de instalar o renderer, e o renderer acrescenta 8 px de cada lado: a largura ficava
 * 16 px curta e o Swing truncava o texto. O resultado era visível em todas as tabelas paginadas —
 * <i>"Por página: …"</i> em vez de <i>"Por página: 50"</i> — e em todos os filtros de estado —
 * <i>"Todos os esta…"</i> em vez de <i>"Todos os estados"</i>.
 *
 * <p>Nenhum teste apanhava isto porque nenhum teste <b>media</b> um ecrã pintado; e o compilador
 * nunca apanha uma largura. Esta guarda mede.
 */
class ComboWidthFitsTest {

    /**
     * A invariante, dita directamente: a largura que o {@code styleComboBox} <b>congela</b> tem de
     * ser pelo menos a largura <b>natural</b> do combo já com o renderer instalado.
     *
     * <p>Medir assim — desfazendo o congelamento e voltando a perguntar ao Swing — é o que apanha o
     * defeito. Duas versões anteriores deste teste calculavam a largura à mão e passavam contra o
     * código partido, porque a conta à mão ignorava a seta e as margens do próprio combo. O Swing
     * sabe medir-se; o teste só tem de lhe perguntar duas vezes.
     */
    @Test
    void frozenWidthIsNotNarrowerThanTheRendererNeeds() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");

        JComboBox<String> combo = new JComboBox<>(new String[]{
                "Todos os estados", "Activo", "Liquidado", "Desactivado"});
        UIHelper.styleComboBox(combo);

        int frozen = combo.getPreferredSize().width;
        combo.setPreferredSize(null);
        int natural = combo.getPreferredSize().width;

        assertTrue(frozen >= natural, String.format(
                "largura congelada %d px < %d px de que o combo precisa com o renderer instalado — "
                        + "o valor aparece truncado com reticências", frozen, natural));
    }

    /** O caso concreto que se via em todas as tabelas paginadas do sistema. */
    @Test
    void pageSizeComboShowsTheNumberAndNotEllipsis() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");

        JComboBox<Integer> pageSize = new JComboBox<>(new Integer[]{25, 50, 100, 200});
        UIHelper.styleComboBox(pageSize);
        pageSize.setSelectedItem(50);

        int frozen = pageSize.getPreferredSize().width;
        pageSize.setPreferredSize(null);
        assertTrue(frozen >= pageSize.getPreferredSize().width,
                "o selector de registos por página mostrava \"…\" em vez do número");
    }

    /**
     * Um valor de conjunto fechado tem de caber, não só o cabeçalho.
     *
     * <p>Nas Contas Correntes via-se "Corrente (por …" — e todos os escalões de antiguidade começam
     * por "Corrente" ou por um número, pelo que a célula cortada não distinguia nada de nada.
     */
    @Test
    void ensureColumnFitsMakesRoomForTheWidestKnownValue() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");

        JTable table = new JTable(new DefaultTableModel(new String[]{"Nº", "Antiguidade"}, 0));
        UIHelper.styleTable(table);
        UIHelper.ensureColumnFits(table, 1, "Corrente (por vencer)");

        int needed = table.getFontMetrics(table.getFont()).stringWidth("Corrente (por vencer)");
        assertTrue(table.getColumnModel().getColumn(1).getMinWidth() >= needed,
                "o escalão mais longo continua a não caber");
    }

    /** Cabeçalho de coluna truncado numa coluna de dinheiro é a diferença entre ler e adivinhar. */
    @Test
    void ensureHeadersFitKeepsMoneyHeadersReadable() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "precisa de ambiente gráfico");

        JTable table = new JTable(new DefaultTableModel(
                new String[]{"ID", "Colaborador", "Descontado (MT)", "Em Dívida (MT)"}, 0));
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);

        var metrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            var column = table.getColumnModel().getColumn(i);
            int needed = metrics.stringWidth(String.valueOf(column.getHeaderValue()));
            assertTrue(column.getMinWidth() >= needed,
                    "cabeçalho \"" + column.getHeaderValue() + "\" não cabe na coluna");
        }
    }

}
