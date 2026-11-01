package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.SidebarNavItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/UI_ORGANIZACAO_NAVEGACAO_SPEC.md. */
class SidebarActiveItemTest {

    /** Ecrã e título tal como MainFrame.navigate os define. */
    private static final Map<String, String> ADMIN_SCREENS = Map.ofEntries(
            Map.entry("dashboard", "Painel Inicial"), Map.entry("pos", "POS — Caixa"),
            Map.entry("comercial", "Vendas & Faturação"), Map.entry("compras", "Compras"),
            Map.entry("stock", "Stock & Armazéns"), Map.entry("financeiro", "Tesouraria"),
            Map.entry("hr", "Recursos Humanos"), Map.entry("desempenho", "Desempenho Comercial"),
            Map.entry("crm", "CRM & Assistência"), Map.entry("clientes", "Clientes"),
            Map.entry("fiscal", "Área Fiscal"), Map.entry("contabilidade", "Contabilidade"),
            Map.entry("assets", "Activos & Património"), Map.entry("manufacturing", "Produção & Fabrico"),
            Map.entry("fleet", "Gestão de Frotas"), Map.entry("recurring", "Avenças & Contratos"),
            Map.entry("quality", "Qualidade & HST"), Map.entry("approvals", "Aprovações"),
            Map.entry("auditoria_forense", "Auditoria Forense"), Map.entry("assistente_ia", "Assistente de Gestão"),
            Map.entry("config", "Configurações"), Map.entry("notifications", "Notificações"));
    private static final Map<String, String> SELLER_SCREENS = Map.of(
            "dashboard", "Painel Inicial", "pos", "POS — Caixa", "comercial", "Vendas & Faturação",
            "clientes", "Clientes", "crm", "CRM & Assistência", "notifications", "Notificações");

    @Test
    @DisplayName("Todos os ecrãs do administrador têm item activo no menu")
    void adminScreensHaveItem() throws Exception {
        assertEveryScreenHasItem("ADMIN", ADMIN_SCREENS);
    }

    @Test
    @DisplayName("Todos os ecrãs do vendedor têm item activo no menu")
    void sellerScreensHaveItem() throws Exception {
        assertEveryScreenHasItem("SELLER", SELLER_SCREENS);
    }

    @Test
    @DisplayName("Em Vendas, o item segue a aba aberta")
    void commercialFollowsTab() {
        assertThat(SidebarActiveItem.labelFor("comercial", "Vendas & Faturação", "ADMIN", "Movimentos"))
                .isEqualTo("Movimentos");
        assertThat(SidebarActiveItem.labelFor("comercial", "Vendas & Faturação", "ADMIN", "Faturação"))
                .isEqualTo("Pedidos");
    }

    private static void assertEveryScreenHasItem(String role, Map<String, String> screens) throws Exception {
        Set<String> labels = menuLabels(role);
        screens.forEach((card, title) -> assertThat(labels)
                .as("item activo de %s para %s", card, role)
                .contains(SidebarActiveItem.labelFor(card, title, role, "Cotações")));
    }

    private static Set<String> menuLabels(String role) throws Exception {
        AtomicReference<Set<String>> labels = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            DesktopSession session = new DesktopSession("t", Instant.now().plusSeconds(60), "ana", "Ana", false,
                    List.of(new DesktopSession.CompanyAccess(1L, "Empresa", role)));
            session.selectCompany(1L);
            CollapsibleSidebar sidebar = MainFrameSidebarBuilder.build(session, false, card -> {}, null);
            labels.set(sidebar.getNavItems().stream().map(SidebarNavItem::getLabel).collect(Collectors.toSet()));
        });
        return labels.get();
    }
}
