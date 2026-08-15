package com.phcpro.modules.accounting.service;

import com.phcpro.architecture.events.PaymentReceivedEvent;
import com.phcpro.architecture.events.SaleRegisteredEvent;
import com.phcpro.modules.accounting.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Lançamentos automáticos a partir dos factos de negócio (CT-20..CT-27).
 * Ver docs/CONTABILIDADE_SPEC.md §5.
 */
class AutomaticPostingServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate HOJE = LocalDate.of(2026, 8, 15);

    private ChartOfAccountsService chartOfAccountsService;
    private JournalService journalService;
    private AutomaticPostingService service;

    @BeforeEach
    void setUp() {
        chartOfAccountsService = mock(ChartOfAccountsService.class);
        journalService = mock(JournalService.class);
        service = new AutomaticPostingService(chartOfAccountsService, journalService);

        when(chartOfAccountsService.hasChart(COMPANY_ID)).thenReturn(true);
        when(chartOfAccountsService.requirePostableAccount(eq(COMPANY_ID), anyString()))
                .thenAnswer(call -> account(call.getArgument(1)));
        when(journalService.findByDocument(any(), any(), any())).thenReturn(Optional.empty());
    }

    private Account account(String code) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Conta " + code);
        account.setAccountClass(AccountClass.ofCode(code));
        account.setNature(AccountClass.ofCode(code).defaultNature());
        account.setPostable(true);
        account.setActive(true);
        return account;
    }

    private SaleRegisteredEvent venda(String net, String tax, String total, String cost,
                                      String paidNow, boolean cash) {
        return new SaleRegisteredEvent(COMPANY_ID, 10L, "FT-2026/1", HOJE,
                new BigDecimal(net), new BigDecimal(tax), new BigDecimal(total),
                new BigDecimal(cost), new BigDecimal(paidNow), cash);
    }

    private JournalEntry captureEntry() {
        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalService).save(captor.capture(), eq(COMPANY_ID));
        return captor.getValue();
    }

    private BigDecimal debitOf(JournalEntry entry, String code) {
        return entry.getLines().stream()
                .filter(line -> line.getAccount().getCode().equals(code))
                .map(JournalLine::safeDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal creditOf(JournalEntry entry, String code) {
        return entry.getLines().stream()
                .filter(line -> line.getAccount().getCode().equals(code))
                .map(JournalLine::safeCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test // CT-20
    void vendaAFiado_debitaClientes_creditaVendasEIva() {
        service.onSaleRegistered(venda("200.00", "32.00", "232.00", "0", "0", false));

        JournalEntry entry = captureEntry();
        assertTrue(entry.isBalanced());
        assertEquals(new BigDecimal("232.00"), debitOf(entry, PgcNirfChart.CLIENTES));
        assertEquals(new BigDecimal("200.00"), creditOf(entry, PgcNirfChart.VENDAS));
        assertEquals(new BigDecimal("32.00"), creditOf(entry, PgcNirfChart.IVA_LIQUIDADO));
        assertEquals(JournalSource.INVOICE, entry.getSource());
        assertEquals("FT-2026/1", entry.getSourceDocumentNumber());
    }

    @Test // CT-21
    void vendaPagaEmNumerario_entraEmCaixa_naoEmClientes() {
        service.onSaleRegistered(venda("200.00", "32.00", "232.00", "0", "232.00", true));

        JournalEntry entry = captureEntry();
        assertEquals(new BigDecimal("232.00"), debitOf(entry, PgcNirfChart.CAIXA));
        assertEquals(BigDecimal.ZERO, debitOf(entry, PgcNirfChart.CLIENTES));
        assertTrue(entry.isBalanced());
    }

    @Test // CT-22
    void vendaPagaPorBanco_entraEmDepositos() {
        service.onSaleRegistered(venda("100.00", "16.00", "116.00", "0", "116.00", false));

        JournalEntry entry = captureEntry();
        assertEquals(new BigDecimal("116.00"), debitOf(entry, PgcNirfChart.BANCO));
        assertEquals(BigDecimal.ZERO, debitOf(entry, PgcNirfChart.CAIXA));
    }

    @Test // CT-23
    void vendaParcialmentePaga_repartePorCaixaEClientes() {
        service.onSaleRegistered(venda("100.00", "16.00", "116.00", "0", "40.00", true));

        JournalEntry entry = captureEntry();
        assertEquals(new BigDecimal("40.00"), debitOf(entry, PgcNirfChart.CAIXA));
        assertEquals(new BigDecimal("76.00"), debitOf(entry, PgcNirfChart.CLIENTES));
        assertTrue(entry.isBalanced());
    }

    @Test // CT-24
    void comCustoConhecido_lancaTambemOCustoDasMercadoriasVendidas() {
        service.onSaleRegistered(venda("200.00", "32.00", "232.00", "120.00", "0", false));

        JournalEntry entry = captureEntry();
        assertEquals(new BigDecimal("120.00"), debitOf(entry, PgcNirfChart.CMVMC));
        assertEquals(new BigDecimal("120.00"), creditOf(entry, PgcNirfChart.MERCADORIAS));
        assertTrue(entry.isBalanced(), "o par CMVMC/Mercadorias não pode desequilibrar o lançamento");
    }

    @Test // CT-25
    void semCustoConhecido_naoInventaCusto() {
        service.onSaleRegistered(venda("200.00", "32.00", "232.00", "0", "0", false));

        JournalEntry entry = captureEntry();
        assertEquals(BigDecimal.ZERO, debitOf(entry, PgcNirfChart.CMVMC));
        assertEquals(BigDecimal.ZERO, creditOf(entry, PgcNirfChart.MERCADORIAS));
    }

    @Test // CT-26
    void recebimento_soMoveOSaldo_naoVoltaALancarOProveito() {
        service.onPaymentReceived(new PaymentReceivedEvent(
                COMPANY_ID, 55L, "RC-2026/3", HOJE, new BigDecimal("232.00"), true));

        JournalEntry entry = captureEntry();
        assertEquals(new BigDecimal("232.00"), debitOf(entry, PgcNirfChart.CAIXA));
        assertEquals(new BigDecimal("232.00"), creditOf(entry, PgcNirfChart.CLIENTES));
        assertEquals(BigDecimal.ZERO, creditOf(entry, PgcNirfChart.VENDAS),
                "a venda já foi lançada na emissão da fatura — lançá-la aqui contá-la-ia duas vezes");
        assertEquals(JournalSource.RECEIPT, entry.getSource());
    }

    @Test // CT-27
    void semPlanoDeContas_naoLancaEnaoEstoira() {
        when(chartOfAccountsService.hasChart(COMPANY_ID)).thenReturn(false);

        assertDoesNotThrow(() -> service.onSaleRegistered(venda("200.00", "32.00", "232.00", "0", "0", false)));
        verify(journalService, never()).save(any(), any());
    }

    @Test // CT-28
    void documentoJaLancado_naoLancaOutraVez() {
        when(journalService.findByDocument(COMPANY_ID, JournalSource.INVOICE, 10L))
                .thenReturn(Optional.of(new JournalEntry()));

        service.onSaleRegistered(venda("200.00", "32.00", "232.00", "0", "0", false));

        verify(journalService, never()).save(any(), any());
    }

    @Test // CT-29
    void vendaDeValorZeroNaoGeraLancamento() {
        service.onSaleRegistered(venda("0", "0", "0", "0", "0", false));

        verify(journalService, never()).save(any(), any());
    }
}
