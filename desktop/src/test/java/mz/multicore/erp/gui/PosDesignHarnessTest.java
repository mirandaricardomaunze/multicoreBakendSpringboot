package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SidebarFavoritesCoordinator;
import mz.multicore.erp.gui.components.SidebarFavoritesManager;
import mz.multicore.erp.gui.components.SidebarNavItem;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.pos.scale.PosScaleLiveWidget;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.POSCatalogItemDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/POS_DESIGN_ECRA_SPEC.md. */
class PosDesignHarnessTest {

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("PD-01: Código de barras é o primeiro campo e o mais largo")
    void pd01_barcodeComesFirst() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            assertThat(SwingUtilities.isDescendingFrom(pos.barcodeField, (Container) pos.topSelectsBar.getComponent(0))).isTrue();
            assertThat(PosLayout.HEADER_FIELD_WEIGHTS[0]).isEqualTo(
                    java.util.Arrays.stream(PosLayout.HEADER_FIELD_WEIGHTS).max().orElseThrow());

            pos.topSelectsBar.setSize(1260, 60);
            pos.topSelectsBar.doLayout();
            List<Integer> widths = java.util.Arrays.stream(pos.topSelectsBar.getComponents()).map(Component::getWidth).toList();
            assertThat(widths.get(0)).isEqualTo(java.util.Collections.max(widths));
        });
    }

    @Test
    @DisplayName("PD-02: Cabeçalho tem um só rótulo de cliente e sem asterisco")
    void pd02_oneClientLabel() throws Exception {
        onEdt(() -> {
            List<String> labels = all(pos().topSelectsBar).stream()
                    .filter(JLabel.class::isInstance).map(c -> ((JLabel) c).getText())
                    .filter(text -> text != null && !text.isBlank()).toList();
            assertThat(labels).containsExactly("Código de barras", "Cliente", "Armazém", "Conta");
        });
    }

    @Test
    @DisplayName("PD-03: Cliente sem resultados indica cliente novo, sem o seleccionar")
    void pd03_unknownClientUsesTypedName() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            pos.clientsList = List.of(client("Consumidor Final", "000000000"));
            pos.catalogController.filterClients("Joana");

            assertThat(pos.clientCombo.getSelectedItem()).isEqualTo(PosCatalogController.NEW_CLIENT_HINT);
            assertThat(pos.filteredClients).isEmpty();
        });
    }

    @Test
    @DisplayName("PD-04: NUIT só com zeros não é mostrado")
    void pd04_placeholderTaxIdIsHidden() {
        assertThat(PosLayout.clientLabel("Consumidor Final", "000000000")).isEqualTo("Consumidor Final");
        assertThat(PosLayout.clientLabel("Consumidor Final", null)).isEqualTo("Consumidor Final");
        assertThat(PosLayout.clientLabel("Loja Azul", "400123456")).isEqualTo("Loja Azul (400123456)");
    }

    @Test
    @DisplayName("PD-05: Acções do carrinho acompanham a existência de artigos")
    void pd05_cartActionsFollowItems() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            pos.applySessionState(Optional.of(session()));
            assertThat(pos.cartLineButtons).hasSize(5).noneMatch(JButton::isEnabled);
            assertThat(pos.checkoutBtn.isEnabled()).isFalse();

            pos.cartItems.add(new POSPanel.CartItem(product(1L), BigDecimal.ONE, BigDecimal.ZERO, null, null));
            pos.updateCartTotal(0);
            assertThat(pos.cartLineButtons).allMatch(JButton::isEnabled);
            assertThat(pos.checkoutBtn.isEnabled()).isTrue();
        });
    }

    @Test
    @DisplayName("PD-06: Caixa fechada bloqueia Finalizar Venda mesmo com artigos")
    void pd06_closedTillBlocksCheckout() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            pos.cartItems.add(new POSPanel.CartItem(product(1L), BigDecimal.ONE, BigDecimal.ZERO, null, null));
            pos.applySessionState(Optional.empty());

            assertThat(pos.checkoutBtn.isEnabled()).isFalse();
            assertThat(pos.checkoutBtn.getToolTipText()).contains("Abra a caixa");
        });
    }

    @Test
    @DisplayName("PD-07: Colunas do catálogo acompanham a largura")
    void pd07_catalogColumnsFollowWidth() {
        assertThat(PosLayout.catalogColumns(200)).isEqualTo(2);
        assertThat(PosLayout.catalogColumns(480)).isEqualTo(3);
        assertThat(PosLayout.catalogColumns(900)).isEqualTo(5);
    }

    @Test
    @DisplayName("PD-08: Em 1296×620 cabem 6 cartões inteiros, sem deslocação horizontal")
    void pd08_catalogFitsSmallScreen() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            pos.catalogController.applyCatalogPage(page(7, 1));
            layOut(pos, 1296, 620);

            Rectangle viewport = pos.productGridScroll.getViewport().getViewRect();
            long whole = java.util.Arrays.stream(pos.productGrid.getComponents())
                    .filter(card -> viewport.contains(card.getBounds())).count();
            assertThat(whole).isGreaterThanOrEqualTo(6);
            assertThat(pos.productGrid.getWidth()).isLessThanOrEqualTo(viewport.width);
        });
    }

    @Test
    @DisplayName("PD-09: Paginação só aparece com mais de uma página")
    void pd09_paginationOnlyWhenNeeded() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            JPanel bar = (JPanel) ((ModernPanel) pos.productGridScroll.getParent()).getComponent(1);

            pos.catalogController.applyCatalogPage(page(7, 1));
            assertThat(bar.isVisible()).isFalse();

            pos.catalogController.applyCatalogPage(page(36, 3));
            assertThat(bar.isVisible()).isTrue();
        });
    }

    @Test
    @DisplayName("PD-10: Stock inteiro não mostra decimais")
    void pd10_stockFormat() {
        assertThat(PosCatalogController.stockLabel(new BigDecimal("118.000"))).isEqualTo("118 disp.");
        assertThat(PosCatalogController.stockLabel(new BigDecimal("1200"))).isEqualTo("1.200 disp.");
        assertThat(PosCatalogController.stockLabel(new BigDecimal("1.500"))).isEqualTo("1,5 disp.");
    }

    @Test
    @DisplayName("PD-11: Balança oculta sem configuração e simulação só para gestão")
    void pd11_scaleAccess() throws Exception {
        onEdt(() -> {
            PosScaleLiveWidget widget = new PosScaleLiveWidget(new SerialScaleReader(true));
            widget.applyAccess(false, "ADMIN");
            assertThat(widget.isVisible()).isFalse();
            widget.applyAccess(true, "CAIXA");
            assertThat(widget.isVisible()).isTrue();
        });
        assertThat(PosScaleLiveWidget.canSimulate(true, "ADMIN")).isTrue();
        assertThat(PosScaleLiveWidget.canSimulate(true, "MANAGER")).isTrue();
        assertThat(PosScaleLiveWidget.canSimulate(true, "CAIXA")).isFalse();
        assertThat(PosScaleLiveWidget.canSimulate(true, null)).isFalse();
        assertThat(PosScaleLiveWidget.canSimulate(false, "ADMIN")).isFalse();
    }

    @Test
    @DisplayName("PD-12: Simulação não aparenta balança ligada")
    void pd12_simulationDoesNotLookConnected() throws Exception {
        PosScaleLiveWidget widget = new PosScaleLiveWidget(new SerialScaleReader(true));
        SwingUtilities.invokeAndWait(() -> { });
        JLabel dot = (JLabel) widget.getComponent(0);

        assertThat(dot.getToolTipText()).isEqualTo("Balança em simulação");
    }

    @Test
    @DisplayName("PD-13: Fechar Caixa mostra a tecla real e não grita a vermelho")
    void pd13_closeTillIsQuiet() throws Exception {
        onEdt(() -> {
            POSPanel pos = pos();
            assertThat(pos.closeSessionBtn.getShortcut()).isEqualTo("F12");
            assertThat(pos.closeSessionBtn.getBackground()).isEqualTo(UIHelper.ROW_ALT);
            Container actions = pos.closeSessionBtn.getParent();
            assertThat(actions.getComponent(actions.getComponentCount() - 1)).isSameAs(pos.closeSessionBtn);
        });
    }

    @Test
    @DisplayName("PD-14: Barra lateral recolhe em ecrãs pequenos e favorito não duplica")
    void pd14_sidebar() {
        assertThat(PosLayout.shouldCollapseSidebar(1366)).isTrue();
        assertThat(PosLayout.shouldCollapseSidebar(1600)).isFalse();

        SidebarFavoritesManager favorites = SidebarFavoritesManager.getInstance();
        String label = "Modulo PD-14";
        SidebarNavItem pinned = new SidebarNavItem("fas-box", label, Color.BLUE, () -> { });
        SidebarNavItem other = new SidebarNavItem("fas-box", "Outro PD-14", Color.BLUE, () -> { });
        SidebarFavoritesCoordinator coordinator = new SidebarFavoritesCoordinator(new JPanel(), () -> List.of(pinned, other));
        try {
            favorites.addFavorite(label);
            coordinator.rebuildFavorites(false);
            assertThat(pinned.isVisible()).isFalse();
            assertThat(other.isVisible()).isTrue();

            favorites.removeFavorite(label);
            coordinator.rebuildFavorites(false);
            assertThat(pinned.isVisible()).isTrue();
        } finally {
            favorites.removeFavorite(label);
        }
    }

    @Test
    @DisplayName("PD-15: Nome da sessão só substitui o login do próprio utilizador")
    void pd15_operatorLabel() {
        assertThat(PosLayout.operatorLabel("ana", "ana", "Ana Costa")).isEqualTo("Ana Costa");
        assertThat(PosLayout.operatorLabel("joao", "ana", "Ana Costa")).isEqualTo("joao");
        assertThat(PosLayout.operatorLabel("ana", "ana", null)).isEqualTo("ana");
    }

    private static void onEdt(Runnable test) throws Exception {
        Long company = CurrentUserContext.getCurrentCompanyId();
        String username = CurrentUserContext.getUsername();
        String role = CurrentUserContext.getRole();
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser(username, role);
            CurrentUserContext.setCurrentCompanyId(company);
            try {
                test.run();
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    private static void layOut(Container container, int width, int height) {
        container.setSize(width, height);
        layOut(container);
        layOut(container);
    }

    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layOut(nested);
        }
    }

    private static List<Component> all(Container container) {
        List<Component> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            found.add(child);
            if (child instanceof Container nested) found.addAll(all(nested));
        }
        return found;
    }

    private static TillSessionDTO session() {
        return new TillSessionDTO(10L, "ana", 1L, BigDecimal.ZERO, null, null, null, LocalDateTime.now(), null, "OPEN");
    }

    private static ClientDTO client(String name, String taxId) {
        ClientDTO client = Mockito.mock(ClientDTO.class);
        Mockito.when(client.name()).thenReturn(name);
        Mockito.when(client.taxId()).thenReturn(taxId);
        return client;
    }

    private static ProductDTO product(long id) {
        ProductDTO product = Mockito.mock(ProductDTO.class);
        Mockito.when(product.id()).thenReturn(id);
        Mockito.when(product.name()).thenReturn("Massa Esparguete 500g");
        Mockito.when(product.unitPrice()).thenReturn(new BigDecimal("95.00"));
        Mockito.when(product.taxRate()).thenReturn(BigDecimal.ZERO);
        Mockito.when(product.stockTracked()).thenReturn(true);
        return product;
    }

    private static PageResponse<POSCatalogItemDTO> page(int items, int totalPages) {
        List<POSCatalogItemDTO> rows = IntStream.rangeClosed(1, items).mapToObj(id -> {
            POSCatalogItemDTO row = Mockito.mock(POSCatalogItemDTO.class);
            ProductDTO product = product(id);
            Mockito.when(row.product()).thenReturn(product);
            Mockito.when(row.sellable()).thenReturn(true);
            Mockito.when(row.stockQuantity()).thenReturn(new BigDecimal("118.000"));
            return row;
        }).toList();
        PageResponse<POSCatalogItemDTO> page = Mockito.mock(PageResponse.class);
        Mockito.when(page.items()).thenReturn(rows);
        Mockito.when(page.page()).thenReturn(0);
        Mockito.when(page.totalPages()).thenReturn(totalPages);
        Mockito.when(page.totalElements()).thenReturn((long) items);
        return page;
    }

    private static POSPanel pos() {
        return new POSPanel(Mockito.mock(POSApiClient.class), Mockito.mock(ComercialApiClient.class),
                Mockito.mock(InventoryApiClient.class), Mockito.mock(FinanceApiClient.class),
                Mockito.mock(PromotionApiClient.class),
                Mockito.mock(mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser.class));
    }
}
