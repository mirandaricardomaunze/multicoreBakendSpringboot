package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Utilitário e resolução de contas PGC-NIRF para lançamentos automáticos.
 */
@Component
public class PostingHelper {

    private final ChartOfAccountsService chartOfAccountsService;
    private final JournalService journalService;

    public PostingHelper(ChartOfAccountsService chartOfAccountsService, JournalService journalService) {
        this.chartOfAccountsService = chartOfAccountsService;
        this.journalService = journalService;
    }

    public boolean canPost(Long companyId) {
        return companyId != null && chartOfAccountsService.hasChart(companyId);
    }

    public boolean hasPayrollAccounts(Long companyId) {
        return chartOfAccountsService.hasAccounts(companyId,
                PgcNirfChart.CUSTOS_PESSOAL, PgcNirfChart.ENCARGOS_PESSOAL,
                PgcNirfChart.IRPS_RETIDO, PgcNirfChart.INSS_A_ENTREGAR,
                PgcNirfChart.PESSOAL_OUTROS_DESCONTOS, PgcNirfChart.CAIXA);
    }

    public boolean hasAccounts(Long companyId, String... codes) {
        return chartOfAccountsService.hasAccounts(companyId, codes);
    }

    public boolean alreadyPosted(Long companyId, JournalSource source, Long documentId) {
        return documentId != null
                && journalService.findByDocument(companyId, source, documentId).isPresent();
    }

    public Account cashAccount(Long companyId, boolean cash) {
        return account(companyId, cash ? PgcNirfChart.CAIXA : PgcNirfChart.BANCO);
    }

    public Account account(Long companyId, String code) {
        return chartOfAccountsService.requirePostableAccount(companyId, code);
    }

    public static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
