package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.CancelReasonRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.ExtendQuotationValidityRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.UpdateQuotationRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP especializado para cotações e propostas comerciais.
 */
@Component
@Profile("desktop")
public class CommercialQuotationApiClient {

    private final DesktopClientFactory clientFactory;

    public CommercialQuotationApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<QuotationDTO> getQuotationsByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/quotations?companyId=" + companyId, QuotationDTO.class);
    }

    public QuotationDTO getQuotationById(Long id) {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/quotations/" + id, QuotationDTO.class);
    }

    public QuotationDTO createQuotation(CreateQuotationRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/quotations", request, QuotationDTO.class);
    }

    public QuotationDTO updateQuotation(Long id, UpdateQuotationRequest request) {
        return clientFactory.authenticatedClient()
                .put("/api/comercial/quotations/" + id, request, QuotationDTO.class);
    }

    public QuotationDTO sendQuotation(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/quotations/" + id + "/send", null, QuotationDTO.class);
    }

    public QuotationDTO acceptQuotation(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/quotations/" + id + "/accept", null, QuotationDTO.class);
    }

    public QuotationDTO rejectQuotation(Long id, String reason) {
        return clientFactory.authenticatedClient().post("/api/comercial/quotations/" + id + "/reject",
                new CancelReasonRequest(reason), QuotationDTO.class);
    }

    public QuotationDTO cancelQuotation(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/quotations/" + id + "/cancel", null, QuotationDTO.class);
    }

    public QuotationDTO extendQuotationValidity(Long id, LocalDate validUntil) {
        return clientFactory.authenticatedClient().post("/api/comercial/quotations/" + id + "/extend",
                new ExtendQuotationValidityRequest(validUntil), QuotationDTO.class);
    }

    public OrderDTO convertQuotation(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/quotations/" + id + "/convert", null, OrderDTO.class);
    }

    public InvoiceDTO convertQuotationToInvoice(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/quotations/" + id + "/convert-to-invoice", null, InvoiceDTO.class);
    }

    public List<QuotationDTO> getOpenQuotationsByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/quotations/open?companyId=" + companyId, QuotationDTO.class);
    }

    public byte[] renderQuotation(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/quotation/" + id);
    }

    public byte[] renderQuotationThermal(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/quotation/" + id + "/thermal");
    }
}
