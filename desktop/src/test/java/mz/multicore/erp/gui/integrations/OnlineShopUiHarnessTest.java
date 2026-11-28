package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.NotificationFeed;
import mz.multicore.erp.gui.commercial.OnlineShopOrderInfo;
import mz.multicore.erp.gui.components.RecordDetailsDialog;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.gui.OnlineShopPublishBox;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopUnbilledOrderDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness de UI da loja online no desktop (docs/LOJA_ONLINE_SPEC.md §3.5).
 * LO-UI-01: Configurações › Integrações ganha, depois de "Webhooks", a secção "Loja online" (card, ícone pelo
 * {@code UIHelper.icon}) com "Loja activa", "Código público" e a pré-visualização {@code /loja/{código}}, uma caixa
 * por armazém de venda em "Lojas de levantamento", tabela "Zonas de entrega" (Nome, Taxa em MT) com "Adicionar zona" e
 * "Remover zona", "Telefone de contacto" e "Prazo de cancelamento (dias)" com 7 por omissão; lê e grava por
 * {@code GET}/{@code PUT /api/online-shop/settings}; outro perfil não pede nada e vê o aviso.
 * LO-UI-02: a ficha do artigo tem a caixa "Publicar na loja online", desligada por omissão e sempre desligada no
 * artigo novo até ser gravado; o estado vem do servidor e só se grava por
 * {@code PUT /api/online-shop/products/{id}/published} quando a caixa mudou.
 * LO-UI-03: a lista de encomendas marca "Loja online" na coluna "Origem" e o detalhe mostra contacto, entrega e morada;
 * o sino dos perfis de vendas tem uma linha por encomenda online por facturar ({@code GET /api/online-shop/orders/unbilled}).
 */
class OnlineShopUiHarnessTest {

    private static final List<WarehouseDTO> SALES_WAREHOUSES = List.of(
            warehouse(1L, "Loja Baixa"), warehouse(2L, "Loja Matola"));

    private static final OnlineShopSettingsDTO SETTINGS = new OnlineShopSettingsDTO(true, "mercearia-baixa",
            List.of(2L), List.of(new OnlineShopDeliveryZoneDTO("Matola", new BigDecimal("150.00"))),
            "841234567", 5);

    @AfterEach
    void clearSession() throws Exception {
        onEdt(CurrentUserContext::clear);
    }

    @Test
    @DisplayName("LO-UI-01: O separador Integrações tem a secção Loja online por baixo dos Webhooks")
    void loUi01_seccaoDepoisDosWebhooks() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            ConfigPanel config = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                    mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                    mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), null, null,
                    mock(ExchangeRateApiClient.class), mock(AccountingApiClient.class), mock(ApiKeyApiClient.class),
                    mock(WebhookApiClient.class), mock(OnlineShopApiClient.class));
            JTabbedPane tabs = firstOf(config, JTabbedPane.class);
            IntegrationsPanel integrations = (IntegrationsPanel) tabs.getComponentAt(tabs.indexOfTab("Integrações"));
            List<Component> sections = List.of(integrations.getComponents());
            WebhooksPanel webhooks = firstOf(integrations, WebhooksPanel.class);
            OnlineShopSettingsPanel shop = firstOf(integrations, OnlineShopSettingsPanel.class);
            assertThat(shop).as("secção Loja online no separador Integrações").isNotNull();
            assertThat(sections.indexOf(shop)).as("depois de Webhooks").isGreaterThan(sections.indexOf(webhooks));
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, shop.codeField())).as("num card")
                    .isNotNull();
        });
    }

    @Test
    @DisplayName("LO-UI-01: O formulário mostra as definições, uma caixa por armazém de venda e as zonas com taxa")
    void loUi01_formularioComAsDefinicoes() throws Exception {
        OnlineShopSettingsPanel panel = loaded(SETTINGS);
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            assertThat(panel.enabledBox().getText()).isEqualTo("Loja activa");
            assertThat(panel.enabledBox().isSelected()).isTrue();
            assertThat(panel.codeField().getText()).isEqualTo("mercearia-baixa");
            assertThat(panel.addressLabel().getText()).isEqualTo("Endereço: /loja/mercearia-baixa");
            assertThat(panel.pickupBoxes()).hasSize(2);
            assertThat(panel.pickupBoxes().get(1L).getText()).isEqualTo("Loja Baixa");
            assertThat(panel.pickupBoxes().get(1L).isSelected()).isFalse();
            assertThat(panel.pickupBoxes().get(2L).isSelected()).isTrue();
            JTable zones = panel.zonesTable();
            assertThat(zones.getColumnName(0)).isEqualTo("Nome");
            assertThat(zones.getColumnName(1)).isEqualTo("Taxa (MT)");
            assertThat(zones.getModel().getValueAt(0, 0)).isEqualTo("Matola");
            assertThat(zones.getModel().getValueAt(0, 1)).isEqualTo("150.00");
            assertThat(panel.addZoneButton().getText()).isEqualTo("Adicionar zona");
            assertThat(panel.removeZoneButton().getText()).isEqualTo("Remover zona");
            assertThat(panel.removeZoneButton().isEnabled()).as("sem zona seleccionada").isFalse();
            zones.setRowSelectionInterval(0, 0);
            assertThat(panel.removeZoneButton().isEnabled()).isTrue();
            assertThat(panel.phoneField().getText()).isEqualTo("841234567");
            assertThat(panel.daysSpinner().getValue()).isEqualTo(5);
            assertThat(panel.saveButton().getText()).isEqualTo("Guardar");
            assertThat(panel.saveButton().isEnabled()).isTrue();
            assertThat(panel.statusLabel().getText()).isEqualTo("Activa em /loja/mercearia-baixa");

            panel.codeField().setText("  Nova-Loja ");
            assertThat(panel.addressLabel().getText()).as("pré-visualização do endereço")
                    .isEqualTo("Endereço: /loja/nova-loja");
            panel.codeField().setText("");
            assertThat(panel.addressLabel().getText()).isEqualTo("Endereço: /loja/{código}");
        });
    }

    @Test
    @DisplayName("LO-UI-01: Sem definições gravadas a loja vem desligada e o prazo é 7 dias")
    void loUi01_semDefinicoesPrazoSete() throws Exception {
        OnlineShopSettingsPanel panel = loaded(new OnlineShopSettingsDTO(false, null, List.of(), List.of(), null, 7));
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            assertThat(panel.enabledBox().isSelected()).isFalse();
            assertThat(panel.codeField().getText()).isEmpty();
            assertThat(panel.daysSpinner().getValue()).isEqualTo(7);
            assertThat(panel.zonesTable().getModel().getRowCount()).isZero();
            assertThat(panel.statusLabel().getText()).isEqualTo("Desactivada");
        });
    }

    @Test
    @DisplayName("LO-UI-01: Guardar envia o formulário tal como está; o backend valida e responde com as suas mensagens")
    void loUi01_guardarEnviaOPedido() throws Exception {
        OnlineShopApiClient client = mock(OnlineShopApiClient.class);
        when(client.saveSettings(org.mockito.ArgumentMatchers.any())).thenReturn(SETTINGS);
        OnlineShopSettingsPanel panel = loaded(client, SETTINGS);
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            panel.pickupBoxes().get(1L).setSelected(true);
            panel.addZoneButton().doClick();
            JTable zones = panel.zonesTable();
            if (zones.isEditing()) zones.getCellEditor().cancelCellEditing();
            zones.getModel().setValueAt(" Baixa ", 1, 0);
            zones.getModel().setValueAt("1.460,50", 1, 1);
            panel.phoneField().setText(" 841234567 ");
            panel.daysSpinner().setValue(10);
            panel.saveButton().doClick();
        });
        ArgumentCaptor<SaveOnlineShopSettingsRequest> sent = ArgumentCaptor.forClass(SaveOnlineShopSettingsRequest.class);
        verify(client, timeout(5000)).saveSettings(sent.capture());
        SaveOnlineShopSettingsRequest request = sent.getValue();
        assertThat(request.enabled()).isTrue();
        assertThat(request.publicCode()).isEqualTo("mercearia-baixa");
        assertThat(request.pickupWarehouseIds()).containsExactly(1L, 2L);
        assertThat(request.deliveryZones()).extracting(OnlineShopDeliveryZoneDTO::name).containsExactly("Matola", "Baixa");
        assertThat(request.deliveryZones().get(1).fee()).isEqualByComparingTo("1460.50");
        assertThat(request.contactPhone()).isEqualTo("841234567");
        assertThat(request.autoCancelDays()).isEqualTo(10);

        assertThat(OnlineShopSettingsPanel.parseFee("150")).isEqualByComparingTo("150");
        assertThat(OnlineShopSettingsPanel.parseFee("150,5 MT")).isEqualByComparingTo("150.5");
        assertThat(OnlineShopSettingsPanel.parseFee("abc")).as("o backend recusa a zona sem taxa").isNull();
        assertThat(OnlineShopSettingsPanel.parseFee("")).isNull();
    }

    @Test
    @DisplayName("LO-UI-01: Outro perfil não pede as definições, vê o aviso e não grava")
    void loUi01_soAdmin() throws Exception {
        OnlineShopApiClient client = mock(OnlineShopApiClient.class);
        AtomicReference<OnlineShopSettingsPanel> ref = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            ref.set(new OnlineShopSettingsPanel(client));
            ref.get().refresh();
        });
        onEdt(() -> {
            OnlineShopSettingsPanel panel = ref.get();
            assertThat(panel.statusLabel().getText()).isEqualTo("Só o administrador gere a loja online.");
            assertThat(panel.saveButton().isEnabled()).isFalse();
            assertThat(panel.enabledBox().isEnabled()).isFalse();
            assertThat(panel.addZoneButton().isEnabled()).isFalse();
            assertThat(panel.pickupBoxes()).isEmpty();
        });
        verify(client, never()).settings();
        verify(client, never()).salesWarehouses();
    }

    @Test
    @DisplayName("LO-UI-01: O ADMIN carrega definições e armazéns de venda pelo cliente interno")
    void loUi01_adminCarregaPeloCliente() throws Exception {
        OnlineShopApiClient client = mock(OnlineShopApiClient.class);
        when(client.settings()).thenReturn(SETTINGS);
        when(client.salesWarehouses()).thenReturn(SALES_WAREHOUSES);
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            new OnlineShopSettingsPanel(client).refresh();
        });
        verify(client, timeout(5000)).settings();
        verify(client, timeout(5000)).salesWarehouses();
    }

    @Test
    @DisplayName("LO-UI-01: Ícones pelo UIHelper.icon sem emojis, card ModernPanel e endpoints internos no cliente")
    void loUi01_iconesECliente() throws Exception {
        String panel = Files.readString(gui("OnlineShopSettingsPanel.java"));
        assertThat(panel).contains("UIHelper.icon(\"fas-store\"").contains("UIHelper.icon(\"fas-save\"")
                .contains("UIHelper.icon(\"fas-plus\"").contains("UIHelper.icon(\"fas-trash-alt\"")
                .contains("new ModernPanel(").contains("UIHelper.createRefreshButton(")
                .contains("SignedInUser.isAdmin()").contains("error.getMessage()")
                .doesNotContain("JOptionPane").doesNotContain("new Color(");
        assertThat(panel.codePoints().filter(cp -> cp >= 0x1F300 && cp <= 0x1FAFF).count()).as("sem emojis").isZero();
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client",
                "OnlineShopApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/online-shop/settings").contains(".put(SETTINGS");
    }

    @Test
    @DisplayName("LO-UI-02: No artigo novo a caixa Publicar na loja online vem desligada até o artigo ser gravado")
    void loUi02_artigoNovoDesligado() throws Exception {
        onEdt(() -> {
            OnlineShopPublishBox publish = OnlineShopPublishBox.forNewProduct();
            assertThat(publish.checkBox().getText()).isEqualTo("Publicar na loja online");
            assertThat(publish.checkBox().isSelected()).isFalse();
            assertThat(publish.checkBox().isEnabled()).isFalse();
            assertThat(publish.checkBox().getToolTipText()).isEqualTo("Grave o artigo antes de o publicar na loja online.");
            publish.checkBox().setSelected(true);
            publish.pendingSave(5L).run();
        });
    }

    @Test
    @DisplayName("LO-UI-02: O estado vem do servidor e só se grava quando a caixa muda")
    void loUi02_lidoDoServidorGravadoSoQuandoMuda() throws Exception {
        OnlineShopApiClient client = mock(OnlineShopApiClient.class);
        when(client.isPublished(7L)).thenReturn(true);
        AtomicReference<OnlineShopPublishBox> ref = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            ref.set(OnlineShopPublishBox.forProduct(client));
            ref.get().load(7L);
            assertThat(ref.get().checkBox().isEnabled()).as("à espera do servidor").isFalse();
        });
        verify(client, timeout(5000)).isPublished(7L);
        for (int i = 0; i < 100 && !Boolean.FALSE.equals(ref.get().checkBox().getClientProperty("loading")); i++) {
            Thread.sleep(50);
        }
        onEdt(() -> { });
        onEdt(() -> {
            OnlineShopPublishBox publish = ref.get();
            assertThat(publish.checkBox().isSelected()).as("estado do servidor").isTrue();
            assertThat(publish.checkBox().isEnabled()).isTrue();
            publish.pendingSave(7L).run();
            verify(client, never()).setPublished(7L, false);
            verify(client, never()).setPublished(7L, true);

            publish.checkBox().setSelected(false);
            publish.pendingSave(7L).run();
            verify(client).setPublished(7L, false);

            publish.load(8L);
            publish.apply(7L, true);
            assertThat(publish.checkBox().isEnabled()).as("resposta de outro artigo ignorada").isFalse();
            publish.apply(8L, false);
            assertThat(publish.checkBox().isSelected()).isFalse();
            publish.checkBox().setSelected(true);
            publish.pendingSave(8L).run();
            verify(client).setPublished(8L, true);
        });
    }

    @Test
    @DisplayName("LO-UI-02: A ficha do artigo usa a caixa no registo e na edição e grava depois do artigo")
    void loUi02_fichaDoArtigoUsaACaixa() throws Exception {
        String form = Files.readString(gui("..", "StockProductFormDialog.java"));
        assertThat(form).contains("OnlineShopPublishBox.forNewProduct()")
                .contains("OnlineShopPublishBox.forProduct(owner.onlineShopApiClient)")
                .contains("publishBox.load(p.id())").contains("publishBox.pendingSave(selected.id())")
                .contains("\"Loja online:\", publishBox.checkBox()");
        assertThat(form.indexOf("publishIfChanged.run()")).as("depois de gravar o artigo")
                .isGreaterThan(form.indexOf("owner.comercialApiClient.updateProduct("));
        assertThat(Files.readString(gui("..", "MainFrame.java"))).contains("inventoryPhysicalCountingApiClient, onlineShopApiClient)");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client",
                "OnlineShopApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/online-shop/products/").contains("\"/published\"");
    }

    @Test
    @DisplayName("LO-UI-03: A coluna Origem marca Loja online e o detalhe mostra contacto, entrega e morada")
    void loUi03_marcaEDetalhe() {
        OrderDTO online = order("LOJA_ONLINE", "Loja online", "841234567", "Entrega — Matola", "Rua da Paz, 12", null);
        assertThat(OnlineShopOrderInfo.originColumn(online)).isEqualTo("Loja online");
        assertThat(OnlineShopOrderInfo.details(online))
                .extracting(RecordDetailsDialog.PropertyItem::label, RecordDetailsDialog.PropertyItem::value)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Contacto", "841234567"),
                        org.assertj.core.groups.Tuple.tuple("Entrega", "Entrega — Matola"),
                        org.assertj.core.groups.Tuple.tuple("Morada", "Rua da Paz, 12"));

        OrderDTO pickup = order("LOJA_ONLINE", "Loja online", "841234567", "Levantamento — Loja Baixa", null, null);
        assertThat(OnlineShopOrderInfo.details(pickup)).extracting(RecordDetailsDialog.PropertyItem::label)
                .as("sem morada no levantamento").containsExactly("Contacto", "Entrega");

        OrderDTO desktop = order("DESKTOP", "Desktop", null, null, null, null);
        assertThat(OnlineShopOrderInfo.originColumn(desktop)).as("sem marca nas do desktop").isEqualTo("Directa");
        assertThat(OnlineShopOrderInfo.originColumn(order("DESKTOP", "Desktop", null, null, null, "COT-7")))
                .isEqualTo("Cotação COT-7");
        assertThat(OnlineShopOrderInfo.details(desktop)).isEmpty();
    }

    @Test
    @DisplayName("LO-UI-03: A lista e o detalhe das encomendas usam a marca e as linhas da loja online")
    void loUi03_listaEDetalheUsamAInfo() throws Exception {
        assertThat(Files.readString(gui("..", "CommercialOrderOperations.java")))
                .contains("OnlineShopOrderInfo.originColumn(order)");
        assertThat(Files.readString(gui("..", "commercial", "OrderDetailsDialog.java")))
                .contains("OnlineShopOrderInfo.details(order)");
        assertThat(Files.readString(gui("..", "MainFrame.java"))).contains(".withOnlineShopOrders(onlineShopApiClient)");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client",
                "OnlineShopApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/online-shop/orders/unbilled");
    }

    @Test
    @DisplayName("LO-UI-03: O sino dos perfis de vendas tem uma linha por encomenda online por facturar")
    void loUi03_sinoDosPerfisDeVendas() throws Exception {
        OnlineShopApiClient client = mock(OnlineShopApiClient.class);
        when(client.unbilledOrders()).thenReturn(List.of(
                new OnlineShopUnbilledOrderDTO(9L, "ENC-0009", "Ana Machava", "Entrega — Matola",
                        LocalDateTime.of(2026, 11, 28, 10, 30)),
                new OnlineShopUnbilledOrderDTO(10L, "ENC-0010", "Consumidor Final", "Levantamento — Loja Baixa", null)));
        for (String role : List.of("SELLER", "MANAGER", "ADMIN")) {
            CurrentUserContext.setCurrentUser("u", role);
            List<NotificationFeed.NotificationItem> items = feed(client).load(1L);
            assertThat(items).as(role).hasSize(2);
            NotificationFeed.NotificationItem first = items.stream()
                    .filter(i -> i.detail().startsWith("ENC-0009")).findFirst().orElseThrow();
            assertThat(first.type()).isEqualTo("Loja online");
            assertThat(first.title()).isEqualTo("Encomenda da loja online por facturar");
            assertThat(first.detail()).isEqualTo("ENC-0009 — Ana Machava · Entrega — Matola");
            assertThat(first.priority()).isEqualTo(2);
            assertThat(first.moduleCard()).as("abre o card das encomendas").isEqualTo("comercial");
        }
        verify(client, org.mockito.Mockito.times(3)).unbilledOrders();

        OnlineShopApiClient other = mock(OnlineShopApiClient.class);
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");
        assertThat(feed(other).load(1L)).as("outro perfil").isEmpty();
        verify(other, never()).unbilledOrders();

        OnlineShopApiClient failing = mock(OnlineShopApiClient.class);
        when(failing.unbilledOrders()).thenThrow(new RuntimeException("offline"));
        CurrentUserContext.setCurrentUser("u", "SELLER");
        assertThat(feed(failing).load(1L)).as("o sino não falha").isEmpty();
        CurrentUserContext.clear();
    }

    private static NotificationFeed feed(OnlineShopApiClient client) {
        return new NotificationFeed(null, null, null, null).withOnlineShopOrders(client);
    }

    private static OrderDTO order(String origin, String originLabel, String phone, String fulfilment, String address,
                                  String quotation) {
        return new OrderDTO(1L, "ENC-1", 2L, "Consumidor Final", null, "Ana Machava", BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.TEN, "PENDING_APPROVAL", null, List.of(), LocalDateTime.now(), null, 0, null,
                OrderKind.FORMAL_ORDER, OrderKind.FORMAL_ORDER.label(), "Pendente de aprovação", null, quotation, null,
                null, null, false, null, null, null, null, null, null, 0L, null, null, null, origin, originLabel, phone,
                fulfilment, address);
    }

    private static OnlineShopSettingsPanel loaded(OnlineShopSettingsDTO settings) throws Exception {
        return loaded(mock(OnlineShopApiClient.class), settings);
    }

    private static OnlineShopSettingsPanel loaded(OnlineShopApiClient client, OnlineShopSettingsDTO settings)
            throws Exception {
        AtomicReference<OnlineShopSettingsPanel> panel = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            panel.set(new OnlineShopSettingsPanel(client));
            panel.get().apply(SALES_WAREHOUSES, settings);
        });
        return panel.get();
    }

    private static WarehouseDTO warehouse(Long id, String name) {
        return new WarehouseDTO(id, name, null, null, null, 1L, null, true, null, null, true);
    }

    private static Path gui(String... file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "integrations").resolve(Path.of("", file));
        return Files.exists(nested) ? nested : Path.of("desktop").resolve(nested);
    }

    private static <T extends Component> T firstOf(Container root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = firstOf(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        if (failure.get() instanceof Error error) throw error;
        if (failure.get() instanceof Exception exception) throw exception;
    }
}
