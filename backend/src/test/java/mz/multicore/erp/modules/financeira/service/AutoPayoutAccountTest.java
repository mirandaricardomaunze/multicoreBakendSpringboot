package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Automatic payouts (salaries, expenses) are posted to Caixa, so they must leave the cash account, not the bank. */
class AutoPayoutAccountTest {

    @AfterEach
    void clear() {
        CurrentUserContext.clear();
    }

    @Test
    void payoutLeavesTheCashAccountEvenWhenABankSortsFirst() {
        CurrentUserContext.setCurrentCompanyId(1L);
        TreasuryAccount bank = account("Banco BCI", TreasuryAccountType.BANK);
        TreasuryAccount cash = account("Caixa Loja", TreasuryAccountType.CASH);
        TreasuryAccountRepository accounts = mock(TreasuryAccountRepository.class);
        when(accounts.findByCompanyIdOrderByName(1L)).thenReturn(List.of(bank, cash));

        TreasuryTransactionRepository transactions = mock(TreasuryTransactionRepository.class);
        when(transactions.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        new TreasuryOperationService(accounts, transactions)
                .registerAutoPayout(new BigDecimal("300"), "Pagamento Salário REC-2026/1");

        assertThat(cash.getBalance()).isEqualByComparingTo("700");
        assertThat(bank.getBalance()).isEqualByComparingTo("1000");
    }

    private static TreasuryAccount account(String name, TreasuryAccountType type) {
        TreasuryAccount account = new TreasuryAccount();
        account.setName(name);
        account.setAccountType(type);
        account.setBalance(new BigDecimal("1000"));
        return account;
    }
}
