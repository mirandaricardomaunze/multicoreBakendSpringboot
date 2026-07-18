package com.phcpro.desktop.client;

import com.phcpro.modules.crm.dto.CreateWorkSheetRequest;
import com.phcpro.modules.crm.dto.SupportTicketDTO;
import com.phcpro.modules.crm.dto.WorkSheetDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP para o CRM / assistência ({@code /api/crm}). Espelha o padrão do
 * {@link ComercialApiClient}: métodos tipados sobre o {@link DesktopClientFactory}.
 */
@Component
@Profile("desktop")
public class CRMApiClient {

    private final DesktopClientFactory clientFactory;

    public CRMApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<SupportTicketDTO> getAllTickets() {
        return clientFactory.authenticatedClient().getList("/api/crm/tickets", SupportTicketDTO.class);
    }

    public List<WorkSheetDTO> getAllWorkSheets() {
        return clientFactory.authenticatedClient().getList("/api/crm/worksheets", WorkSheetDTO.class);
    }

    public WorkSheetDTO createWorkSheet(CreateWorkSheetRequest request) {
        return clientFactory.authenticatedClient().post("/api/crm/worksheets", request, WorkSheetDTO.class);
    }

    public void billWorkSheet(Long id) {
        clientFactory.authenticatedClient().post("/api/crm/worksheets/" + id + "/bill", null);
    }
}
