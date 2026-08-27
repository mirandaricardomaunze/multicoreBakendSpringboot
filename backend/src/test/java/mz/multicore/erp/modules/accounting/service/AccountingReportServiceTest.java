package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.LedgerDTO;
import mz.multicore.erp.modules.accounting.dto.TrialBalanceDTO;
import mz.multicore.erp.modules.accounting.dto.TrialBalanceLineDTO;
import mz.multicore.erp.modules.accounting.model.*;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Balancete e razão (CT-40..CT-46). Ver docs/CONTABILIDADE_SPEC.md §6.
 */
class AccountingReportServiceTest {

    private static final Long COMPANY_ID = 1L;

    private JournalService journalService;
    private AccountRepository accountRepository;
    private AccountingReportService service;

    private final Map<String, Account> chart = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        journalService = mock(JournalService.class);
        accountRepository = mock(AccountRepository.class);
        service = new AccountingReportService(journalService, accountRepository);

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("contabilista", "MANAGER");

        when(accountRepository.findByCompanyIdAndCode(eq(COMPANY_ID), anyString()))
                .thenAnswer(call -> Optional.ofNullable(account(call.getArgument(1))));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    /**
     * Conta com a natureza <b>do plano semeado</b>, não a da classe. IVA liquidado (2431) é
     * classe 2 mas credora: derivar a natureza da classe daria saldos com o sinal trocado — é
     * exactamente por isso que a natureza é gravada em cada conta.
     */
    private Account account(String code) {
        return chart.computeIfAbsent(code, c -> {
            AccountNature nature = PgcNirfChart.accounts().stream()
                    .filter(seed -> seed.code().equals(c))
                    .map(PgcNirfChart.Seed::nature)
                    .findFirst()
                    .orElse(AccountClass.ofCode(c).defaultNature());
            Account account = new Account();
            account.setCode(c);
            account.setName("Conta " + c);
            account.setAccountClass(AccountClass.ofCode(c));
            account.setNature(nature);
            account.setPostable(true);
            account.setActive(true);
            return account;
        });
    }

    /** Lançamento de venda a fiado: D Clientes 232 / C Vendas 200 / C IVA 32. */
    private JournalEntry venda(LocalDate date, String number, String total, String net, String tax) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(date);
        entry.setEntryNumber(number);
        entry.setDescription("Venda " + number);
        entry.setSource(JournalSource.INVOICE);
        entry.setSourceDocumentNumber("FT-" + number);
        entry.addLine(JournalLine.debit(account(PgcNirfChart.CLIENTES), new BigDecimal(total), null));
        entry.addLine(JournalLine.credit(account(PgcNirfChart.VENDAS), new BigDecimal(net), null));
        entry.addLine(JournalLine.credit(account(PgcNirfChart.IVA_LIQUIDADO), new BigDecimal(tax), null));
        return entry;
    }

    /** Recebimento: D Caixa / C Clientes. */
    private JournalEntry recebimento(LocalDate date, String number, String amount) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(date);
        entry.setEntryNumber(number);
        entry.setDescription("Recebimento " + number);
        entry.setSource(JournalSource.RECEIPT);
        entry.addLine(JournalLine.debit(account(PgcNirfChart.CAIXA), new BigDecimal(amount), null));
        entry.addLine(JournalLine.credit(account(PgcNirfChart.CLIENTES), new BigDecimal(amount), null));
        return entry;
    }

    private void stubEntries(LocalDate from, LocalDate to, List<JournalEntry> entries) {
        when(journalService.findEntriesBetween(eq(COMPANY_ID), any(), any())).thenAnswer(call -> {
            LocalDate askedFrom = call.getArgument(1);
            LocalDate askedTo = call.getArgument(2);
            List<JournalEntry> result = new ArrayList<>();
            for (JournalEntry entry : entries) {
                if (!entry.getEntryDate().isBefore(askedFrom) && !entry.getEntryDate().isAfter(askedTo)) {
                    result.add(entry);
                }
            }
            return result;
        });
    }

    private TrialBalanceLineDTO lineOf(TrialBalanceDTO balance, String code) {
        return balance.lines().stream().filter(l -> l.accountCode().equals(code)).findFirst().orElseThrow();
    }

    @Test // CT-40
    void balancete_somaPorContaEFecha() {
        LocalDate dia = LocalDate.of(2026, 8, 10);
        stubEntries(dia, dia, List.of(
                venda(dia, "LC-2026/1", "232.00", "200.00", "32.00"),
                recebimento(dia, "LC-2026/2", "232.00")));

        TrialBalanceDTO balance = service.getTrialBalance(dia, dia);

        assertTrue(balance.balanced(), "débitos e créditos têm de bater certo");
        assertEquals(new BigDecimal("464.00"), balance.totalDebit());
        assertEquals(new BigDecimal("464.00"), balance.totalCredit());
        assertEquals(new BigDecimal("200.00"), lineOf(balance, PgcNirfChart.VENDAS).balance());
        assertEquals(new BigDecimal("232.00"), lineOf(balance, PgcNirfChart.CAIXA).balance());
    }

    @Test // CT-41
    void balancete_clienteQuePagouFicaComSaldoZero() {
        LocalDate dia = LocalDate.of(2026, 8, 10);
        stubEntries(dia, dia, List.of(
                venda(dia, "LC-2026/1", "232.00", "200.00", "32.00"),
                recebimento(dia, "LC-2026/2", "232.00")));

        TrialBalanceLineDTO clientes = lineOf(service.getTrialBalance(dia, dia), PgcNirfChart.CLIENTES);

        assertEquals(new BigDecimal("232.00"), clientes.totalDebit());
        assertEquals(new BigDecimal("232.00"), clientes.totalCredit());
        assertEquals(new BigDecimal("0.00"), clientes.balance(), "vendeu e recebeu: não deve nada");
    }

    @Test // CT-42
    void balancete_saldoDeContaCredoraNaoSaiNegativo() {
        LocalDate dia = LocalDate.of(2026, 8, 10);
        stubEntries(dia, dia, List.of(venda(dia, "LC-2026/1", "232.00", "200.00", "32.00")));

        TrialBalanceDTO balance = service.getTrialBalance(dia, dia);

        // Vendas e IVA são credoras: o saldo é positivo do lado do crédito.
        assertEquals(new BigDecimal("200.00"), lineOf(balance, PgcNirfChart.VENDAS).balance());
        assertEquals(new BigDecimal("32.00"), lineOf(balance, PgcNirfChart.IVA_LIQUIDADO).balance());
        assertEquals(AccountNature.CREDORA, lineOf(balance, PgcNirfChart.VENDAS).nature());
    }

    @Test // CT-43
    void balancete_periodoSemLancamentosVemVazio_masEquilibrado() {
        LocalDate dia = LocalDate.of(2026, 1, 1);
        stubEntries(dia, dia, List.of());

        TrialBalanceDTO balance = service.getTrialBalance(dia, dia);

        assertTrue(balance.lines().isEmpty());
        assertEquals(new BigDecimal("0.00"), balance.totalDebit());
        assertTrue(balance.balanced());
    }

    @Test // CT-44
    void razao_temSaldoDeAberturaDoQueVemDeTras() {
        LocalDate janeiro = LocalDate.of(2026, 1, 10);
        LocalDate marco = LocalDate.of(2026, 3, 5);
        stubEntries(janeiro, marco, List.of(
                venda(janeiro, "LC-2026/1", "500.00", "500.00", "0.00"),
                venda(marco, "LC-2026/2", "232.00", "200.00", "32.00")));

        LedgerDTO ledger = service.getLedger(PgcNirfChart.CLIENTES,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(new BigDecimal("500.00"), ledger.openingBalance(),
                "o extracto de Março tem de começar com o que o cliente já devia");
        assertEquals(1, ledger.movements().size());
        assertEquals(new BigDecimal("732.00"), ledger.closingBalance());
    }

    @Test // CT-45
    void razao_saldoAcumuladoAcompanhaCadaMovimento() {
        LocalDate dia = LocalDate.of(2026, 8, 10);
        stubEntries(dia, dia, List.of(
                venda(dia, "LC-2026/1", "232.00", "200.00", "32.00"),
                recebimento(dia, "LC-2026/2", "100.00")));

        LedgerDTO ledger = service.getLedger(PgcNirfChart.CLIENTES, dia, dia);

        assertEquals(2, ledger.movements().size());
        assertEquals(new BigDecimal("232.00"), ledger.movements().get(0).runningBalance());
        assertEquals(new BigDecimal("132.00"), ledger.movements().get(1).runningBalance());
        assertEquals(new BigDecimal("132.00"), ledger.closingBalance());
        assertEquals(new BigDecimal("232.00"), ledger.totalDebit());
        assertEquals(new BigDecimal("100.00"), ledger.totalCredit());
    }

    @Test // CT-46
    void razao_soTrazOsMovimentosDaContaPedida() {
        LocalDate dia = LocalDate.of(2026, 8, 10);
        stubEntries(dia, dia, List.of(venda(dia, "LC-2026/1", "232.00", "200.00", "32.00")));

        LedgerDTO ledger = service.getLedger(PgcNirfChart.VENDAS, dia, dia);

        assertEquals(1, ledger.movements().size());
        assertEquals(new BigDecimal("200.00"), ledger.movements().get(0).credit());
        assertEquals(new BigDecimal("200.00"), ledger.closingBalance());
        assertEquals("FT-LC-2026/1", ledger.movements().get(0).sourceDocumentNumber(),
                "o razão diz qual foi o documento de origem, sem obrigar a ir buscá-lo");
    }
}
