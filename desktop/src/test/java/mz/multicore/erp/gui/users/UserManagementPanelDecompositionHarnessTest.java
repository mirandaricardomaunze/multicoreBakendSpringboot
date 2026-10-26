package mz.multicore.erp.gui.users;

import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.modules.users.dto.AppUserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("HARNESS-USER-MANAGEMENT-PANEL-DECOMPOSITION-001: Decomposição do Painel de Gestão de Utilizadores")
class UserManagementPanelDecompositionHarnessTest {

    @Test
    @DisplayName("UMP-01: UserManagementPanel.java reduzido para <= 150 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/users/UserManagementPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/users/UserManagementPanel.java");
        }
        assertTrue(Files.exists(path), "UserManagementPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 150, "UserManagementPanel.java deve possuir <= 150 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("UMP-02: UserOperationsCoordinator.java desacoplado com <= 130 linhas")
    void testCoordinatorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/users/UserOperationsCoordinator.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/users/UserOperationsCoordinator.java");
        }
        assertTrue(Files.exists(path), "UserOperationsCoordinator.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 130, "UserOperationsCoordinator.java deve possuir <= 130 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("UMP-03: Erradicação absoluta de JOptionPane em UserManagementPanel e UserOperationsCoordinator")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "UserManagementPanel.java",
                "UserOperationsCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/users");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/users");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> content = Files.readAllLines(filePath);
            for (int i = 0; i < content.size(); i++) {
                assertFalse(content.get(i).contains("JOptionPane"),
                        file + " contém JOptionPane na linha " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("UMP-04: Construtor público preservado e instanciação headless sem falhas")
    void testConstructorAndHeadlessInstantiation() {
        UserApiClient userApiClient = Mockito.mock(UserApiClient.class);
        when(userApiClient.getAllUsers()).thenReturn(List.of(
                new AppUserDTO(1L, "admin", "Administrador Principal", "ADMIN", true, true, "admin@co.mz"),
                new AppUserDTO(2L, "vendedor1", "Carlos Vendedor", "SELLER", true, false, "carlos@co.mz")
        ));

        assertDoesNotThrow(() -> {
            UserManagementPanel panel = new UserManagementPanel(userApiClient);
            assertNotNull(panel);
            assertEquals(2, panel.getComponentCount());
        });
    }

    @Test
    @DisplayName("UMP-05: Invariantes canónicas de layout e ações preservadas")
    void testCanonicalInvariants() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/users/UserManagementPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/users/UserManagementPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop(\"Gestão de Utilizadores\""),
                "Deve conter cabeçalho canónico tableCardTop(\"Gestão de Utilizadores\"");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(usersTable)"),
                "Deve calibrar colunas com ensureHeadersFit");
        assertTrue(content.contains("TableQuickPeekController.install(usersTable"),
                "Deve instalar TableQuickPeekController");
        assertTrue(content.contains("KpiCard.createGrid(4)"),
                "Deve conter grid de 4 KPIs");
        assertTrue(content.contains("ClientTablePagination.install(usersTable)"),
                "Deve possuir paginação de tabela");
    }
}
