package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JTabbedPane;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A barra de separadores do Comercial tem de caber na largura validada.
 *
 * <p>O painel usa {@code SCROLL_TAB_LAYOUT} (escolha deliberada — ver
 * {@code UIHelper.styleTabbedPaneMulticore}), pelo que os separadores que não couberem <b>não
 * partem nem encolhem: desaparecem</b> atrás de setas. Ao acrescentar o separador das Cotações, o
 * total passou para 1563 px e a <b>Faturação</b> — o ecrã mais usado do módulo — deixou de estar
 * visível a 1382×736, a resolução que a iteração de 2026-08-15 validou ao vivo.
 *
 * <p>Um separador escondido não dá erro nem avisa: só desaparece. Daí esta guarda, que falha no
 * teste em vez de falhar no balcão.
 */
class CommercialTabStripFitsTest {

    /** Largura validada em 2026-08-15 ("Comercial, RH e Stock sem cortes/sobreposições"). */
    private static final int VALIDATED_WIDTH = 1382;

    /** Cromo de cada separador: ícone de 16 px, espaço para o texto e margens laterais. */
    private static final int ICON = 16;
    private static final int GAP = 6;
    private static final int PADDING = 30;

    @Test
    void commercialTabsFitTheValidatedWindowWidth() throws IOException {
        List<String> titles = tabTitles("ComercialPanel.java");
        assertThat(titles).as("separadores encontrados").hasSizeGreaterThan(5);

        JTabbedPane pane = new JTabbedPane();
        Font font = new Font("Segoe UI", Font.BOLD, 13);
        FontMetrics metrics = pane.getFontMetrics(font);

        int total = 0;
        StringBuilder detail = new StringBuilder();
        for (String title : titles) {
            int width = metrics.stringWidth(title) + ICON + GAP + PADDING;
            total += width;
            detail.append("\n  ").append(title).append(" → ").append(width).append(" px");
        }

        assertThat(total)
                .as("largura total dos %d separadores do Comercial (cabe em %d px?)%s",
                        titles.size(), VALIDATED_WIDTH, detail)
                .isLessThanOrEqualTo(VALIDATED_WIDTH);
    }

    private static List<String> tabTitles(String panel) throws IOException {
        String source = Files.readString(
                Path.of("src", "main", "java", "mz", "multicore", "erp", "gui").resolve(panel));
        Matcher matcher = Pattern.compile("addTab\\(\"([^\"]+)\"").matcher(source);
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            titles.add(matcher.group(1));
        }
        return titles;
    }
}
