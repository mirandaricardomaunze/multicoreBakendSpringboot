package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.ReturnedQtyDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cliente HTTP para notas de crédito ({@code /api/credit-notes}) + impressão. */
@Component
@Profile("desktop")
public class CreditNoteApiClient {

    private final DesktopClientFactory clientFactory;

    public CreditNoteApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<CreditNoteDTO> findByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/credit-notes?companyId=" + companyId, CreditNoteDTO.class);
    }

    public CreditNoteDTO create(CreateCreditNoteRequest request) {
        return clientFactory.authenticatedClient().post("/api/credit-notes", request, CreditNoteDTO.class);
    }

    public CreditNoteDTO approve(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/credit-notes/" + id + "/approve", null, CreditNoteDTO.class);
    }

    public CreditNoteDTO reject(Long id, String reason) {
        return clientFactory.authenticatedClient()
                .post("/api/credit-notes/" + id + "/reject", new RejectRequest(reason), CreditNoteDTO.class);
    }

    /** Quantidades já devolvidas por linha de fatura (linha → quantidade). */
    public Map<Long, BigDecimal> getReturnedQuantitiesByInvoiceLine(Long invoiceId) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (ReturnedQtyDTO r : clientFactory.authenticatedClient()
                .getList("/api/credit-notes/returned-quantities?invoiceId=" + invoiceId, ReturnedQtyDTO.class)) {
            result.put(r.invoiceLineId(), r.quantity());
        }
        return result;
    }

    /** Nota de crédito em PDF ({@code /api/print/credit-note/{id}}). */
    public byte[] renderCreditNote(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/credit-note/" + id);
    }

    record RejectRequest(String rejectionReason) {}
}
