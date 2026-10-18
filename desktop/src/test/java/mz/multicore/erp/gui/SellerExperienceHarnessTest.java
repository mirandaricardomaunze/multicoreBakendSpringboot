package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.KpiCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("Harness da experiência do Vendedor (UXV-01 a UXV-05)")
class SellerExperienceHarnessTest {

    @Test
    @DisplayName("UXV-01/02: painel dedicado usa quatro KPIs canónicos e seis acções")
    void sellerDashboard_hasCanonicalKpisAndQuickActions() {
        SellerDashboardPanel panel = new SellerDashboardPanel(
                mock(ComercialApiClient.class), mock(POSApiClient.class), ignored -> { });

        List<String> texts = texts(panel);
        assertTrue(texts.containsAll(List.of("Nova venda POS", "Nova encomenda", "Nova cotação",
                "Pesquisar clientes", "Consultar produtos", "Notificações")));
        assertEquals(4, countLabelsStartingWith(panel,
                List.of("VENDAS NA SESSÃO", "ATENDIMENTOS", "ENCOMENDAS PENDENTES", "COTAÇÕES ABERTAS")));
        assertEquals(96, KpiCard.STANDARD_CARD_HEIGHT);
    }

    @Test
    @DisplayName("UXV-03: painel não apresenta indicadores de gestão")
    void sellerDashboard_excludesManagementIndicators() {
        SellerDashboardPanel panel = new SellerDashboardPanel(
                mock(ComercialApiClient.class), mock(POSApiClient.class), ignored -> { });
        String all = String.join(" ", texts(panel)).toLowerCase();
        assertFalse(all.contains("tesouraria"));
        assertFalse(all.contains("fiscal"));
        assertFalse(all.contains("salário"));
        assertFalse(all.contains("aprovação"));
    }

    @Test
    @DisplayName("UXV-04/05: MainFrame selecciona painel e pesquisa próprios do SELLER")
    void mainFrame_routesSellerToDedicatedExperience() throws Exception {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/MainFrame.java"));
        assertTrue(source.contains("new SellerDashboardPanel"));
        assertTrue(source.contains("buildSellerSearchIndex()"));
        assertTrue(source.contains("Esta área requer perfil de gestor ou administrador"));
        assertFalse(source.substring(source.indexOf("private List<GlobalSearchDialog.SearchItem> buildSellerSearchIndex()"))
                .contains("seller_financeiro"));
    }

    private static List<String> texts(Component component) {
        List<String> result = new ArrayList<>();
        if (component instanceof JLabel label && label.getText() != null) result.add(label.getText());
        if (component instanceof AbstractButton button && button.getText() != null) result.add(button.getText());
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) result.addAll(texts(child));
        }
        return result;
    }

    private static int countLabelsStartingWith(JComponent component, List<String> prefixes) {
        return (int) texts(component).stream().filter(text -> prefixes.stream().anyMatch(text::startsWith)).count();
    }
}
