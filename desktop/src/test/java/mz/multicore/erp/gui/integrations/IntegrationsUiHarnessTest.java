package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.NotificationFeed;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import mz.multicore.erp.modules.webhooks.dto.CreateWebhookSubscriptionRequest;
import mz.multicore.erp.modules.webhooks.dto.WebhookAlertDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness de UI das integrações (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4, §4.4.1, §4.4.2).
 * API-UI-01: Configurações › Integrações (último separador) com as chaves de API da empresa activa: tabela com
 * identificação, permissões PT-MZ, último uso e estado; "Nova chave" com nome e pelo menos uma permissão; segredo
 * mostrado uma vez com "Copiar"; "Revogar" só de uma chave activa, com confirmação; gerir só ADMIN.
 * WH-UI-01: secção "Webhooks" no mesmo separador: tabela com eventos e estados PT-MZ; "Novo webhook" com URL e pelo
 * menos um evento; segredo {@code whsec_} mostrado uma vez; Activar/Desactivar pela selecção; registo de entregas com
 * filtro de estado; "Reenviar" só {@code FAILED}; aviso no sino só para o ADMIN; gerir só ADMIN.
 */
class IntegrationsUiHarnessTest {

    private static final List<ApiKeyDTO> KEYS = List.of(
            new ApiKeyDTO(1L, "Loja online", "AbCd1234", EnumSet.of(ApiKeyPermission.CATALOG_READ, ApiKeyPermission.ORDERS_WRITE),
                    LocalDateTime.of(2026, 11, 20, 9, 5), "admin", LocalDateTime.of(2026, 11, 27, 14, 30),
                    null, null, true),
            new ApiKeyDTO(2L, "Contabilista", "ZzYy9876", Set.of(ApiKeyPermission.ORDERS_READ),
                    LocalDateTime.of(2026, 11, 21, 10, 0), "admin", null,
                    LocalDateTime.of(2026, 11, 25, 16, 45), "maria", false));

    @AfterEach
    void clearSession() throws Exception {
        onEdt(CurrentUserContext::clear);
    }

    @Test
    @DisplayName("API-UI-01: Configurações ganham o separador Integrações no fim, com ícone e a secção Chaves de API")
    void apiUi01_configuracoesTemSeparadorIntegracoes() throws Exception {
        onEdt(() -> {
            ConfigPanel config = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                    mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                    mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), null, null,
                    mock(ExchangeRateApiClient.class), mock(AccountingApiClient.class), mock(ApiKeyApiClient.class));

            JTabbedPane tabs = firstOf(config, JTabbedPane.class);
            int index = tabs.indexOfTab("Integrações");
            assertThat(index).as("separador \"Integrações\" é o último").isEqualTo(tabs.getTabCount() - 1);
            assertThat(tabs.getComponentAt(index)).isInstanceOf(IntegrationsPanel.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.indexOfTab("Centros de Custo")).isEqualTo(index - 1);
            assertThat(firstOf((Container) tabs.getComponentAt(index), ApiKeysPanel.class)).isNotNull();
        });
    }

    @Test
    @DisplayName("API-UI-01: A tabela mostra nome, identificação, permissões PT-MZ, criação, último uso e estado")
    void apiUi01_tabelaDeChaves() throws Exception {
        ApiKeysPanel panel = loadedPanel("ADMIN");
        onEdt(() -> {
            JTable table = panel.keysTable();
            assertThat(table.getColumnCount()).isEqualTo(6);
            assertThat(table.getColumnName(0)).isEqualTo("Nome");
            assertThat(table.getColumnName(1)).isEqualTo("Identificação");
            assertThat(table.getColumnName(2)).isEqualTo("Permissões");
            assertThat(table.getColumnName(3)).isEqualTo("Criada em / por");
            assertThat(table.getColumnName(4)).isEqualTo("Último uso");
            assertThat(table.getColumnName(5)).isEqualTo("Estado");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).as("revogadas continuam visíveis").isEqualTo(2);
            assertThat(model.getValueAt(0, 1)).isEqualTo("mk_live_AbCd1234...");
            assertThat(model.getValueAt(0, 2)).isEqualTo("Ler catálogo, Criar encomendas");
            assertThat(model.getValueAt(0, 3)).isEqualTo("20/11/2026 09:05 por admin");
            assertThat(model.getValueAt(0, 4)).isEqualTo("27/11/2026 14:30");
            assertThat(model.getValueAt(0, 5)).isEqualTo("Activa");
            assertThat(model.getValueAt(1, 2)).isEqualTo("Ler encomendas");
            assertThat(model.getValueAt(1, 4)).isEqualTo("Nunca usada");
            assertThat(model.getValueAt(1, 5)).isEqualTo("Revogada em 25/11/2026 16:45 por maria");
            assertThat(model.isCellEditable(0, 0)).isFalse();
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("API-UI-01: Revogar só com uma chave activa seleccionada; Nova chave sempre para o ADMIN")
    void apiUi01_revogarSoChaveActiva() throws Exception {
        ApiKeysPanel panel = loadedPanel("ADMIN");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            JTable table = panel.keysTable();
            assertThat(panel.newButton().isEnabled()).isTrue();
            assertThat(panel.revokeButton().isEnabled()).as("sem selecção").isFalse();
            table.setRowSelectionInterval(0, 0);
            assertThat(panel.revokeButton().isEnabled()).as("chave activa").isTrue();
            table.setRowSelectionInterval(1, 1);
            assertThat(panel.revokeButton().isEnabled()).as("já revogada").isFalse();
            assertThat(panel.revokeConfirmation(KEYS.get(0)))
                    .isEqualTo("A chave Loja online (mk_live_AbCd1234...) deixa de funcionar imediatamente. Revogar?");
        });
    }

    @Test
    @DisplayName("API-UI-01: Só o ADMIN gere chaves: outro perfil não pede a lista e vê o aviso")
    void apiUi01_soAdminGere() throws Exception {
        ApiKeyApiClient client = mock(ApiKeyApiClient.class);
        AtomicReference<ApiKeysPanel> ref = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            ref.set(new ApiKeysPanel(client));
            ref.get().refresh();
        });
        onEdt(() -> {
            ApiKeysPanel panel = ref.get();
            assertThat(panel.newButton().isEnabled()).isFalse();
            assertThat(panel.revokeButton().isEnabled()).isFalse();
            assertThat(panel.statusLabel().getText()).isEqualTo("Só o administrador gere as chaves de API.");
            assertThat(panel.keysTable().getModel().getRowCount()).isZero();
        });
        verify(client, never()).list();
    }

    @Test
    @DisplayName("API-UI-01: Nova chave valida nome e pelo menos uma permissão com as mensagens do backend")
    void apiUi01_validacaoDaNovaChave() {
        assertThatThrownBy(() -> NewApiKeyDialog.validated("  ", Set.of(ApiKeyPermission.CATALOG_READ)))
                .hasMessage("Indique o nome da chave de API (ex.: Loja online).");
        assertThatThrownBy(() -> NewApiKeyDialog.validated("Loja online", Set.of()))
                .hasMessage("Escolha pelo menos uma permissão para a chave de API.");
        assertThatThrownBy(() -> NewApiKeyDialog.validated("x".repeat(101), Set.of(ApiKeyPermission.CATALOG_READ)))
                .hasMessage("O nome da chave de API tem no máximo 100 caracteres.");
        assertThat(NewApiKeyDialog.validated(" Loja online ", Set.of(ApiKeyPermission.ORDERS_READ)).name())
                .isEqualTo("Loja online");
        assertThat(ApiKeyLabels.permission(ApiKeyPermission.CATALOG_READ)).isEqualTo("Ler catálogo");
        assertThat(ApiKeyLabels.permission(ApiKeyPermission.ORDERS_READ)).isEqualTo("Ler encomendas");
        assertThat(ApiKeyLabels.permission(ApiKeyPermission.ORDERS_WRITE)).isEqualTo("Criar encomendas");
    }

    @Test
    @DisplayName("API-UI-01: O segredo aparece uma vez, só de leitura, com Copiar e o aviso; descartado ao fechar")
    void apiUi01_segredoMostradoUmaVez() throws Exception {
        onEdt(() -> {
            ApiKeySecretDialog dialog = new ApiKeySecretDialog("mk_live_segredo123");
            JTextField field = dialog.secretField();
            assertThat(field.getText()).isEqualTo("mk_live_segredo123");
            assertThat(field.isEditable()).isFalse();
            assertThat(dialog.warningText()).isEqualTo("Esta chave não volta a ser mostrada. Guarde-a agora.");
            assertThat(dialog.copyButton().getText()).isEqualTo("Copiar");
            dialog.discard();
            assertThat(field.getText()).as("segredo descartado ao fechar").isEmpty();
        });
    }

    @Test
    @DisplayName("API-UI-01: Ícones pelo UIHelper.icon, diálogos canónicos e endpoints internos no cliente")
    void apiUi01_iconesDialogosECliente() throws Exception {
        String panel = Files.readString(gui("ApiKeysPanel.java"));
        String newDialog = Files.readString(gui("NewApiKeyDialog.java"));
        String secretDialog = Files.readString(gui("ApiKeySecretDialog.java"));
        assertThat(panel).contains("UIHelper.icon(").contains("UIHelper.confirm(").contains("apiKeyApiClient.revoke(")
                .contains("SignedInUser.isAdmin()").doesNotContain("JOptionPane");
        assertThat(newDialog).contains("new ModernFormDialog(").contains("setOnSaveAsync(")
                .contains("apiKeyApiClient.create(").doesNotContain("JOptionPane");
        assertThat(secretDialog).contains("new ModernFormDialog(").contains("getSystemClipboard()")
                .doesNotContain("JOptionPane");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "ApiKeyApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/integrations/api-keys");
    }

    private static final List<WebhookSubscriptionDTO> WEBHOOKS = List.of(
            new WebhookSubscriptionDTO(10L, "https://loja.exemplo.co.mz/hooks",
                    EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED, WebhookEventType.INVOICE_ISSUED), true, 2,
                    LocalDateTime.of(2026, 11, 22, 8, 15), "admin", null),
            new WebhookSubscriptionDTO(11L, "https://contas.exemplo.co.mz/in",
                    EnumSet.of(WebhookEventType.CREDIT_NOTE_ISSUED, WebhookEventType.ORDER_STATUS_CHANGED), false, 20,
                    LocalDateTime.of(2026, 11, 23, 9, 0), "admin", LocalDateTime.of(2026, 11, 27, 18, 40)),
            new WebhookSubscriptionDTO(12L, "https://parado.exemplo.co.mz/x",
                    EnumSet.of(WebhookEventType.INVOICE_ISSUED), false, 0,
                    LocalDateTime.of(2026, 11, 24, 10, 0), null, null));

    private static final List<WebhookDeliveryDTO> DELIVERIES = List.of(
            new WebhookDeliveryDTO(100L, "evt_falhou", "invoice.issued", WebhookDeliveryStatus.FAILED, 6, 500, 120L,
                    "O destino respondeu 500.", null, LocalDateTime.of(2026, 11, 27, 10, 0), null),
            new WebhookDeliveryDTO(101L, "evt_entregue", "stock.availability_changed", WebhookDeliveryStatus.DELIVERED,
                    1, 200, 35L, null, null, LocalDateTime.of(2026, 11, 27, 11, 0), LocalDateTime.of(2026, 11, 27, 11, 0)),
            new WebhookDeliveryDTO(102L, "evt_espera", "order.status_changed", WebhookDeliveryStatus.PENDING, 0, null,
                    null, null, LocalDateTime.of(2026, 11, 28, 9, 1), LocalDateTime.of(2026, 11, 28, 9, 0), null));

    @Test
    @DisplayName("WH-UI-01: O separador Integrações tem a secção Webhooks por baixo das chaves de API")
    void whUi01_separadorTemSeccaoWebhooks() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            ConfigPanel config = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                    mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                    mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), null, null,
                    mock(ExchangeRateApiClient.class), mock(AccountingApiClient.class), mock(ApiKeyApiClient.class),
                    mock(WebhookApiClient.class));
            JTabbedPane tabs = firstOf(config, JTabbedPane.class);
            IntegrationsPanel integrations = (IntegrationsPanel) tabs.getComponentAt(tabs.indexOfTab("Integrações"));
            List<Component> sections = List.of(integrations.getComponents());
            ApiKeysPanel keys = firstOf(integrations, ApiKeysPanel.class);
            WebhooksPanel webhooks = firstOf(integrations, WebhooksPanel.class);
            assertThat(keys).isNotNull();
            assertThat(webhooks).as("secção Webhooks no separador Integrações").isNotNull();
            assertThat(sections.indexOf(webhooks)).as("por baixo das chaves de API").isGreaterThan(sections.indexOf(keys));
        });
    }

    @Test
    @DisplayName("WH-UI-01: A tabela mostra URL, eventos PT-MZ, falhas, criação e estado com desactivação automática")
    void whUi01_tabelaDeWebhooks() throws Exception {
        WebhooksPanel panel = loadedWebhooks(mock(WebhookApiClient.class), "ADMIN");
        onEdt(() -> {
            JTable table = panel.subscriptionsTable();
            assertThat(table.getColumnCount()).isEqualTo(5);
            assertThat(table.getColumnName(0)).isEqualTo("URL");
            assertThat(table.getColumnName(1)).isEqualTo("Eventos");
            assertThat(table.getColumnName(2)).isEqualTo("Falhas seguidas");
            assertThat(table.getColumnName(3)).isEqualTo("Criada em / por");
            assertThat(table.getColumnName(4)).isEqualTo("Estado");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).isEqualTo(3);
            assertThat(model.getValueAt(0, 0)).isEqualTo("https://loja.exemplo.co.mz/hooks");
            assertThat(model.getValueAt(0, 1)).isEqualTo("Factura emitida, Disponibilidade de stock");
            assertThat(model.getValueAt(0, 2)).isEqualTo(2);
            assertThat(model.getValueAt(0, 3)).isEqualTo("22/11/2026 08:15 por admin");
            assertThat(model.getValueAt(0, 4)).isEqualTo("Activa");
            assertThat(model.getValueAt(1, 1)).isEqualTo("Nota de crédito emitida, Estado da encomenda");
            assertThat(model.getValueAt(1, 4)).isEqualTo("Desactivada automaticamente em 27/11/2026 18:40");
            assertThat(model.getValueAt(2, 3)).isEqualTo("24/11/2026 10:00");
            assertThat(model.getValueAt(2, 4)).isEqualTo("Inactiva");
            assertThat(model.isCellEditable(0, 0)).isFalse();
            assertThat(panel.statusLabel().getText()).isEqualTo("1 webhook(s) activo(s) de 3");
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("WH-UI-01: Novo webhook valida URL e pelo menos um evento com as mensagens do backend e envia os códigos")
    void whUi01_validacaoDoNovoWebhook() {
        assertThatThrownBy(() -> NewWebhookDialog.validated("  ", Set.of(WebhookEventType.INVOICE_ISSUED)))
                .hasMessage("Indique o URL do webhook.");
        assertThatThrownBy(() -> NewWebhookDialog.validated("https://loja.exemplo.co.mz/hooks", Set.of()))
                .hasMessage("Escolha pelo menos um evento.");
        CreateWebhookSubscriptionRequest request = NewWebhookDialog.validated(" https://loja.exemplo.co.mz/hooks ",
                EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED, WebhookEventType.INVOICE_ISSUED));
        assertThat(request.url()).isEqualTo("https://loja.exemplo.co.mz/hooks");
        assertThat(request.events()).containsExactly("invoice.issued", "stock.availability_changed");
        assertThat(WebhookLabels.event(WebhookEventType.INVOICE_ISSUED)).isEqualTo("Factura emitida");
        assertThat(WebhookLabels.event(WebhookEventType.CREDIT_NOTE_ISSUED)).isEqualTo("Nota de crédito emitida");
        assertThat(WebhookLabels.event(WebhookEventType.ORDER_STATUS_CHANGED)).isEqualTo("Estado da encomenda");
        assertThat(WebhookLabels.event(WebhookEventType.STOCK_AVAILABILITY_CHANGED)).isEqualTo("Disponibilidade de stock");
    }

    @Test
    @DisplayName("WH-UI-01: O segredo whsec_ aparece uma vez, só de leitura, com Copiar e o aviso; descartado ao fechar")
    void whUi01_segredoMostradoUmaVez() throws Exception {
        onEdt(() -> {
            ApiKeySecretDialog dialog = ApiKeySecretDialog.forWebhook("whsec_segredo123");
            JTextField field = dialog.secretField();
            assertThat(field.getText()).isEqualTo("whsec_segredo123");
            assertThat(field.isEditable()).isFalse();
            assertThat(dialog.warningText()).isEqualTo("Este segredo não volta a ser mostrado. Guarde-o agora para "
                    + "validar a assinatura X-Multicore-Signature.");
            assertThat(dialog.copyButton().getText()).isEqualTo("Copiar");
            dialog.discard();
            assertThat(field.getText()).as("segredo descartado ao fechar").isEmpty();
        });
    }

    @Test
    @DisplayName("WH-UI-01: Activar/Desactivar muda de rótulo pela selecção e desactivar pede confirmação")
    void whUi01_activarDesactivarPelaSeleccao() throws Exception {
        WebhooksPanel panel = loadedWebhooks(mock(WebhookApiClient.class), "ADMIN");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            JTable table = panel.subscriptionsTable();
            assertThat(panel.newButton().isEnabled()).isTrue();
            assertThat(panel.toggleButton().isEnabled()).as("sem selecção").isFalse();
            table.setRowSelectionInterval(0, 0);
            assertThat(panel.toggleButton().isEnabled()).isTrue();
            assertThat(panel.toggleButton().getText()).isEqualTo("Desactivar");
            table.setRowSelectionInterval(1, 1);
            assertThat(panel.toggleButton().getText()).isEqualTo("Activar");
            assertThat(panel.deactivateConfirmation(WEBHOOKS.get(0)))
                    .isEqualTo("O webhook https://loja.exemplo.co.mz/hooks deixa de receber avisos. Desactivar?");
        });
    }

    @Test
    @DisplayName("WH-UI-01: O registo de entregas é da subscrição seleccionada e o filtro passa o status")
    void whUi01_registoDeEntregasComFiltro() throws Exception {
        WebhookApiClient client = mock(WebhookApiClient.class);
        when(client.deliveries(anyLong(), any())).thenReturn(DELIVERIES);
        WebhooksPanel panel = loadedWebhooks(client, "ADMIN");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            panel.subscriptionsTable().setRowSelectionInterval(0, 0);
        });
        verify(client, timeout(5000)).deliveries(10L, null);
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            panel.statusFilter().setSelectedItem("Falhada");
            assertThat(panel.selectedStatusFilter()).isEqualTo(WebhookDeliveryStatus.FAILED);
        });
        verify(client, timeout(5000)).deliveries(10L, WebhookDeliveryStatus.FAILED);
        assertThat(WebhookApiClient.deliveriesPath(10L, WebhookDeliveryStatus.FAILED))
                .isEqualTo("/api/integrations/webhooks/10/deliveries?page=0&size=50&status=FAILED");
        assertThat(WebhookApiClient.deliveriesPath(10L, null))
                .isEqualTo("/api/integrations/webhooks/10/deliveries?page=0&size=50");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            panel.applyDeliveries(DELIVERIES);
            JTable table = panel.deliveriesTable();
            assertThat(table.getColumnCount()).isEqualTo(9);
            assertThat(table.getColumnName(0)).isEqualTo("Evento");
            assertThat(table.getColumnName(3)).isEqualTo("Último código");
            assertThat(table.getColumnName(8)).isEqualTo("Entregue em");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getValueAt(0, 0)).isEqualTo("Factura emitida");
            assertThat(model.getValueAt(0, 1)).isEqualTo("Falhada");
            assertThat(model.getValueAt(0, 2)).isEqualTo(6);
            assertThat(model.getValueAt(0, 3)).isEqualTo("500");
            assertThat(model.getValueAt(0, 4)).isEqualTo("120 ms");
            assertThat(model.getValueAt(0, 5)).isEqualTo("O destino respondeu 500.");
            assertThat(model.getValueAt(1, 1)).isEqualTo("Entregue");
            assertThat(model.getValueAt(1, 8)).isEqualTo("27/11/2026 11:00");
            assertThat(model.getValueAt(2, 0)).isEqualTo("Estado da encomenda");
            assertThat(model.getValueAt(2, 1)).isEqualTo("Pendente");
            assertThat(model.getValueAt(2, 3)).as("sem código HTTP").isEqualTo("—");
            assertThat(model.getValueAt(2, 6)).isEqualTo("28/11/2026 09:01");
        });
    }

    @Test
    @DisplayName("WH-UI-01: Reenviar só para uma entrega FAILED seleccionada, com confirmação")
    void whUi01_reenviarSoFailed() throws Exception {
        WebhooksPanel panel = loadedWebhooks(mock(WebhookApiClient.class), "ADMIN");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            panel.applyDeliveries(DELIVERIES);
        });
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            JTable table = panel.deliveriesTable();
            assertThat(panel.resendButton().isEnabled()).as("sem selecção").isFalse();
            table.setRowSelectionInterval(0, 0);
            assertThat(panel.resendButton().isEnabled()).as("FAILED").isTrue();
            table.setRowSelectionInterval(1, 1);
            assertThat(panel.resendButton().isEnabled()).as("DELIVERED").isFalse();
            table.setRowSelectionInterval(2, 2);
            assertThat(panel.resendButton().isEnabled()).as("PENDING").isFalse();
            assertThat(panel.resendConfirmation(DELIVERIES.get(0)))
                    .isEqualTo("A entrega evt_falhou volta a ser enviada. Reenviar?");
        });
    }

    @Test
    @DisplayName("WH-UI-01: Só o ADMIN gere webhooks: outro perfil não pede a lista e tem os botões desactivados")
    void whUi01_soAdminGere() throws Exception {
        WebhookApiClient client = mock(WebhookApiClient.class);
        AtomicReference<WebhooksPanel> ref = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            ref.set(new WebhooksPanel(client));
            ref.get().refresh();
        });
        onEdt(() -> {
            WebhooksPanel panel = ref.get();
            assertThat(panel.newButton().isEnabled()).isFalse();
            assertThat(panel.toggleButton().isEnabled()).isFalse();
            assertThat(panel.resendButton().isEnabled()).isFalse();
            assertThat(panel.statusLabel().getText()).isEqualTo("Só o administrador gere os webhooks.");
            assertThat(panel.subscriptionsTable().getModel().getRowCount()).isZero();
        });
        verify(client, never()).list();
    }

    @Test
    @DisplayName("WH-UI-01: O sino traz o aviso de desactivação automática só para o ADMIN e leva às Configurações")
    void whUi01_sinoSoParaAdmin() {
        String message = "O webhook https://contas.exemplo.co.mz/in foi desactivado após 20 entregas falhadas seguidas.";
        WebhookApiClient client = mock(WebhookApiClient.class);
        when(client.alerts()).thenReturn(List.of(new WebhookAlertDTO(11L, "https://contas.exemplo.co.mz/in", 20,
                LocalDateTime.of(2026, 11, 27, 18, 40), message)));
        NotificationFeed feed = new NotificationFeed(null, null, null, null).withWebhookAlerts(client);

        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        assertThat(feed.load(1L)).filteredOn(item -> "Webhook".equals(item.type())).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo(message);
            assertThat(item.when()).isEqualTo("27/11/2026 18:40");
            assertThat(item.moduleCard()).isEqualTo("config");
            assertThat(item.priority()).isEqualTo(3);
        });

        when(client.alerts()).thenThrow(new IllegalStateException("sem rede"));
        assertThat(feed.load(1L)).as("um erro do pedido nunca parte o sino")
                .noneMatch(item -> "Webhook".equals(item.type()));

        WebhookApiClient other = mock(WebhookApiClient.class);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        assertThat(new NotificationFeed(null, null, null, null).withWebhookAlerts(other).load(1L))
                .noneMatch(item -> "Webhook".equals(item.type()));
        verify(other, never()).alerts();
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("WH-UI-01: Ícones pelo UIHelper.icon sem emojis, diálogos canónicos e endpoints internos no cliente")
    void whUi01_iconesDialogosECliente() throws Exception {
        String panel = Files.readString(gui("WebhooksPanel.java"));
        String newDialog = Files.readString(gui("NewWebhookDialog.java"));
        assertThat(panel).contains("UIHelper.icon(\"fas-satellite-dish\"").contains("UIHelper.icon(\"fas-plus\"")
                .contains("UIHelper.icon(\"fas-power-off\"").contains("UIHelper.icon(\"fas-redo\"")
                .contains("UIHelper.createRefreshButton(").contains("UIHelper.confirm(")
                .contains("ApiKeySecretDialog.forWebhook(").contains("SignedInUser.isAdmin()")
                .doesNotContain("JOptionPane");
        assertThat(newDialog).contains("new ModernFormDialog(").contains("setOnSaveAsync(")
                .contains("webhookApiClient.create(").doesNotContain("JOptionPane");
        for (String source : List.of(panel, newDialog, Files.readString(gui("WebhookLabels.java")))) {
            assertThat(source.codePoints().filter(cp -> cp >= 0x1F300 && cp <= 0x1FAFF).count())
                    .as("sem emojis").isZero();
        }
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "WebhookApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/integrations/webhooks").doesNotContain("X-Api-Key");
    }

    private static WebhooksPanel loadedWebhooks(WebhookApiClient client, String role) throws Exception {
        AtomicReference<WebhooksPanel> panel = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("utilizador", role);
            panel.set(new WebhooksPanel(client));
            panel.get().applySubscriptions(WEBHOOKS);
        });
        return panel.get();
    }

    private static ApiKeysPanel loadedPanel(String role) throws Exception {
        AtomicReference<ApiKeysPanel> panel = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("utilizador", role);
            panel.set(new ApiKeysPanel(mock(ApiKeyApiClient.class)));
            panel.get().applyKeys(KEYS);
        });
        return panel.get();
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
