package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.CancelReasonRequest;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.CreateReceiptRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.ReceiptDTO;
import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardResultDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP especializado para faturas, prestações, recibos, devoluções e radar de cobranças.
 */
@Component
@Profile("desktop")
public class CommercialInvoiceApiClient {

    private final DesktopClientFactory clientFactory;

    public CommercialInvoiceApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<InvoiceDTO> getAllInvoices() {
        return clientFactory.authenticatedClient().getList("/api/comercial/invoices", InvoiceDTO.class);
    }

    @SuppressWarnings("unchecked")
    public PageResponse<InvoiceDTO> getInvoicePage(Long companyId, int page, int size) {
        return clientFactory.authenticatedClient().getGeneric(
                "/api/comercial/invoices/page?companyId=" + companyId + "&page=" + page + "&size=" + size,
                PageResponse.class, InvoiceDTO.class);
    }

    @SuppressWarnings("unchecked")
    public PageResponse<InvoiceDTO> getPOSSalesPage(Long companyId, int page, int size, LocalDate from, LocalDate to) {
        String path = "/api/comercial/pos-sales/page?companyId=" + companyId + "&page=" + page + "&size=" + size;
        if (from != null) path += "&from=" + from;
        if (to != null) path += "&to=" + to;
        return clientFactory.authenticatedClient().getGeneric(path, PageResponse.class, InvoiceDTO.class);
    }

    public POSSalesSummaryDTO getPOSSalesSummary(Long companyId, LocalDate from, LocalDate to) {
        String path = "/api/comercial/pos-sales/summary?companyId=" + companyId;
        if (from != null) path += "&from=" + from;
        if (to != null) path += "&to=" + to;
        return clientFactory.authenticatedClient().get(path, POSSalesSummaryDTO.class);
    }

    public List<InvoiceDTO> getPOSSalesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/pos-sales?companyId=" + companyId, InvoiceDTO.class);
    }

    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        return clientFactory.authenticatedClient().post("/api/comercial/invoices", request, InvoiceDTO.class);
    }

    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices?companyId=" + companyId, InvoiceDTO.class);
    }

    public List<InvoiceDTO> searchInvoices(String query) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices/search?query=" + enc(query), InvoiceDTO.class);
    }

    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices/outstanding?companyId=" + companyId, InvoiceDTO.class);
    }

    public void cancelInvoice(Long invoiceId, String reason) {
        clientFactory.authenticatedClient()
                .post("/api/comercial/invoices/" + invoiceId + "/cancel", new CancelReasonRequest(reason));
    }

    public List<ReceiptDTO> getReceiptsByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/receipts?companyId=" + companyId, ReceiptDTO.class);
    }

    public ReceiptDTO createReceipt(Long invoiceId, Long treasuryAccountId, String paymentMethod, BigDecimal amountPaid) {
        return clientFactory.authenticatedClient().post("/api/comercial/receipts",
                new CreateReceiptRequest(invoiceId, treasuryAccountId, paymentMethod, amountPaid), ReceiptDTO.class);
    }

    public void cancelReceipt(Long receiptId, String reason) {
        clientFactory.authenticatedClient()
                .post("/api/comercial/receipts/" + receiptId + "/cancel", new CancelReasonRequest(reason));
    }

    public byte[] renderInvoice(Long invoiceId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/invoice/" + invoiceId);
    }

    public byte[] renderInvoiceThermal(Long invoiceId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/invoice/" + invoiceId + "/thermal");
    }

    public byte[] renderOrder(Long orderId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/order/" + orderId);
    }

    public byte[] renderGuide(Long invoiceId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/guide/" + invoiceId);
    }

    public List<BusinessUnitDTO> getActiveBusinessUnits(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/business-units/active?companyId=" + companyId, BusinessUnitDTO.class);
    }

    public List<InvoiceInstallmentDTO> createInvoiceInstallmentPlan(Long invoiceId, CreateInstallmentPlanRequest request) {
        return clientFactory.authenticatedClient()
                .postForList("/api/comercial/invoices/" + invoiceId + "/installments", request, InvoiceInstallmentDTO.class);
    }

    public List<InvoiceInstallmentDTO> getInvoiceInstallments(Long invoiceId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices/" + invoiceId + "/installments", InvoiceInstallmentDTO.class);
    }

    /** Recebe o valor por recibo (RC) na conta indicada; o plano de prestações acompanha. */
    public List<InvoiceInstallmentDTO> payInvoiceInstallment(Long invoiceId, BigDecimal amount, Long financeAccountId) {
        return clientFactory.authenticatedClient().postForList("/api/comercial/invoices/" + invoiceId
                + "/installments/pay?amount=" + amount + "&financeAccountId=" + financeAccountId, null, InvoiceInstallmentDTO.class);
    }

    public List<mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO> treasuryAccounts() {
        return clientFactory.authenticatedClient().getList("/api/finance/accounts",
                mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO.class);
    }

    public CollectionRadarSummaryDTO getCollectionRadarSummary(Long companyId) {
        String path = "/api/comercial/radar/summary" + (companyId != null ? "?companyId=" + companyId : "");
        return clientFactory.authenticatedClient().get(path, CollectionRadarSummaryDTO.class);
    }

    public List<CollectionRadarItemDTO> getCollectionRadarItems(Long companyId, String bucket) {
        StringBuilder path = new StringBuilder("/api/comercial/radar/items?");
        if (companyId != null) path.append("companyId=").append(companyId).append("&");
        if (bucket != null && !bucket.isBlank()) path.append("bucket=").append(enc(bucket));
        return clientFactory.authenticatedClient().getList(path.toString(), CollectionRadarItemDTO.class);
    }

    public CollectionReminderTemplateDTO getCollectionReminderTemplate(Long invoiceId, String channel) {
        String path = "/api/comercial/radar/invoices/" + invoiceId + "/reminder?channel=" + enc(channel);
        return clientFactory.authenticatedClient().get(path, CollectionReminderTemplateDTO.class);
    }

    public List<ReturnableInvoiceLineDTO> getReturnableInvoiceLines(Long invoiceId) {
        String path = "/api/comercial/invoices/" + invoiceId + "/returnable-lines";
        return clientFactory.authenticatedClient().getList(path, ReturnableInvoiceLineDTO.class);
    }

    public SalesReturnWizardResultDTO processSalesReturn(Long invoiceId, SalesReturnWizardRequest request) {
        String path = "/api/comercial/invoices/" + invoiceId + "/sales-return";
        return clientFactory.authenticatedClient().post(path, request, SalesReturnWizardResultDTO.class);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
