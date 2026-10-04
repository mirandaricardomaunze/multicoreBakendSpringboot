package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class HRContractsPanelHarnessTest {

    @Test
    @DisplayName("HRC-01: O painel de contratos encapsula tabela e filtros dentro de um ModernPanel card")
    void testContractsCardContainment() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                HRApiClient hrApi = Mockito.mock(HRApiClient.class);
                PrintApiClient printApi = Mockito.mock(PrintApiClient.class);
                HRPanel hrPanel = new HRPanel(hrApi, printApi);
                HRContractsPanel panel = new HRContractsPanel(hrPanel);

                JPanel built = panel.buildPanel();
                assertNotNull(built);
                assertEquals(BorderLayout.class, built.getLayout().getClass());

                BorderLayout layout = (BorderLayout) built.getLayout();
                Component center = layout.getLayoutComponent(BorderLayout.CENTER);
                assertTrue(center instanceof ModernPanel, "O corpo central deve ser um ModernPanel card");
                ModernPanel card = (ModernPanel) center;

                BorderLayout cardLayout = (BorderLayout) card.getLayout();
                Component cardNorth = cardLayout.getLayoutComponent(BorderLayout.NORTH);
                assertNotNull(cardNorth, "Filtros devem estar dentro do card em BorderLayout.NORTH");

                Component cardCenter = cardLayout.getLayoutComponent(BorderLayout.CENTER);
                assertTrue(cardCenter instanceof JScrollPane, "Tabela deve estar em JScrollPane dentro do card");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("HRC-02: Header de contratos usa ActionMenuButton para prevenir colisão de botões")
    void testActionMenuInHeader() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                HRApiClient hrApi = Mockito.mock(HRApiClient.class);
                PrintApiClient printApi = Mockito.mock(PrintApiClient.class);
                HRPanel hrPanel = new HRPanel(hrApi, printApi);
                HRContractsPanel panel = new HRContractsPanel(hrPanel);

                JPanel built = panel.buildPanel();
                BorderLayout layout = (BorderLayout) built.getLayout();
                Container card = (Container) layout.getLayoutComponent(BorderLayout.CENTER);
                boolean foundMenu = hasActionMenuButton(card, "Gestão do Contrato");
                assertTrue(foundMenu, "Deve possuir ActionMenuButton para gestão de contrato");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    private boolean hasActionMenuButton(Container container, String expectedText) {
        for (Component c : container.getComponents()) {
            if (c instanceof ActionMenuButton && expectedText.equals(((ActionMenuButton) c).getText())) {
                return true;
            }
            if (c instanceof Container) {
                if (hasActionMenuButton((Container) c, expectedText)) return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("HRC-03: HRContractsPanel.java cumpre estritamente o limite de 1000 linhas")
    void testLineCountUnder1000() throws IOException {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "HRContractsPanel.java");
        assertTrue(Files.exists(path), "HRContractsPanel.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 1000, "HRContractsPanel.java tem " + lines + " linhas; deve ser <= 1000");
    }
}
