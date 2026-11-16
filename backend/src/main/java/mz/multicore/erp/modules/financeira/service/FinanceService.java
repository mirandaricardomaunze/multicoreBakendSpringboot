package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FinanceService {

    private final TreasuryOperationService operationService;
    private final TreasuryQueryService queryService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public FinanceService(TreasuryOperationService operationService, TreasuryQueryService queryService) {
        this(operationService, queryService, null);
    }

    @Autowired
    public FinanceService(TreasuryOperationService operationService,
                          TreasuryQueryService queryService,
                          org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.operationService = operationService;
        this.queryService = queryService;
        this.eventPublisher = eventPublisher;
    }

    public FinanceService(TreasuryAccountRepository accountRepository,
                          TreasuryTransactionRepository transactionRepository) {
        TreasuryOperationService op = new TreasuryOperationService(accountRepository, transactionRepository);
        this.operationService = op;
        this.queryService = new TreasuryQueryService(accountRepository, transactionRepository, op);
        this.eventPublisher = null;
    }

    @Transactional(readOnly = true)
    public List<TreasuryAccountDTO> getAllAccounts() { return queryService.getAllAccounts(); }

    @Transactional(readOnly = true)
    public List<TreasuryTransactionDTO> getAllTransactions() { return queryService.getAllTransactions(); }

    @Transactional(readOnly = true)
    public PageResponse<TreasuryTransactionDTO> getTransactionsPage(Integer page, Integer size) {
        return queryService.getTransactionsPage(page, size);
    }

    @Transactional
    public TreasuryTransactionDTO registerTransaction(Long accountId, String type, BigDecimal amount, String description) {
        return operationService.registerTransaction(accountId, type, amount, description);
    }

    @Transactional(readOnly = true)
    public TreasuryAccountType accountType(Long accountId) { return queryService.accountType(accountId); }

    @Transactional
    public void registerAutoExpensePayout(BigDecimal amount, String description) {
        registerAutoPayout(amount, "Pagamento Despesa: " + description, mz.multicore.erp.architecture.events.PayoutPurpose.STAFF_EXPENSE);
    }

    /** Automatic payout that the books must also see: the ledger posts it by purpose (TreasuryPayoutEvent). */
    @Transactional
    public void registerAutoPayout(BigDecimal amount, String description, mz.multicore.erp.architecture.events.PayoutPurpose purpose) {
        TreasuryTransactionDTO tx = operationService.registerAutoPayout(amount, description);
        if (tx == null || eventPublisher == null) return;
        eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.TreasuryPayoutEvent(
                mz.multicore.erp.architecture.security.CurrentUserContext.getCurrentCompanyId(), tx.id(),
                tx.transactionDate().toLocalDate(), amount, purpose, accountType(tx.accountId()) == TreasuryAccountType.CASH, description));
    }

    @Transactional
    public void registerAutoPayout(BigDecimal amount, String description) {
        operationService.registerAutoPayout(amount, description);
    }
}
