package com.phcpro.desktop.client;

import com.phcpro.modules.support.dto.CreateTicketRequest;
import com.phcpro.modules.support.dto.SupportMessageDTO;
import com.phcpro.modules.support.dto.SupportTicketDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Cliente HTTP para a assistência do lado da empresa ({@code /api/support/tickets}). */
@Component
@Profile("desktop")
public class SupportApiClient {

    private final DesktopClientFactory clientFactory;

    public SupportApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<SupportTicketDTO> listCompanyTickets() {
        return clientFactory.authenticatedClient().getList("/api/support/tickets", SupportTicketDTO.class);
    }

    public SupportTicketDTO openTicket(CreateTicketRequest request) {
        return clientFactory.authenticatedClient().post("/api/support/tickets", request, SupportTicketDTO.class);
    }

    public List<SupportMessageDTO> listCompanyMessages(Long id) {
        return clientFactory.authenticatedClient()
                .getList("/api/support/tickets/" + id + "/messages", SupportMessageDTO.class);
    }

    public void addCompanyMessage(Long id, String body) {
        clientFactory.authenticatedClient().post("/api/support/tickets/" + id + "/messages", Map.of("body", body));
    }
}
