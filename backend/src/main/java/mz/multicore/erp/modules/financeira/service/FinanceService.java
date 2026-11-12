package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.service.InvoiceCommandService;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FinanceService {

    private final TreasuryOperationService operationService;
    private final TreasuryQueryService queryService;
    private final InvoiceCommandService invoiceCommands;

    @Autowired
    public FinanceService(TreasuryOperationService operationService,
                          TreasuryQueryService queryService,
                          @Lazy InvoiceCommandService invoiceCommands) {
        this.operationService = operationService;
        this.queryService = queryService;
        this.invoiceCommands = invoiceCommands;
    }

    public FinanceService(TreasuryAccountRepository accountRepository,
                          TreasuryTransactionRepository transactionRepository,
                          @Lazy InvoiceCommandService invoiceCommands) {
        TreasuryOperationService op = new TreasuryOperationService(accountRepository, transactionRepository);
        this.operationService = op;
        this.queryService = new TreasuryQueryService(accountRepository, transactionRepository, op);
        this.invoiceCommands = invoiceCommands;
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
    public void payInvoice(Long invoiceId, Long accountId) {
        PermissionGuard.requireManagerOrAdmin("liquidar fatura");

        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Invoice invoice = invoiceCommands.lockForCompany(invoiceId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        if (!invoice.getStatus().isCollectable()) {
            throw new BusinessRuleException("Apenas faturas por cobrar podem ser liquidadas. Estado atual: " + invoice.getStatus());
        }

        BigDecimal outstanding = invoice.outstandingAmount();
        String description = "Recebimento Fatura " + invoice.getInvoiceNumber() + " - " + invoice.getClient().getName();
        registerTransaction(accountId, "DEBIT", outstanding, description);

        invoice.setAmountPaid(invoice.getTotalAmount());
        invoice.setStatus(invoice.deriveStatusFromPayments());
        invoiceCommands.save(invoice);
    }

    @Transactional
    public void registerAutoExpensePayout(BigDecimal amount, String description) {
        operationService.registerAutoExpensePayout(amount, description);
    }

    @Transactional
    public void registerAutoPayout(BigDecimal amount, String description) {
        operationService.registerAutoPayout(amount, description);
    }
}
