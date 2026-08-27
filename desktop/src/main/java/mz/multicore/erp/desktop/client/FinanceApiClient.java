package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.financeira.dto.PayInvoiceRequest;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP para a tesouraria ({@code /api/finance}). Espelha o padrão do
 * {@link ComercialApiClient}: métodos tipados sobre o {@link DesktopClientFactory}.
 */
@Component
@Profile("desktop")
public class FinanceApiClient {

    private final DesktopClientFactory clientFactory;

    public FinanceApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<TreasuryAccountDTO> getAllAccounts() {
        return clientFactory.authenticatedClient().getList("/api/finance/accounts", TreasuryAccountDTO.class);
    }

    public List<TreasuryTransactionDTO> getAllTransactions() {
        return clientFactory.authenticatedClient().getList("/api/finance/transactions", TreasuryTransactionDTO.class);
    }

    public void payInvoice(Long invoiceId, Long accountId) {
        clientFactory.authenticatedClient().post("/api/finance/pay-invoice", new PayInvoiceRequest(invoiceId, accountId));
    }
}
