package com.phcpro.modules.accounting.model;

import com.phcpro.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Partida dobrada — regra de domínio pura (CT-01..CT-10).
 * Ver docs/CONTABILIDADE_SPEC.md.
 */
class JournalEntryTest {

    private Account account(String code, AccountNature nature, boolean postable) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Conta " + code);
        account.setAccountClass(AccountClass.ofCode(code));
        account.setNature(nature);
        account.setPostable(postable);
        account.setActive(true);
        return account;
    }

    private JournalEntry entry() {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.of(2026, 8, 15));
        entry.setDescription("Teste");
        return entry;
    }

    @Test // CT-01
    void lancamentoEquilibradoPassa() {
        JournalEntry entry = entry();
        entry.addLine(JournalLine.debit(account("1101", AccountNature.DEVEDORA, true), new BigDecimal("232.00"), null));
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), new BigDecimal("200.00"), null));
        entry.addLine(JournalLine.credit(account("2431", AccountNature.CREDORA, true), new BigDecimal("32.00"), null));

        assertDoesNotThrow(entry::validateForPosting);
        assertTrue(entry.isBalanced());
        assertEquals(new BigDecimal("232.00"), entry.totalDebit());
        assertEquals(new BigDecimal("232.00"), entry.totalCredit());
    }

    @Test // CT-02
    void lancamentoDesequilibradoERecusado_eDizOsDoisTotais() {
        JournalEntry entry = entry();
        entry.addLine(JournalLine.debit(account("1101", AccountNature.DEVEDORA, true), new BigDecimal("100.00"), null));
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), new BigDecimal("90.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateForPosting);

        assertTrue(error.getMessage().contains("desequilibrado"));
        assertTrue(error.getMessage().contains("100.00"));
        assertTrue(error.getMessage().contains("90.00"));
    }

    @Test // CT-03
    void umaSoPartidaNaoEPartidaDobrada() {
        JournalEntry entry = entry();
        entry.addLine(JournalLine.debit(account("1101", AccountNature.DEVEDORA, true), new BigDecimal("50.00"), null));

        assertThrows(BusinessRuleException.class, entry::validateForPosting);
    }

    @Test // CT-04
    void naoSeLancaEmContaMae() {
        JournalEntry entry = entry();
        entry.addLine(JournalLine.debit(account("11", AccountNature.DEVEDORA, /*postable*/ false), new BigDecimal("10.00"), null));
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), new BigDecimal("10.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateForPosting);

        assertTrue(error.getMessage().contains("conta-mãe"));
    }

    @Test // CT-05
    void partidaComOsDoisLadosPreenchidosERecusada() {
        JournalLine ambigua = new JournalLine();
        ambigua.setAccount(account("1101", AccountNature.DEVEDORA, true));
        ambigua.setDebit(new BigDecimal("10.00"));
        ambigua.setCredit(new BigDecimal("10.00"));

        JournalEntry entry = entry();
        entry.addLine(ambigua);
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), new BigDecimal("10.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateForPosting);

        assertTrue(error.getMessage().contains("débito e crédito ao mesmo tempo"));
    }

    @Test // CT-06
    void partidaSemValorERecusada() {
        JournalLine vazia = new JournalLine();
        vazia.setAccount(account("1101", AccountNature.DEVEDORA, true));

        JournalEntry entry = entry();
        entry.addLine(vazia);
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), new BigDecimal("10.00"), null));

        assertThrows(BusinessRuleException.class, entry::validateForPosting);
    }

    @Test // CT-07
    void valorNegativoNaoEPartida() {
        JournalEntry entry = entry();
        entry.addLine(JournalLine.debit(account("1101", AccountNature.DEVEDORA, true), new BigDecimal("-10.00"), null));
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), new BigDecimal("-10.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateForPosting);

        assertTrue(error.getMessage().contains("negativos"));
    }

    @Test // CT-08
    void lancamentoDeValorZeroNaoMovimentaNada() {
        JournalEntry entry = entry();
        entry.addLine(JournalLine.debit(account("1101", AccountNature.DEVEDORA, true), BigDecimal.ZERO, null));
        entry.addLine(JournalLine.credit(account("7101", AccountNature.CREDORA, true), BigDecimal.ZERO, null));

        assertThrows(BusinessRuleException.class, entry::validateForPosting);
    }

    @Test // CT-09
    void naturezaDaContaDecideOSinalDoSaldo() {
        // Caixa (devedora): entra 100, sai 30 → saldo 70 devedor.
        assertEquals(new BigDecimal("70"),
                AccountNature.DEVEDORA.balanceOf(new BigDecimal("100"), new BigDecimal("30")));
        // Fornecedores (credora): credita 100, debita 30 → saldo 70 credor (positivo na natureza).
        assertEquals(new BigDecimal("70"),
                AccountNature.CREDORA.balanceOf(new BigDecimal("30"), new BigDecimal("100")));
    }

    @Test // CT-10
    void classeVemDoPrimeiroDigitoDoCodigo() {
        assertEquals(AccountClass.MEIOS_CIRCULANTES_FINANCEIROS, AccountClass.ofCode("1101"));
        assertEquals(AccountClass.TERCEIROS, AccountClass.ofCode("2101"));
        assertEquals(AccountClass.PROVEITOS_E_GANHOS, AccountClass.ofCode("7101"));
        assertThrows(IllegalArgumentException.class, () -> AccountClass.ofCode("9999"));
        assertThrows(IllegalArgumentException.class, () -> AccountClass.ofCode(""));
    }

    @Test // CT-11
    void codigoDaContaMaeEOCodigoMenosUmDigito() {
        assertEquals("210", Account.parentCodeOf("2101"));
        assertEquals("2", Account.parentCodeOf("21"));
        assertNull(Account.parentCodeOf("2"));
        assertNull(Account.parentCodeOf(null));
    }
}
