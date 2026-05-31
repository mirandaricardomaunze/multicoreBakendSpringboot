package com.phcpro.modules.financeira.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.financeira.dto.*;
import com.phcpro.modules.financeira.model.*;
import com.phcpro.modules.financeira.repository.*;
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
        return accountRepository.findAll().stream()
                .map(a -> new TreasuryAccountDTO(a.getId(), a.getName(), a.getAccountNumber(), a.getBalance()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TreasuryTransactionDTO> getAllTransactions() {
        return transactionRepository.findAllByOrderByTransactionDateDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TreasuryTransactionDTO registerTransaction(Long accountId, String type, BigDecimal amount, String description) {
        TreasuryAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("Conta de tesouraria não encontrada."));

        if ("DEBIT".equalsIgnoreCase(type)) {
            account.setBalance(account.getBalance().add(amount));
        } else if ("CREDIT".equalsIgnoreCase(type)) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new BusinessRuleException("Saldo insuficiente na conta " + account.getName() + " para esta operação.");
            }
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            throw new BusinessRuleException("Tipo de transação inválido: " + type);
        }

        account = accountRepository.save(account);

        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setTreasuryAccount(account);
        tx.setTransactionType(type.toUpperCase());
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setTransactionDate(LocalDateTime.now());
        tx = transactionRepository.save(tx);

        return toDTO(tx);
    }

    @Transactional
    public void payInvoice(Long invoiceId, Long accountId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        if (invoice.getStatus() != InvoiceStatus.APPROVED) {
            throw new BusinessRuleException("Apenas faturas no estado APROVADA podem ser pagas. Estado atual: " + invoice.getStatus());
        }

        // Settling sales invoice represents an inflow of money (DEBIT)
        String description = "Recebimento Fatura " + invoice.getInvoiceNumber() + " - " + invoice.getClient().getName();
        registerTransaction(accountId, "DEBIT", invoice.getTotalAmount(), description);

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void registerAutoExpensePayout(BigDecimal amount, String description) {
        // Automatically deduct from the first account (Cash/Caixa Geral) if available
        List<TreasuryAccount> accounts = accountRepository.findAll();
        if (accounts.isEmpty()) {
            return; // No account set up yet, skip auto payout log or handle gracefully
        }
        TreasuryAccount defaultAccount = accounts.get(0);
        
        // Register an outflow (CREDIT) for the employee reimbursement
        defaultAccount.setBalance(defaultAccount.getBalance().subtract(amount));
        accountRepository.save(defaultAccount);

        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setTreasuryAccount(defaultAccount);
        tx.setTransactionType("CREDIT");
        tx.setAmount(amount);
        tx.setDescription("Pagamento Despesa: " + description);
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    private TreasuryTransactionDTO toDTO(TreasuryTransaction tx) {
        return new TreasuryTransactionDTO(
                tx.getId(),
                tx.getTreasuryAccount().getId(),
                tx.getTreasuryAccount().getName(),
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getTransactionDate()
        );
    }
}
