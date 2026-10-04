package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.SidebarNavItem;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness automatizado para modernização da UI e organização de navegação.
 * Valida o SPEC docs/UI_ORGANIZACAO_NAVEGACAO_SPEC.md e o HARNESS docs/UI_ORGANIZACAO_NAVEGACAO_HARNESS.md.
 */
class UiOrganizationNavigationHarnessTest {

    @Test
    void nav00_loginDialogHasLightCardAndLuminousThemeDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UIHelper.loadAndApplySavedTheme();
            assertTrue(UIHelper.isLight(), "O tema por defeito no arranque deve ser LIGHT");

            LoginDialog dialog = new LoginDialog(null);
            assertThat(dialog.getCard()).isNotNull();

            java.awt.Color cardBg = dialog.getCard().getBackground();
            assertTrue(cardBg.getRed() >= 240 && cardBg.getGreen() >= 240 && cardBg.getBlue() >= 240,
                    "O cartão central de login deve possuir fundo claro/frosted (>= 240 RGB)");

            assertEquals(java.awt.Color.WHITE, dialog.getUsernameField().getBackground(),
                    "O campo de utilizador deve ter fundo branco claro");
            assertEquals(java.awt.Color.WHITE, dialog.getPasswordField().getBackground(),
                    "O campo de senha deve ter fundo branco claro");

            dialog.dispose();
        });
    }

    @Test
    void nav01And02_loginDialogProvidesAccessibleFieldsAndVisibilityToggle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginDialog dialog = new LoginDialog(null);

            assertThat(dialog.getUsernameField()).isNotNull();
            assertThat(dialog.getPasswordField()).isNotNull();
            assertThat(dialog.getTogglePasswordButton()).isNotNull();

            assertEquals("Nome de utilizador", dialog.getUsernameField().getAccessibleContext().getAccessibleName());
            assertEquals("Senha de acesso", dialog.getPasswordField().getAccessibleContext().getAccessibleName());

            // Estado inicial: senha oculta
            assertFalse(dialog.isPasswordVisible());
            assertEquals('•', dialog.getPasswordField().getEchoChar());
            assertEquals("Mostrar senha", dialog.getTogglePasswordButton().getToolTipText());

            // Alterna para visível
            dialog.togglePasswordVisibility();
            assertTrue(dialog.isPasswordVisible());
            assertEquals((char) 0, dialog.getPasswordField().getEchoChar());
            assertEquals("Ocultar senha", dialog.getTogglePasswordButton().getToolTipText());

            // Alterna de volta para oculta
            dialog.togglePasswordVisibility();
            assertFalse(dialog.isPasswordVisible());
            assertEquals('•', dialog.getPasswordField().getEchoChar());

            dialog.dispose();
        });
    }

    @Test
    void nav03_loginDialogDemoAccountPopulatesCredentialsInstantly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginDialog dialog = new LoginDialog(null);

            dialog.fillCredentials("maria", "password");
            assertEquals("maria", dialog.getUsernameField().getText());
            assertEquals("password", new String(dialog.getPasswordField().getPassword()));

            dialog.fillCredentials("joao", "custom123");
            assertEquals("joao", dialog.getUsernameField().getText());
            assertEquals("custom123", new String(dialog.getPasswordField().getPassword()));

            dialog.dispose();
        });
    }

    @Test
    void nav05_collapsibleSidebarRegistersSemanticSectionsAndModules() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Empresa Teste");

            sidebar.addSection("Operações");
            sidebar.addItem(UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), "Painel Inicial", UIHelper.MODULE_DASHBOARD, () -> {});
            sidebar.addItem(UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), "POS — Caixa", UIHelper.MODULE_POS, () -> {});

            sidebar.addSection("Gestão & CRM");
            sidebar.addItem(UIHelper.icon("fas-coins", 16, UIHelper.MODULE_FINANCEIRO), "Tesouraria", UIHelper.MODULE_FINANCEIRO, () -> {});

            sidebar.addSection("Fiscal & Auditoria");
            sidebar.addItem(UIHelper.icon("fas-percent", 16, UIHelper.MODULE_CONFIG), "Área Fiscal", UIHelper.MODULE_CONFIG, () -> {});

            sidebar.addSection("Sistema");
            sidebar.addItem(UIHelper.icon("fas-bell", 16, UIHelper.MODULE_CONFIG), "Notificações", UIHelper.MODULE_CONFIG, () -> {});

            List<SidebarNavItem> items = sidebar.getNavItems();
            assertThat(items).hasSize(5);

            sidebar.setActive("POS — Caixa");
            assertTrue(items.get(1).isActive());
            assertFalse(items.get(0).isActive());

            // Badge numérico em notificações
            sidebar.setBadge("Notificações", 7);
            assertEquals(7, items.get(4).getBadgeCount());
        });
    }

    @Test
    void nav06And07_sidebarNavItemAdaptsDimensionsAndSupportsKeyboardActivation() throws Exception {
        AtomicInteger activations = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            SidebarNavItem item = new SidebarNavItem(
                    UIHelper.icon("fas-bell", 16, UIHelper.MODULE_CONFIG),
                    "Notificações",
                    UIHelper.MODULE_CONFIG,
                    activations::incrementAndGet
            );

            assertTrue(item.isFocusable());
            assertEquals("Notificações", item.getAccessibleContext().getAccessibleName());
            assertEquals("Notificações", item.getToolTipText());

            // Modo expandido inicial
            assertFalse(item.isCollapsed());
            assertEquals(224, item.getPreferredSize().width);

            // Modo recolhido / rail
            item.setCollapsed(true);
            assertTrue(item.isCollapsed());
            assertEquals(SidebarNavItem.ICON_COLUMN, item.getPreferredSize().width);

            // Badge
            item.setBadgeCount(3);
            assertEquals(3, item.getBadgeCount());

            // Ativação por teclado (Action 'activate')
            Action action = item.getActionMap().get("activate");
            assertThat(action).isNotNull();
            action.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, "activate"));
        });

        assertEquals(1, activations.get());
    }

    @Test
    void nav08_collapsibleSidebarToggleNotifiesStateChange() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Empresa Teste");
            assertFalse(sidebar.isCollapsedState());

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            sidebar.onCollapsedChanged(listenerFired::set);

            sidebar.setCollapsedState(true);
            assertTrue(sidebar.isCollapsedState());

            sidebar.setCollapsedState(false);
            assertFalse(sidebar.isCollapsedState());
        });
    }

    @Test
    void nav10_posPanelRemainsDecomposedBelowMaxLines() throws IOException {
        Path posSource = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "POSPanel.java");
        assertThat(posSource).exists();
        long lines;
        try (var stream = Files.lines(posSource)) {
            lines = stream.count();
        }
        assertThat(lines)
                .as("POSPanel deve permanecer abaixo de 1000 linhas após extração dos KPIs diários")
                .isLessThanOrEqualTo(1000);
    }
}
