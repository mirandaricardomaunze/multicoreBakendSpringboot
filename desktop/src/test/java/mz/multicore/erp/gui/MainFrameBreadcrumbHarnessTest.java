package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.TopNavBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.CardLayout;

import static org.assertj.core.api.Assertions.assertThat;

/** ECR-07 de docs/ECRA_CAMINHO_E_MENU_SPEC.md: o caminho segue o separador do ecrã visível. */
class MainFrameBreadcrumbHarnessTest {

    @Test
    @DisplayName("ECR-07: o caminho segue o separador seleccionado e some num ecrã sem separadores")
    void ecr07_pathFollowsSelectedTab() {
        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        JTabbedPane stockTabs = new JTabbedPane();
        stockTabs.addTab("Níveis de Stock", new JPanel());
        stockTabs.addTab("Transferências entre Armazéns", new JPanel());
        JPanel stock = new JPanel();
        stock.add(stockTabs);
        content.add(stock, "stock");
        content.add(new JPanel(), "approvals");

        TopNavBar bar = new TopNavBar("Stock", "");
        MainFrameBreadcrumb breadcrumb = new MainFrameBreadcrumb(bar, content);

        cards.show(content, "stock");
        breadcrumb.followVisibleCard();
        assertThat(bar.getSection()).isEqualTo("Níveis de Stock");

        stockTabs.setSelectedIndex(1);
        assertThat(bar.getSection()).isEqualTo("Transferências entre Armazéns");

        cards.show(content, "approvals");
        bar.setTitle("Aprovações");
        breadcrumb.followVisibleCard();
        assertThat(bar.getSection()).isNull();

        stockTabs.setSelectedIndex(0);
        assertThat(bar.getSection()).as("separador de um ecrã escondido não mexe no caminho").isNull();
    }
}
