package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TransactionType;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.model.TreasuryTransaction;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TreasuryOperationService {

    private final TreasuryAccountRepository accountRepository;
    private final TreasuryTransactionRepository transactionRepository;

    public TreasuryOperationService(TreasuryAccountRepository accountRepository,
                                    TreasuryTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

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

    public void registerAutoExpensePayout(BigDecimal amount, String description) {
        registerAutoPayout(amount, "Pagamento Despesa: " + description);
    }

    public TreasuryTransactionDTO registerAutoPayout(BigDecimal amount, String description) {
        List<TreasuryAccount> accounts = accountRepository.findByCompanyIdOrderByName(
                CurrentUserContext.getCurrentCompanyId());
        if (accounts.isEmpty()) {
            return null;
        }
        // The ledger posts automatic payouts (salaries, expenses, taxes) to Caixa: take them from the cash account.
        TreasuryAccount defaultAccount = accounts.stream()
                .filter(a -> a.getAccountType() == TreasuryAccountType.CASH).findFirst().orElse(accounts.get(0));

        defaultAccount.setBalance(defaultAccount.getBalance().subtract(amount));
        accountRepository.save(defaultAccount);

        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setTreasuryAccount(defaultAccount);
        tx.setTransactionType(TransactionType.CREDIT);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setTransactionDate(LocalDateTime.now());
        return toDTO(transactionRepository.save(tx));
    }

    public TreasuryTransactionDTO toDTO(TreasuryTransaction tx) {
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
