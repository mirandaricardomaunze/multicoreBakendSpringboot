package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryTransaction;
import mz.multicore.erp.modules.financeira.model.TransactionType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Harness da paginação real de tesouraria definida na SPEC de robustez multiutilizador. */
class TreasuryPaginationHarnessTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void transactionsPage_isTenantScopedAndCappedAtServer() {
        var accounts = mock(TreasuryAccountRepository.class);
        var transactions = mock(TreasuryTransactionRepository.class);
        var service = new FinanceService(accounts, transactions, mock(InvoiceRepository.class));
        CurrentUserContext.setCurrentCompanyId(7L);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");

        TreasuryAccount account = new TreasuryAccount();
        account.setId(3L);
        account.setName("Caixa principal");
        TreasuryTransaction transaction = new TreasuryTransaction();
        transaction.setId(11L);
        transaction.setTreasuryAccount(account);
        transaction.setTransactionType(TransactionType.DEBIT);
        transaction.setAmount(new BigDecimal("125.50"));
        transaction.setDescription("Recebimento");
        transaction.setTransactionDate(LocalDateTime.of(2026, 10, 11, 10, 30));

        var expectedPageable = org.springframework.data.domain.PageRequest.of(0, 200);
        when(transactions.findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(
                eq(7L), eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of(transaction), expectedPageable, 401));

        PageResponse<TreasuryTransactionDTO> result = service.getTransactionsPage(-1, 10_000);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(200);
        assertThat(result.totalElements()).isEqualTo(401);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.items()).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(11L);
            assertThat(dto.accountId()).isEqualTo(3L);
        });
        verify(transactions).findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(7L, expectedPageable);
    }
}
