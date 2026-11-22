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

    private static final String COST_CENTERS = "/api/accounting/cost-centers";

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

    public IncomeStatementDTO getIncomeStatement(LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().get(
                "/api/accounting/income-statement?from=" + from + "&to=" + to,
                IncomeStatementDTO.class);
    }

    /** Acerta 3201 e 3401 ao stock físico ao custo. */
    public mz.multicore.erp.modules.accounting.dto.InventoryAdjustmentDTO adjustInventory() {
        return clientFactory.authenticatedClient().post("/api/accounting/inventory-adjustment", null,
                mz.multicore.erp.modules.accounting.dto.InventoryAdjustmentDTO.class);
    }

    /** Centros de custo da empresa activa; traz sempre o centro por omissão (SPEC §3.1). */
    public List<CostCenterDTO> listCostCenters() {
        return clientFactory.authenticatedClient().getList(COST_CENTERS, CostCenterDTO.class);
    }

    public CostCenterDTO createCostCenter(SaveCostCenterRequest request) {
        return clientFactory.authenticatedClient().post(COST_CENTERS, request, CostCenterDTO.class);
    }

    public CostCenterDTO updateCostCenter(Long id, SaveCostCenterRequest request) {
        return clientFactory.authenticatedClient().put(COST_CENTERS + "/" + id, request, CostCenterDTO.class);
    }

    public CostCenterDTO setDefaultCostCenter(Long id) {
        return clientFactory.authenticatedClient().put(COST_CENTERS + "/" + id + "/default", null, CostCenterDTO.class);
    }

    public void deleteCostCenter(Long id) {
        clientFactory.authenticatedClient().delete(COST_CENTERS + "/" + id);
    }

    public BalanceSheetDTO getBalanceSheet(LocalDate asOf) {
        return clientFactory.authenticatedClient().get(
                "/api/accounting/balance-sheet?asOf=" + asOf, BalanceSheetDTO.class);
    }
}
