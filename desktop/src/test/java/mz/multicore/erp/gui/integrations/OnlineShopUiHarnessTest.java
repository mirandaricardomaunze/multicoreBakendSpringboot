package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
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

    private static Path gui(String file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "integrations", file);
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
