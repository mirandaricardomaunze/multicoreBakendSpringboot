package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.PayoutPurpose;
import mz.multicore.erp.architecture.events.TreasuryPayoutEvent;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HR payouts that left the treasury without a posting now reach the books, on the account of their purpose. */
class TreasuryPayoutPostingServiceTest {

    @Test
    void staffAdvanceDebitsTheStaffAccountAndCreditsTheCash() {
        PostingHelper helper = mock(PostingHelper.class);
        JournalService journal = mock(JournalService.class);
        when(helper.canPost(1L)).thenReturn(true);
        when(helper.hasAccounts(eq(1L), any(String[].class))).thenReturn(true);
        when(helper.account(eq(1L), anyString())).thenAnswer(inv -> account(inv.getArgument(1)));
        when(helper.cashAccount(eq(1L), anyBoolean())).thenAnswer(inv -> account((Boolean) inv.getArgument(1) ? "1101" : "1201"));

        new TreasuryPayoutPostingService(helper, journal).onPayout(new TreasuryPayoutEvent(1L, 77L, LocalDate.now(),
                new BigDecimal("5000"), PayoutPurpose.STAFF_ADVANCE, true, "Adiantamento a Joao"));

        ArgumentCaptor<JournalEntry> entry = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journal).save(entry.capture(), eq(1L));
        assertThat(entry.getValue().getLines().get(0).getAccount().getCode()).isEqualTo(PgcNirfChart.PESSOAL_OUTROS_DESCONTOS);
        assertThat(entry.getValue().getLines().get(1).getAccount().getCode()).isEqualTo(PgcNirfChart.CAIXA);
        assertThat(entry.getValue().getLines().get(0).getDebit()).isEqualByComparingTo("5000");
    }

    private static Account account(String code) {
        Account account = new Account();
        account.setCode(code);
        return account;
    }
}
