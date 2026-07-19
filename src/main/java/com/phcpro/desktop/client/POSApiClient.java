package com.phcpro.desktop.client;

import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.pos.dto.CashMovementRequest;
import com.phcpro.modules.pos.dto.CloseSessionRequest;
import com.phcpro.modules.pos.dto.OpenSessionRequest;
import com.phcpro.modules.pos.dto.POSCheckoutRequest;
import com.phcpro.modules.pos.dto.POSReturnRequest;
import com.phcpro.modules.pos.dto.PosPaymentRequest;
import com.phcpro.modules.pos.dto.TillMovementDTO;
import com.phcpro.modules.pos.dto.TillSessionDTO;
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
        return clientFactory.authenticatedClient().post("/api/pos/sessions/" + sessionId + "/close",
                new CloseSessionRequest(closingBalanceReal, depositAccountId), TillSessionDTO.class);
    }

    public TillMovementDTO addCashMovement(Long sessionId, String type, BigDecimal amount, String description) {
        return clientFactory.authenticatedClient().post("/api/pos/sessions/" + sessionId + "/movements",
                new CashMovementRequest(type, amount, description), TillMovementDTO.class);
    }

    /** Finaliza a venda e devolve o documento emitido (id, número e total para recibo/mensagem). */
    public InvoiceDTO checkout(POSCheckoutRequest request) {
        return clientFactory.authenticatedClient().post("/api/pos/checkout", request, InvoiceDTO.class);
    }

    public CreditNoteDTO returnSale(POSReturnRequest request) {
        return clientFactory.authenticatedClient().post("/api/pos/returns", request, CreditNoteDTO.class);
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

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
