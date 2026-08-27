package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.financeira.dto.*;
import mz.multicore.erp.modules.financeira.model.*;
import mz.multicore.erp.modules.financeira.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private final TreasuryAccountRepository accountRepository;
    private final TreasuryTransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;

    public FinanceService(
            TreasuryAccountRepository accountRepository,
            TreasuryTransactionRepository transactionRepository,
            @Lazy InvoiceRepository invoiceRepository // Lazy to avoid recursive resolution
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<TreasuryAccountDTO> getAllAccounts() {
        return accountRepository.findByCompanyIdOrderByName(CurrentUserContext.getCurrentCompanyId()).stream()
                .map(a -> new TreasuryAccountDTO(a.getId(), a.getName(), a.getAccountNumber(), a.getBalance()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TreasuryTransactionDTO> getAllTransactions() {
        return transactionRepository.findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(
                        CurrentUserContext.getCurrentCompanyId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TreasuryTransactionDTO registerTransaction(Long accountId, String type, BigDecimal amount, String description) {
        TransactionType transactionType;
        try {
            transactionType = TransactionType.valueOf(type == null ? "" : type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de transação inválido: " + type);
        }

        TreasuryAccount account = accountRepository.findByIdAndCompanyId(accountId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Conta de tesouraria não encontrada."));

        if (transactionType == TransactionType.DEBIT) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new BusinessRuleException("Saldo insuficiente na conta " + account.getName() + " para esta operação.");
            }
            account.setBalance(account.getBalance().subtract(amount));
        }

        account = accountRepository.save(account);

        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setTreasuryAccount(account);
        tx.setTransactionType(transactionType);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setTransactionDate(LocalDateTime.now());
        tx = transactionRepository.save(tx);

        return toDTO(tx);
    }

    @Transactional
    public void payInvoice(Long invoiceId, Long accountId) {
        // Liquidar uma fatura é movimento de dinheiro: exige gerente, como anular recibo.
        PermissionGuard.requireManagerOrAdmin("liquidar fatura");

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        if (!invoice.getStatus().isCollectable()) {
            throw new BusinessRuleException("Apenas faturas por cobrar podem ser liquidadas. Estado atual: " + invoice.getStatus());
        }

        // Só entra o que falta receber — o total incluiria outra vez o que já foi pago
        // por recibo ou no POS, contando esse valor duas vezes na tesouraria.
        BigDecimal outstanding = invoice.outstandingAmount();
        String description = "Recebimento Fatura " + invoice.getInvoiceNumber() + " - " + invoice.getClient().getName();
        registerTransaction(accountId, "DEBIT", outstanding, description);

        invoice.setAmountPaid(invoice.getTotalAmount());
        invoice.setStatus(invoice.deriveStatusFromPayments());
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void registerAutoExpensePayout(BigDecimal amount, String description) {
        registerAutoPayout(amount, "Pagamento Despesa: " + description);
    }

    /**
     * Regista uma saída de caixa (CREDIT) na conta de tesouraria por defeito da empresa
     * activa — usado para pagamentos automáticos (reembolsos de despesa, salários).
     * Se a empresa ainda não tem conta configurada, a operação é ignorada graciosamente.
     */
    @Transactional
    public void registerAutoPayout(BigDecimal amount, String description) {
        List<TreasuryAccount> accounts = accountRepository.findByCompanyIdOrderByName(
                CurrentUserContext.getCurrentCompanyId());
        if (accounts.isEmpty()) {
            return; // No account set up yet, skip auto payout gracefully
        }
        TreasuryAccount defaultAccount = accounts.get(0);

        defaultAccount.setBalance(defaultAccount.getBalance().subtract(amount));
        accountRepository.save(defaultAccount);

        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setTreasuryAccount(defaultAccount);
        tx.setTransactionType(TransactionType.CREDIT);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    private TreasuryTransactionDTO toDTO(TreasuryTransaction tx) {
        return new TreasuryTransactionDTO(
                tx.getId(),
                tx.getTreasuryAccount().getId(),
                tx.getTreasuryAccount().getName(),
                tx.getTransactionType().name(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getTransactionDate()
        );
    }
}
