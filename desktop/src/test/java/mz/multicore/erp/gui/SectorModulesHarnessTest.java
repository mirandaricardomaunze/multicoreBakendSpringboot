package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.SidebarNavItem;
import mz.multicore.erp.gui.sector.ModulePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Sector modules as first-class screens: own cards, titles, sidebar entries and profile-driven menu. */
class SectorModulesHarnessTest {

    private static SectorModules modules() {
        return SectorModules.create(null, null, null, mock(ComercialApiClient.class), mock(InventoryApiClient.class),
                mock(PromotionApiClient.class), null, mock(UnitsApiClient.class), mock(PricingApiClient.class),
                mock(PickupApiClient.class), mock(BulkStockApiClient.class), mock(ProjectsApiClient.class),
                mock(AdvancesApiClient.class), mock(FinanceApiClient.class), mock(ButcheryApiClient.class),
                mock(ExpiryApiClient.class));
    }

    @Test
    @DisplayName("SM-01: Preços, Levantamentos, Obras and Adiantamentos are their own pages with a module header")
    void modulesHaveOwnPages() {
        Map<String, JComponent> cards = new LinkedHashMap<>();
        modules().registerCards((page, card) -> cards.put(card, page));
        assertThat(cards.keySet()).containsExactlyInAnyOrder("precos", "levantamentos", "obras", "adiantamentos", "talho", "validades");
        cards.values().forEach(page -> assertThat(page).isInstanceOf(ModulePage.class));
        assertThat(MainFrameModuleTitles.titleFor("levantamentos")).isEqualTo("Levantamentos");
        assertThat(MainFrameModuleTitles.titleFor("obras")).isEqualTo("Obras");
        assertThat(MainFrameModuleTitles.titleFor("precos")).isEqualTo("Preços");
        assertThat(MainFrameModuleTitles.titleFor("adiantamentos")).isEqualTo("Adiantamentos");
        assertThat(MainFrameModuleTitles.titleFor("stock")).isEqualTo("Stock");
    }

    @Test
    @DisplayName("SM-02: Sellers see Levantamentos and Obras but not Preços nor Adiantamentos (they issue invoices)")
    void sellerSidebar() {
        SectorModules modules = modules();
        CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Teste");
        modules.addSidebarItem(sidebar, SectorModules.PICKUPS, true, card -> { });
        modules.addSidebarItem(sidebar, SectorModules.PROJECTS, true, card -> { });
        modules.addSidebarItem(sidebar, SectorModules.PRICES, true, card -> { });
        modules.addSidebarItem(sidebar, SectorModules.ADVANCES, true, card -> { });
        List<SidebarNavItem> items = sidebar.getNavItems();
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("SM-03: The activity profile hides modules the business does not use; no profile shows all")
    void profileDrivesMenu() {
        SectorModules modules = modules();
        CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Teste");
        for (String card : List.of(SectorModules.PRICES, SectorModules.PICKUPS, SectorModules.PROJECTS, SectorModules.ADVANCES)) {
            modules.addSidebarItem(sidebar, card, false, c -> { });
        }
        List<SidebarNavItem> items = sidebar.getNavItems();
        modules.applyCapabilities(List.of("PRICE_LISTS"));
        assertThat(items.stream().filter(SidebarNavItem::isVisible).count()).as("general retail: only Preços").isEqualTo(1);
        modules.applyCapabilities(List.of("PRICE_LISTS", "PENDING_PICKUP", "CUSTOMER_PROJECTS"));
        assertThat(items.stream().filter(SidebarNavItem::isVisible).count()).as("building materials: three").isEqualTo(3);
        modules.applyCapabilities(List.of("PRICE_LISTS", "ADVANCE_PAYMENTS"));
        assertThat(items.stream().filter(SidebarNavItem::isVisible).count()).as("bakery: prices and advances").isEqualTo(2);
        modules.applyCapabilities(List.of());
        assertThat(items.stream().filter(SidebarNavItem::isVisible).count()).as("unknown profile: show all").isEqualTo(4);
    }

    @Test
    @DisplayName("SM-04: Talho shows only for the butcher capability; a bakery with production does not see it")
    void butcheryFollowsItsOwnCapability() {
        SectorModules modules = modules();
        CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Teste");
        modules.addSidebarItem(sidebar, SectorModules.BUTCHERY, true, c -> { });
        SidebarNavItem talho = sidebar.getNavItems().get(0);
        modules.applyCapabilities(List.of("PRODUCTION", "CUSTOM_ORDERS"));
        assertThat(talho.isVisible()).isFalse();
        modules.applyCapabilities(List.of("CARCASS_BREAKDOWN"));
        assertThat(talho.isVisible()).isTrue();
        assertThat(MainFrameModuleTitles.titleFor("talho")).isEqualTo("Talho");
    }
}
