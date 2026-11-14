package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness dos nomes curtos nos separadores e dos combos com o texto inteiro (docs/SEPARADORES_SPEC.md). */
class TabNamesAndCombosHarnessTest {

    /** Espaço real num ecrã de 1366 px com o menu recolhido (igual ao TabStripFitsTest). */
    private static final int VALIDATED_WIDTH = 1270;
    private static final int TAB_CHROME = 16 + 4 + 23;

    @Test
    @DisplayName("TAB-07: o separador desenha o nome curto, mas guarda o nome completo e mostra-o no tooltip")
    void tab07_shortLabelFullTitle() throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            UIHelper.styleTabbedPaneMulticore(tabs);
            tabs.addTab("Níveis de Stock", new JPanel());
            tabs.addTab("Transferências entre Armazéns", new JPanel());
            tabs.setSize(1200, 300);
            tabs.doLayout();
            holder[0] = tabs;
        });
        JTabbedPane tabs = holder[0];
        assertThat(tabs.getTitleAt(1)).as("código e testes continuam a ver o nome completo").isEqualTo("Transferências entre Armazéns");
        assertThat(TabShortNames.of(tabs.getTitleAt(1))).isEqualTo("Transferências");
        assertThat(tabs.getToolTipTextAt(1)).isEqualTo("Transferências entre Armazéns");

        FontMetrics metrics = tabs.getFontMetrics(tabs.getFont());
        int fullWidth = metrics.stringWidth("Transferências entre Armazéns");
        int shortWidth = metrics.stringWidth("Transferências");
        assertThat(tabs.getBoundsAt(1).width).as("a largura do separador segue o nome curto")
                .isLessThan(tabs.getBoundsAt(0).width + fullWidth - shortWidth);
        assertThat(tabs.getToolTipTextAt(0)).as("nome que não muda não ganha tooltip").isNull();
    }

    @Test
    @DisplayName("TAB-08: com os nomes curtos, Configurações, Compras, Stock e Clientes cabem sem \"Mais\"")
    void tab08_mainStripsFitWithShortNames() {
        Font font = new Font("Segoe UI", Font.BOLD, 13);
        Assumptions.assumeTrue("Segoe UI".equals(font.getFamily()), "larguras validadas só com a fonte do Windows");
        FontMetrics metrics = new JPanel().getFontMetrics(font);
        List<List<String>> strips = List.of(
                List.of("Log de Auditoria Geral", "Cópias de Segurança & Backups", "Utilizadores & Permissões",
                        "Colunas dos Documentos", "Suporte à Plataforma", "A Minha Assinatura", "Perfil de Actividade",
                        "Balança deste Posto", "Gaveta deste Posto"),
                List.of("Faturas de Compra (V/FT)", "Reposição", "Encomendas a Fornecedor (EC-F)", "Contas a Pagar",
                        "Gestão de Fornecedores", "Gestão de Negócios", "Conta Corrente & Reconciliação"),
                List.of("Níveis de Stock", "Alertas", "Lotes & Validades", "Movimentos & Rastreabilidade",
                        "Transferências entre Armazéns", "Quebras & Desperdício", "Inventário Físico", "Granel"),
                List.of("Directório de Clientes", "Risco de Crédito & Cobrança (Aging)", "Conta Corrente & Reconciliação"));
        for (List<String> strip : strips) {
            int total = strip.stream().mapToInt(t -> metrics.stringWidth(TabShortNames.of(t)) + TAB_CHROME).sum();
            assertThat(total).as("barra %s", strip.get(0)).isLessThanOrEqualTo(VALIDATED_WIDTH);
        }
        assertThat(TabShortNames.all().values()).as("nomes curtos até 20 caracteres").allMatch(s -> s.length() <= 20);
    }

    @Test
    @DisplayName("TC-09: combo de filtro mostra a opção inteira mesmo com largura fixa estreita no ecrã")
    void tc09_comboShowsWholeOption() {
        JComboBox<String> business = TableFilter.combo("Todos os negócios", "Retalho Sul");
        business.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));
        JPanel bar = TableFilter.bar(TableFilter.searchField("Pesquisar…"), business);
        int natural = Math.min(business.getUI().getPreferredSize(business).width, SingleRowFilterLayout.MAX_COMBO_WIDTH);

        bar.setSize(bar.getPreferredSize());
        bar.doLayout();
        assertThat(business.getWidth()).as("largura do texto mais comprido").isGreaterThanOrEqualTo(natural);
        assertThat(business.getHeight()).as("altura fixa mantém-se").isEqualTo(UIHelper.FORM_CONTROL_HEIGHT);
    }
}
