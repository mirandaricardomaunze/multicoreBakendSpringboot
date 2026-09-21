package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.financeira.dto.BankReconciliationSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.CreateBankExpenseAndMatchRequest;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.dto.ManualReconciliationRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP Desktop para o Centro de Reconciliação Bancária (/api/finance/reconciliation).
 */
@Component
@Profile("desktop")
public class BankReconciliationApiClient {

    private final DesktopClientFactory clientFactory;

    public BankReconciliationApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<BankStatementDTO> getStatements(Long accountId) {
        String url = accountId != null
                ? "/api/finance/reconciliation/statements?accountId=" + accountId
                : "/api/finance/reconciliation/statements";
        return clientFactory.authenticatedClient().getList(url, BankStatementDTO.class);
    }

    public BankStatementDTO getStatement(Long statementId) {
        return clientFactory.authenticatedClient().get("/api/finance/reconciliation/statements/" + statementId, BankStatementDTO.class);
    }

    public List<BankStatementItemDTO> getItems(Long statementId) {
        return clientFactory.authenticatedClient().getList("/api/finance/reconciliation/statements/" + statementId + "/items", BankStatementItemDTO.class);
    }

    public BankReconciliationSummaryDTO getSummary(Long statementId) {
        return clientFactory.authenticatedClient().get("/api/finance/reconciliation/statements/" + statementId + "/summary", BankReconciliationSummaryDTO.class);
    }

    public BankStatementDTO importStatement(ImportBankStatementRequest request) {
        return clientFactory.authenticatedClient().post("/api/finance/reconciliation/import", request, BankStatementDTO.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> autoMatch(Long statementId) {
        return clientFactory.authenticatedClient().post("/api/finance/reconciliation/auto-match/" + statementId, null, Map.class);
    }

    public BankStatementItemDTO manualMatch(ManualReconciliationRequest request) {
        return clientFactory.authenticatedClient().post("/api/finance/reconciliation/manual-match", request, BankStatementItemDTO.class);
    }

    public BankStatementItemDTO unmatch(Long itemId) {
        return clientFactory.authenticatedClient().post("/api/finance/reconciliation/unmatch/" + itemId, null, BankStatementItemDTO.class);
    }

    public BankStatementItemDTO createExpenseAndMatch(CreateBankExpenseAndMatchRequest request) {
        return clientFactory.authenticatedClient().post("/api/finance/reconciliation/create-expense-match", request, BankStatementItemDTO.class);
    }

    public BankStatementDTO closeStatement(Long statementId) {
        return clientFactory.authenticatedClient().post("/api/finance/reconciliation/close/" + statementId, null, BankStatementDTO.class);
    }

    public byte[] downloadReportPdf(Long statementId) {
        return clientFactory.authenticatedClient().getBytes("/api/finance/reconciliation/report/" + statementId);
    }
}
