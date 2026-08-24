package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JTabbedPane;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As barras de separadores têm de caber na largura validada.
 *
 * <p>Os painéis usam {@code SCROLL_TAB_LAYOUT} (escolha deliberada — ver
 * {@code UIHelper.styleTabbedPaneMulticore}), pelo que os separadores que não couberem <b>não
 * partem nem encolhem: desaparecem</b> atrás de setas. Ao acrescentar o separador das Cotações, o
 * total do Comercial passou para 1563 px e a <b>Faturação</b> — o ecrã mais usado do módulo —
 * deixou de estar visível a 1382×736, a resolução que a iteração de 2026-08-15 validou ao vivo.
 *
 * <p>Um separador escondido não dá erro nem avisa: só desaparece. Daí esta guarda, que falha no
 * teste em vez de falhar no balcão.
 *
 * <p><b>Passou a cobrir o RH em 2026-08-24</b>, quando o bloco das retenções, dos descontos e das
 * cessações lhe acrescentou três separadores de uma vez: o RH tinha exactamente o mesmo risco e
 * nenhuma guarda. Foi por causa desta medição que "Notas de Despesas" passou a "Despesas".
 */
class TabStripFitsTest {

    /** Largura validada em 2026-08-15 ("Comercial, RH e Stock sem cortes/sobreposições"). */
    private static final int VALIDATED_WIDTH = 1382;

    /** Cromo de cada separador: ícone de 16 px, espaço para o texto e margens laterais. */
    private static final int ICON = 16;
    private static final int GAP = 6;
    private static final int PADDING = 30;

    @Test
    void commercialTabsFitTheValidatedWindowWidth() throws IOException {
        assertTabsFit("ComercialPanel.java", "Comercial");
    }

    @Test
    void hrTabsFitTheValidatedWindowWidth() throws IOException {
        assertTabsFit("HRPanel.java", "RH");
    }

    private void assertTabsFit(String panel, String moduleName) throws IOException {
        Map<String, List<String>> strips = tabStrips(panel);
        assertThat(strips).as("barras de separadores encontradas em %s", panel).isNotEmpty();
        assertThat(strips.values().stream().mapToInt(List::size).max().orElse(0))
                .as("a barra principal de %s", moduleName).isGreaterThan(5);

        JTabbedPane pane = new JTabbedPane();
        Font font = new Font("Segoe UI", Font.BOLD, 13);
        FontMetrics metrics = pane.getFontMetrics(font);

        strips.forEach((receiver, titles) -> {
            int total = 0;
            StringBuilder detail = new StringBuilder();
            for (String title : titles) {
                int width = metrics.stringWidth(title) + ICON + GAP + PADDING;
                total += width;
                detail.append("\n  ").append(title).append(" → ").append(width).append(" px");
            }
            assertThat(total)
                    .as("largura da barra \"%s\" do %s — %d separadores, cabe em %d px?%s",
                            receiver, moduleName, titles.size(), VALIDATED_WIDTH, detail)
                    .isLessThanOrEqualTo(VALIDATED_WIDTH);
        });
    }

    /**
     * Os separadores por <b>barra</b>, agrupados pela variável a que são adicionados.
     *
     * <p>Contar todos os {@code addTab} do ficheiro junta a barra do módulo com a de qualquer
     * diálogo que o painel construa — e foi assim que esta guarda deu um falso positivo no RH assim
     * que o cadastro do colaborador ganhou separadores próprios. Cada barra é medida por si: a de um
     * diálogo também não pode transbordar, só que tem muito menos por onde transbordar.
     */
    private static Map<String, List<String>> tabStrips(String panel) throws IOException {
        String source = Files.readString(
                Path.of("src", "main", "java", "mz", "multicore", "erp", "gui").resolve(panel));
        Matcher matcher = Pattern.compile("(\\w+)\\.addTab\\(\"([^\"]+)\"").matcher(source);
        Map<String, List<String>> strips = new LinkedHashMap<>();
        while (matcher.find()) {
            strips.computeIfAbsent(matcher.group(1), key -> new ArrayList<>()).add(matcher.group(2));
        }
        return strips;
    }
}
