package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.pos.dto.CashMovementRequest;
import mz.multicore.erp.modules.pos.dto.CloseSessionRequest;
import mz.multicore.erp.modules.pos.dto.OpenSessionRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnResultDTO;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.dto.TillMovementDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Cliente HTTP para o ponto de venda ({@code /api/pos}) + recibo/fecho-Z em PDF. */
@Component
@Profile("desktop")
public class POSApiClient {

    private final DesktopClientFactory clientFactory;

    public POSApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /** Sessão de caixa aberta do operador, ou vazio se não houver (204 do servidor). */
    public Optional<TillSessionDTO> getActiveSession(String operator, Long companyId) {
        return Optional.ofNullable(clientFactory.authenticatedClient().get(
                "/api/pos/sessions/active?operator=" + enc(operator) + "&companyId=" + companyId,
                TillSessionDTO.class));
    }

    public TillSessionDTO openSession(String operator, BigDecimal openingBalance, Long companyId) {
        return clientFactory.authenticatedClient().post("/api/pos/sessions/open",
                new OpenSessionRequest(operator, openingBalance, companyId), TillSessionDTO.class);
    }

    public TillSessionDTO closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId) {
        return closeSession(sessionId, closingBalanceReal, depositAccountId, null, null);
    }

    public TillSessionDTO closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId, String notes, String cashBreakdownJson) {
        return clientFactory.authenticatedClient().post("/api/pos/sessions/" + sessionId + "/close",
                new CloseSessionRequest(closingBalanceReal, depositAccountId, notes, cashBreakdownJson), TillSessionDTO.class);
    }

    public TillMovementDTO addCashMovement(Long sessionId, String type, BigDecimal amount, String description) {
        return clientFactory.authenticatedClient().post("/api/pos/sessions/" + sessionId + "/movements",
                new CashMovementRequest(type, amount, description), TillMovementDTO.class);
    }

    /** Finaliza a venda e devolve o documento emitido (id, número e total para recibo/mensagem). */
    public InvoiceDTO checkout(POSCheckoutRequest request) {
        return clientFactory.authenticatedClient().post("/api/pos/checkout", request, InvoiceDTO.class);
    }

    public POSReturnResultDTO returnSale(POSReturnRequest request) {
        return clientFactory.authenticatedClient().post("/api/pos/returns", request, POSReturnResultDTO.class);
    }

    /** Consulta dados e saldo de um Vale de Compras (Store Voucher). */
    public Optional<StoreVoucherDTO> getVoucher(String code, Long companyId) {
        try {
            StoreVoucherDTO voucher = clientFactory.authenticatedClient().get(
                    "/api/pos/vouchers/" + enc(code.trim().toUpperCase()) + "?companyId=" + companyId,
                    StoreVoucherDTO.class);
            return Optional.ofNullable(voucher);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /** Talão térmico do Vale de Compras em PDF ({@code /api/print/pos-voucher/{voucherId}}). */
    public byte[] renderVoucher(Long voucherId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/pos-voucher/" + voucherId);
    }

    /** Talão térmico do Vale de Compras por código em PDF ({@code /api/print/pos-voucher/code/{code}}). */
    public byte[] renderVoucherByCode(String code) {
        return clientFactory.authenticatedClient().getBytes("/api/print/pos-voucher/code/" + enc(code.trim().toUpperCase()));
    }

    /** Pagamento posterior (fiado) de uma fatura em dívida. */
    public void registerLatePayment(Long invoiceId, PosPaymentRequest request) {
        clientFactory.authenticatedClient().post("/api/pos/invoices/" + invoiceId + "/late-payment", request);
    }

    /** Recibo da venda em PDF ({@code /api/print/receipt/{invoiceId}}). */
    public byte[] renderReceipt(Long invoiceId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/receipt/" + invoiceId);
    }

    /** Folha de fecho de caixa (relatório Z) em PDF ({@code /api/print/pos-z-report/{sessionId}}). */
    public byte[] renderZReport(Long sessionId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/pos-z-report/" + sessionId);
    }

    /** Dados calculados do Relatório Z de uma sessão. */
    public mz.multicore.erp.modules.pos.dto.PosZReportDTO getZReport(Long sessionId) {
        return clientFactory.authenticatedClient().get("/api/pos/sessions/" + sessionId + "/z-report",
                mz.multicore.erp.modules.pos.dto.PosZReportDTO.class);
    }

    /** Histórico de sessões de caixa da empresa. */
    public java.util.List<mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO> getSessionsHistory(Long companyId) {
        mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO[] arr = clientFactory.authenticatedClient().get(
                "/api/pos/sessions/history?companyId=" + companyId,
                mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO[].class);
        return arr != null ? java.util.Arrays.asList(arr) : java.util.Collections.emptyList();
    }

    /** Inicia solicitação de pagamento móvel Push USSD (M-Pesa ou e-Mola). */
    public mz.multicore.erp.modules.pos.dto.MobilePaymentResponse initiateMobilePayment(
            mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest request) {
        return clientFactory.authenticatedClient().post("/api/pos/mobile-payment/initiate",
                request, mz.multicore.erp.modules.pos.dto.MobilePaymentResponse.class);
    }

    /** Consulta estado do pagamento móvel em processamento. */
    public mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse getMobilePaymentStatus(
            String transactionId, Long companyId) {
        String url = "/api/pos/mobile-payment/" + enc(transactionId) + "/status"
                + (companyId != null ? "?companyId=" + companyId : "");
        return clientFactory.authenticatedClient().get(url, mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse.class);
    }

    /** Força conclusão para modo de demonstração e testes. */
    public mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse simulateCompleteMobilePayment(
            String transactionId, Long companyId, boolean approve) {
        String url = "/api/pos/mobile-payment/" + enc(transactionId) + "/simulate-complete?approve=" + approve
                + (companyId != null ? "&companyId=" + companyId : "");
        return clientFactory.authenticatedClient().post(url, null, mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse.class);
    }

    // ── Passagem de turno ─────────────────────────────────────────────────

    /** Realiza passagem de turno entre operadores na mesma sessão. */
    public mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO performShiftHandover(
            Long sessionId, mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest request) {
        return clientFactory.authenticatedClient().post(
                "/api/pos/sessions/" + sessionId + "/shift-handover",
                request, mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO.class);
    }

    /** Lista reconciliações de passagem de turno de uma sessão. */
    public java.util.List<mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO> getShiftReconciliations(Long sessionId) {
        mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO[] arr = clientFactory.authenticatedClient().get(
                "/api/pos/sessions/" + sessionId + "/shift-reconciliations",
                mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO[].class);
        return arr != null ? java.util.Arrays.asList(arr) : java.util.Collections.emptyList();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
