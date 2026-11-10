package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Manufactured products leave 3401 Produtos acabados on a sale; bought goods leave 3201 Mercadorias. */
class GoodsAccountsTest {

    private final PostingHelper helper = mock(PostingHelper.class);
    private final FinishedGoodsLookup lookup = mock(FinishedGoodsLookup.class);

    @BeforeEach
    void accounts() {
        for (String code : new String[]{PgcNirfChart.CMVMC, PgcNirfChart.MERCADORIAS, PgcNirfChart.PRODUTOS_ACABADOS}) {
            Account account = new Account();
            account.setCode(code);
            when(helper.account(1L, code)).thenReturn(account);
        }
        when(helper.hasAccounts(eq(1L), any(String[].class))).thenReturn(true);
    }

    @Test
    void saleSplitsTheCostBetweenGoodsAndFinishedProducts() {
        when(lookup.finishedCostOfInvoice(1L, 10L)).thenReturn(new BigDecimal("30.00"));
        JournalEntry entry = new JournalEntry();
        new GoodsAccounts(helper, lookup).addSaleCost(entry, 1L, 10L, new BigDecimal("100.00"));

        assertThat(debit(entry, PgcNirfChart.CMVMC)).isEqualByComparingTo("100");
        assertThat(credit(entry, PgcNirfChart.MERCADORIAS)).isEqualByComparingTo("70");
        assertThat(credit(entry, PgcNirfChart.PRODUTOS_ACABADOS)).isEqualByComparingTo("30");
    }

    @Test
    void returnPutsTheProductsBackWhereTheyCameFrom() {
        when(lookup.finishedCostOfCreditNote(1L, 20L)).thenReturn(new BigDecimal("30.00"));
        JournalEntry entry = new JournalEntry();
        new GoodsAccounts(helper, lookup).addReturnCost(entry, 1L, 20L, new BigDecimal("30.00"));

        assertThat(debit(entry, PgcNirfChart.PRODUTOS_ACABADOS)).isEqualByComparingTo("30");
        assertThat(debit(entry, PgcNirfChart.MERCADORIAS)).isEqualByComparingTo("0");
        assertThat(credit(entry, PgcNirfChart.CMVMC)).isEqualByComparingTo("30");
    }

    @Test
    void withoutTheLookupEverythingStaysInGoods() {
        JournalEntry entry = new JournalEntry();
        new GoodsAccounts(helper, null).addSaleCost(entry, 1L, 10L, new BigDecimal("100.00"));

        assertThat(credit(entry, PgcNirfChart.MERCADORIAS)).isEqualByComparingTo("100");
        assertThat(credit(entry, PgcNirfChart.PRODUTOS_ACABADOS)).isEqualByComparingTo("0");
    }

    private static BigDecimal debit(JournalEntry entry, String code) {
        return entry.getLines().stream().filter(l -> code.equals(l.getAccount().getCode()))
                .map(JournalLine::getDebit).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal credit(JournalEntry entry, String code) {
        return entry.getLines().stream().filter(l -> code.equals(l.getAccount().getCode()))
                .map(JournalLine::getCredit).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
