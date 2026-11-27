package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Harness de UI das integrações (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4, §4.4.1).
 * API-UI-01: Configurações › Integrações (último separador) com as chaves de API da empresa activa: tabela com
 * identificação, permissões PT-MZ, último uso e estado; "Nova chave" com nome e pelo menos uma permissão; segredo
 * mostrado uma vez com "Copiar"; "Revogar" só de uma chave activa, com confirmação; gerir só ADMIN.
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
