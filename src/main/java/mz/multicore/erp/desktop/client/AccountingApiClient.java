package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.accounting.dto.*;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Cliente HTTP da contabilidade. Só HTTP/DTO — nenhuma regra vive aqui. */
@Component
@Profile("desktop")
public class AccountingApiClient {

    private final DesktopClientFactory clientFactory;

    public AccountingApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<AccountDTO> getAccounts() {
        return clientFactory.authenticatedClient().getList("/api/accounting/accounts", AccountDTO.class);
    }

    public AccountDTO createAccount(SaveAccountRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/accounting/accounts", request, AccountDTO.class);
    }

    /** Semeia o PGC-NIRF; devolve quantas contas foram criadas (0 = já existia plano). */
    @SuppressWarnings("unchecked")
    public int seedChart() {
        Map<String, Integer> response = clientFactory.authenticatedClient()
                .post("/api/accounting/accounts/seed", null, Map.class);
        Object created = response == null ? null : response.get("created");
        return created instanceof Number number ? number.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    public PageResponse<JournalEntryDTO> getJournal(int page, int size) {
        return clientFactory.authenticatedClient().getGeneric(
                "/api/accounting/journal?page=" + page + "&size=" + size,
                PageResponse.class, JournalEntryDTO.class);
    }

    public JournalEntryDTO createEntry(CreateJournalEntryRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/accounting/journal", request, JournalEntryDTO.class);
    }

    public TrialBalanceDTO getTrialBalance(LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().get(
                "/api/accounting/trial-balance?from=" + from + "&to=" + to, TrialBalanceDTO.class);
    }

    public LedgerDTO getLedger(String accountCode, LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().get(
                "/api/accounting/ledger/" + accountCode + "?from=" + from + "&to=" + to, LedgerDTO.class);
    }
}
