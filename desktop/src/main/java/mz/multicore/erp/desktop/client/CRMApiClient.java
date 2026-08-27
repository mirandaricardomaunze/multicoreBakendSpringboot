package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.crm.dto.ChangeTicketStatusRequest;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.CrmSettingsDTO;
import mz.multicore.erp.modules.crm.dto.UpdateCrmSettingsRequest;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.UpdateTicketRequest;
import mz.multicore.erp.modules.crm.dto.UpdateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.VoidWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.WorkSheetDTO;
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

    /** Tarifa horária em vigor (é o preço do produto de mão de obra no catálogo). */
    public CrmSettingsDTO getSettings() {
        return clientFactory.authenticatedClient().get("/api/crm/settings", CrmSettingsDTO.class);
    }

    public CrmSettingsDTO updateSettings(UpdateCrmSettingsRequest request) {
        return clientFactory.authenticatedClient().put("/api/crm/settings", request, CrmSettingsDTO.class);
    }

    public List<SupportTicketDTO> getAllTickets() {
        return clientFactory.authenticatedClient().getList("/api/crm/tickets", SupportTicketDTO.class);
    }

    public SupportTicketDTO createTicket(CreateTicketRequest request) {
        return clientFactory.authenticatedClient().post("/api/crm/tickets", request, SupportTicketDTO.class);
    }

    public SupportTicketDTO updateTicket(Long id, UpdateTicketRequest request) {
        return clientFactory.authenticatedClient().put("/api/crm/tickets/" + id, request, SupportTicketDTO.class);
    }

    public SupportTicketDTO changeTicketStatus(Long id, ChangeTicketStatusRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/crm/tickets/" + id + "/status", request, SupportTicketDTO.class);
    }

    public List<WorkSheetDTO> getAllWorkSheets() {
        return clientFactory.authenticatedClient().getList("/api/crm/worksheets", WorkSheetDTO.class);
    }

    public WorkSheetDTO createWorkSheet(CreateWorkSheetRequest request) {
        return clientFactory.authenticatedClient().post("/api/crm/worksheets", request, WorkSheetDTO.class);
    }

    public WorkSheetDTO updateWorkSheet(Long id, UpdateWorkSheetRequest request) {
        return clientFactory.authenticatedClient()
                .put("/api/crm/worksheets/" + id, request, WorkSheetDTO.class);
    }

    public WorkSheetDTO voidWorkSheet(Long id, VoidWorkSheetRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/crm/worksheets/" + id + "/void", request, WorkSheetDTO.class);
    }

    public void billWorkSheet(Long id) {
        clientFactory.authenticatedClient().post("/api/crm/worksheets/" + id + "/bill", null);
    }

    /** Folha de obra em PDF ({@code /api/print/work-sheet/{id}}) — o papel que o cliente assina. */
    public byte[] workSheetPdf(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/work-sheet/" + id);
    }
}
