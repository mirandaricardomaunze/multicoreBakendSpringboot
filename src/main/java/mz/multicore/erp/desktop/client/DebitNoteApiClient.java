package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.CreateDebitNoteRequest;
import mz.multicore.erp.modules.comercial.dto.DebitNoteDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cliente HTTP para notas de débito ({@code /api/debit-notes}) + impressão. */
@Component
@Profile("desktop")
public class DebitNoteApiClient {

    private final DesktopClientFactory clientFactory;

    public DebitNoteApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<DebitNoteDTO> findByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/debit-notes?companyId=" + companyId, DebitNoteDTO.class);
    }

    public DebitNoteDTO create(CreateDebitNoteRequest request) {
        return clientFactory.authenticatedClient().post("/api/debit-notes", request, DebitNoteDTO.class);
    }

    public DebitNoteDTO approve(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/debit-notes/" + id + "/approve", null, DebitNoteDTO.class);
    }

    public DebitNoteDTO reject(Long id, String reason) {
        return clientFactory.authenticatedClient()
                .post("/api/debit-notes/" + id + "/reject", new RejectRequest(reason), DebitNoteDTO.class);
    }

    /** Nota de débito em PDF ({@code /api/print/debit-note/{id}}). */
    public byte[] renderDebitNote(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/debit-note/" + id);
    }

    record RejectRequest(String rejectionReason) {}
}
