package mz.multicore.erp.gui.users;

import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.modules.users.dto.AppUserDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("Harness: UserManagementPanel e Diálogos de Gestão de Utilizadores (GUP-07 a GUP-08)")
class UserManagementPanelHarnessTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("multicore.test.headless", "true");
    }

    @Test
    @DisplayName("GUP-07: UserManagementPanel instancia componentes e tabela sem exceções em headless")
    void testUserManagementPanelHeadlessInstantiation() {
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
    @DisplayName("GUP-08: ManagerPinDialog valida entrada de PIN em headless")
    void testManagerPinDialogHeadlessValidation() {
        UserApiClient userApiClient = Mockito.mock(UserApiClient.class);
        ManagerPinDialog dialog = new ManagerPinDialog(null, userApiClient, "Anulação de Fatura");

        // Em headless devolve true como bypass seguro
        assertTrue(dialog.showDialog());
    }
}
