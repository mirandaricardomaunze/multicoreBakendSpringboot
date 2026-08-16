package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.awt.Cursor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressão do cursor de loading preso: o {@code loadAsync} guardava o cursor anterior da
 * janela e repunha-o no fim. Com dois carregamentos sobrepostos (um painel que carrega várias
 * tabelas), o segundo guardava o cursor de <i>espera</i> do primeiro e repunha-o — a janela
 * ficava com o cursor azul de loading por cima da aplicação, para sempre.
 */
class LoadingCursorTest {

    private static final Cursor WAIT = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private static final Cursor DEFAULT = Cursor.getDefaultCursor();

    @Test // LC-01
    void carregamentosSobrepostos_soLibertamOCursorNoFim() {
        JFrame window = new JFrame();
        try {
            UIHelper.beginLoading(window);   // painel começa a carregar a 1.ª tabela
            assertEquals(WAIT, window.getCursor());

            UIHelper.beginLoading(window);   // 2.ª tabela entra enquanto a 1.ª ainda corre
            assertEquals(WAIT, window.getCursor());

            UIHelper.endLoading(window);     // a 1.ª termina — ainda há uma em curso
            assertEquals(WAIT, window.getCursor(), "ainda há um carregamento activo");

            UIHelper.endLoading(window);     // a última termina
            assertEquals(DEFAULT, window.getCursor(), "o cursor tem de voltar ao normal");
            assertEquals(0, UIHelper.activeLoads(window));
        } finally {
            window.dispose();
        }
    }

    @Test // LC-02
    void carregamentoSimples_repoeOCursor() {
        JFrame window = new JFrame();
        try {
            UIHelper.beginLoading(window);
            UIHelper.endLoading(window);

            assertEquals(DEFAULT, window.getCursor());
        } finally {
            window.dispose();
        }
    }

    @Test // LC-03
    void endLoadingASolta_naoDesequilibraAContagem() {
        JFrame window = new JFrame();
        try {
            UIHelper.endLoading(window); // erro que chega sem begin correspondente
            assertEquals(0, UIHelper.activeLoads(window));

            UIHelper.beginLoading(window);
            assertEquals(WAIT, window.getCursor(), "a contagem não pode ter ficado negativa");
            UIHelper.endLoading(window);
            assertEquals(DEFAULT, window.getCursor());
        } finally {
            window.dispose();
        }
    }

    @Test // LC-04
    void janelaNula_naoRebenta() {
        assertDoesNotThrow(() -> {
            UIHelper.beginLoading(null);
            UIHelper.endLoading(null);
        });
        assertEquals(0, UIHelper.activeLoads(null));
    }
}
