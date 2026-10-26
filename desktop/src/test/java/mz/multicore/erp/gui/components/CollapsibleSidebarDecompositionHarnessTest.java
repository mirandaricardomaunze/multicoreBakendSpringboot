package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de validação física e funcional da decomposição do CollapsibleSidebar (Fase 89).
 */
class CollapsibleSidebarDecompositionHarnessTest {

    @Test
    @DisplayName("Validar limites físicos estritos de linhas de código das classes decompostas")
    void validatePhysicalLineLimits() throws IOException {
        Path basePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "components");

        long sidebarLines = Files.lines(basePath.resolve("CollapsibleSidebar.java")).count();
        long badgeLines = Files.lines(basePath.resolve("SidebarLogoBadge.java")).count();
        long toggleLines = Files.lines(basePath.resolve("SidebarToggleButton.java")).count();
        long sectionLines = Files.lines(basePath.resolve("SidebarSectionHeader.java")).count();
        long userCardLines = Files.lines(basePath.resolve("SidebarUserProfileCard.java")).count();
        long favsLines = Files.lines(basePath.resolve("SidebarFavoritesCoordinator.java")).count();

        assertThat(sidebarLines).as("CollapsibleSidebar deve ter <= 280 linhas").isLessThanOrEqualTo(280);
        assertThat(badgeLines).as("SidebarLogoBadge deve ter <= 60 linhas").isLessThanOrEqualTo(60);
        assertThat(toggleLines).as("SidebarToggleButton deve ter <= 85 linhas").isLessThanOrEqualTo(85);
        assertThat(sectionLines).as("SidebarSectionHeader deve ter <= 100 linhas").isLessThanOrEqualTo(100);
        assertThat(userCardLines).as("SidebarUserProfileCard deve ter <= 165 linhas").isLessThanOrEqualTo(165);
        assertThat(favsLines).as("SidebarFavoritesCoordinator deve ter <= 100 linhas").isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Validar ausência de JOptionPane e de emojis em todo o pacote da sidebar")
    void validateNoJOptionPaneAndNoEmojis() throws IOException {
        Path basePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "components");
        java.util.List<Path> files = java.util.List.of(
                basePath.resolve("CollapsibleSidebar.java"),
                basePath.resolve("SidebarLogoBadge.java"),
                basePath.resolve("SidebarToggleButton.java"),
                basePath.resolve("SidebarSectionHeader.java"),
                basePath.resolve("SidebarUserProfileCard.java"),
                basePath.resolve("SidebarFavoritesCoordinator.java")
        );

        for (Path file : files) {
            String content = Files.readString(file);
            assertThat(content)
                    .as("Arquivo %s não deve conter JOptionPane", file.getFileName())
                    .doesNotContain("JOptionPane");
            assertThat(content)
                    .as("Arquivo %s não deve conter emojis Unicode", file.getFileName())
                    .doesNotContain("⭐", "✅", "❌", "⚠️", "🔥", "🚀");
        }
    }

    @Test
    @DisplayName("Validar instanciação, secções, itens e recolhimento")
    void validateSidebarInstantiationAndOperations() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Empresa Teste");
            assertNotNull(sidebar);
            assertEquals(CollapsibleSidebar.EXPANDED_WIDTH, sidebar.getPreferredSize().width);
            assertFalse(sidebar.isCollapsedState());

            sidebar.setSubBrand("Sucursal Beira");
            sidebar.setUserProfile("Ricardo Miranda", "Administrador");

            sidebar.addSection("Operações");
            SidebarNavItem item1 = sidebar.addItem("fas-th-large", "Painel Inicial", Color.BLUE, () -> {});
            SidebarNavItem item2 = sidebar.addItem("fas-cash-register", "POS — Caixa", Color.GREEN, () -> {});

            assertEquals(2, sidebar.getNavItems().size());
            sidebar.setActive("POS — Caixa");
            assertTrue(item2.isActive());
            assertFalse(item1.isActive());

            sidebar.setBadge("POS — Caixa", 5);
            assertEquals(5, item2.getBadgeCount());

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            sidebar.onCollapsedChanged(listenerFired::set);

            sidebar.toggle();
            assertTrue(sidebar.isCollapsedState());
        });
    }
}
