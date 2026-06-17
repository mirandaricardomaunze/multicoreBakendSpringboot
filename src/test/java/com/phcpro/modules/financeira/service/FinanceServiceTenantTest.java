package com.phcpro.modules.financeira.service;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.financeira.model.TreasuryAccount;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.repository.TreasuryTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceServiceTenantTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void getAllAccounts_readsOnlyActiveCompany() {
        TreasuryAccountRepository accounts = mock(TreasuryAccountRepository.class);
        TreasuryTransactionRepository transactions = mock(TreasuryTransactionRepository.class);
        FinanceService service = new FinanceService(accounts, transactions, mock(InvoiceRepository.class));
        CurrentUserContext.setCurrentCompanyId(7L);

        Company company = new Company();
        company.setId(7L);
        TreasuryAccount account = new TreasuryAccount();
        account.setId(10L);
        account.setName("Conta Maputo");
        account.setBalance(new BigDecimal("1000.00"));
        account.setCompany(company);
        when(accounts.findByCompanyIdOrderByName(7L)).thenReturn(List.of(account));

        var result = service.getAllAccounts();

        assertEquals(1, result.size());
        assertEquals("Conta Maputo", result.get(0).name());
        verify(accounts).findByCompanyIdOrderByName(7L);
    }
}
