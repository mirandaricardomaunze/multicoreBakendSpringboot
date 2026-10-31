package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.BankReconciliationSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.repository.BankStatementItemRepository;
import mz.multicore.erp.modules.financeira.repository.BankStatementRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestrador executivo para conciliação bancária e conferência de tesouraria.
 * Delega o ciclo de vida para colaboradores especializados respeitando os limites físicos de linhas.
 */
@Service
public class BankReconciliationService {

    private final BankStatementImportService importService;
    private final BankReconciliationMatcher matcher;
    private final BankReconciliationQueryService queryService;

    @Autowired
    public BankReconciliationService(
            BankStatementImportService importService,
            BankReconciliationMatcher matcher,
            BankReconciliationQueryService queryService
    ) {
        this.importService = importService;
        this.matcher = matcher;
        this.queryService = queryService;
    }

    public BankReconciliationService(
            BankStatementRepository statementRepository,
            BankStatementItemRepository itemRepository,
            TreasuryAccountRepository accountRepository,
            TreasuryTransactionRepository transactionRepository,
            FinanceService financeService,
            CompanyService companyService
    ) {
        this.matcher = new BankReconciliationMatcher(
                statementRepository, itemRepository, transactionRepository, financeService);
        this.importService = new BankStatementImportService(
                statementRepository, accountRepository, companyService, this.matcher);
        this.queryService = new BankReconciliationQueryService(
                statementRepository, itemRepository, accountRepository);
    }

    @Transactional
    public BankStatementDTO importStatement(ImportBankStatementRequest req) {
        return importService.importStatement(req);
    }

    @Transactional
    public int autoMatch(Long statementId) {
        return matcher.autoMatch(statementId);
    }

    @Transactional
    public BankStatementItemDTO manualMatch(Long statementItemId, Long transactionId) {
        return matcher.manualMatch(statementItemId, transactionId);
    }

    @Transactional
    public BankStatementItemDTO unmatch(Long statementItemId) {
        return matcher.unmatch(statementItemId);
    }

    @Transactional
    public BankStatementItemDTO createAndMatchExpense(Long statementItemId, String description, BigDecimal amount) {
        return matcher.createAndMatchExpense(statementItemId, description, amount);
    }

    @Transactional
    public BankStatementDTO closeStatement(Long statementId) {
        return importService.closeStatement(statementId);
    }

    @Transactional(readOnly = true)
    public List<BankStatementDTO> getStatements(Long treasuryAccountId) {
        return queryService.getStatements(treasuryAccountId);
    }

    @Transactional(readOnly = true)
    public BankStatementDTO getStatement(Long statementId) {
        return queryService.getStatement(statementId);
    }

    @Transactional(readOnly = true)
    public List<BankStatementItemDTO> getStatementItems(Long statementId) {
        return queryService.getStatementItems(statementId);
    }

    @Transactional(readOnly = true)
    public BankReconciliationSummaryDTO getSummary(Long treasuryAccountId) {
        return queryService.getSummary(treasuryAccountId);
    }
}
