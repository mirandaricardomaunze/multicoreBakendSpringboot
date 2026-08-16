package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.users.dto.AppUserDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Cliente HTTP para a gestão de utilizadores da empresa ({@code /api/users}). */
@Component
@Profile("desktop")
public class UserApiClient {

    private final DesktopClientFactory clientFactory;

    public UserApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<AppUserDTO> getAllUsers() {
        return clientFactory.authenticatedClient().getList("/api/users", AppUserDTO.class);
    }

    public AppUserDTO createUser(String username, String name, String password, String role) {
        return clientFactory.authenticatedClient().post("/api/users",
                Map.of("username", username, "name", name, "password", password, "role", role), AppUserDTO.class);
    }

    public AppUserDTO updateUserName(String username, String name) {
        return clientFactory.authenticatedClient()
                .put("/api/users/" + username + "/name", Map.of("name", name), AppUserDTO.class);
    }

    public AppUserDTO updateCompanyRole(String username, String role) {
        return clientFactory.authenticatedClient()
                .patch("/api/users/" + username + "/role", Map.of("role", role), AppUserDTO.class);
    }
}
