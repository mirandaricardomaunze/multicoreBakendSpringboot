package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.dto.ReceiptDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Money received for an invoice from the treasury screen or an instalment plan. It always goes through a receipt (RC):
 * the invoice is never settled without the fiscal document, the posting (Caixa or Banco / Clientes) and, in cash with an
 * open POS session, the drawer. The method follows the account: a cash account is cash, any other is a transfer.
 */
@Service
public class InvoiceSettlementService {

    private final InvoiceRepository invoiceRepository;
    private final ComercialService comercialService;
    private final FinanceService financeService;

    public InvoiceSettlementService(InvoiceRepository invoiceRepository, @Lazy ComercialService comercialService,
                                    @Lazy FinanceService financeService) {
        this.invoiceRepository = invoiceRepository;
        this.comercialService = comercialService;
        this.financeService = financeService;
    }

    /** Receives {@code amount} (null: everything still owed) into the account and issues the receipt. */
    @Transactional
    public ReceiptDTO settle(Long invoiceId, Long accountId, BigDecimal amount) {
        PermissionGuard.requireManagerOrAdmin("liquidar fatura");
        if (accountId == null) throw new BusinessRuleException("Indique a conta onde o dinheiro entrou.");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Invoice invoice = invoiceRepository.lockByIdAndCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        if (!invoice.getStatus().isCollectable()) {
            throw new BusinessRuleException("Apenas faturas por cobrar podem ser liquidadas. Estado atual: " + invoice.getStatus());
        }
        BigDecimal value = amount == null ? invoice.outstandingAmount() : amount;
        String method = financeService.accountType(accountId) == TreasuryAccountType.CASH ? "CASH" : "BANK_TRANSFER";
        return comercialService.createReceipt(invoiceId, accountId, method, value);
    }
}
