package com.phcpro.desktop.client;

import com.phcpro.modules.comercial.dto.ClientDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("desktop")
public class ComercialApiClient {

    private final DesktopClientFactory clientFactory;

    public ComercialApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ClientDTO> getClients() {
        return clientFactory.authenticatedClient().getList("/api/comercial/clients", ClientDTO.class);
    }

    public ClientDTO createClient(String name, String taxId, String email, String address) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/clients", new SaveClientRequest(name, taxId, email, address), ClientDTO.class);
    }

    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address) {
        return clientFactory.authenticatedClient().put(
                "/api/comercial/clients/" + id, new SaveClientRequest(name, taxId, email, address), ClientDTO.class);
    }

    public void deleteClient(Long id) {
        clientFactory.authenticatedClient().delete("/api/comercial/clients/" + id);
    }

    record SaveClientRequest(String name, String taxId, String email, String address) {}
}
