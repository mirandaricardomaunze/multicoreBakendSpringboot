package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.SupplierPaymentRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.service.JournalService;
import mz.multicore.erp.modules.accounting.service.PgcNirfChart;
import mz.multicore.erp.modules.accounting.service.PostingHelper;
import mz.multicore.erp.modules.accounting.service.ProcurementPostingHandler;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness FX-09 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.4): o pagamento de uma compra em moeda estrangeira
 * indica o valor na moeda do documento e o câmbio do pagamento (proposto pela tabela para a data, editável); a
 * tesouraria (sempre em moeda base) sai valor × câmbio do pagamento; a dívida baixa ao câmbio do documento,
 * proporcional nos parciais, e o pagamento que liquida baixa exactamente o saldo em moeda base que restava.
 * Harness FX-10: o lançamento debita 2201 pela baixa da dívida, credita a tesouraria pelo que saiu e lança a
 * diferença a débito de 6601 (perda) ou a crédito de 7601 (ganho), sempre equilibrado.
 */
class ExchangeDifferenceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long ACCOUNT_ID = 5L;
    private static final Long PURCHASE_ID = 70L;

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final FinanceService finance = mock(FinanceService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);

    private final SupplierPaymentService payments = new SupplierPaymentService(purchaseRepository, finance, events,
            mock(AuditLogService.class),
            new DirectPurchaseService(null, new DirectPurchaseQueryService(purchaseRepository)),
            new PurchaseCurrencyResolver(exchangeRates));

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("tesoureiro", "MANAGER");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(call -> call.getArgument(0));
        when(finance.accountType(ACCOUNT_ID)).thenReturn(TreasuryAccountType.BANK);
        when(finance.registerTransaction(anyLong(), anyString(), any(), anyString()))
                .thenAnswer(call -> new TreasuryTransactionDTO(91L, ACCOUNT_ID, "Banco", "CREDIT",
                        call.getArgument(2), call.getArgument(3), LocalDateTime.of(2026, 11, 22, 10, 0)));
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class), any()))
                .thenAnswer(call -> call.getArgument(3));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void fx09_tesourariaSaiValorVezesCambioDoPagamento() {
        Purchase purchase = usdPurchase("100.00", "63.9055");

        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("40.00"), ACCOUNT_ID, "TRF 1",
                new BigDecimal("65.0000"));

        verify(finance).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("2600.00")), anyString());
        assertThat(purchase.getAmountPaid()).isEqualByComparingTo("40.00");
        SupplierPaymentRegisteredEvent event = publishedEvents().get(0);
        assertThat(event.amount()).as("sai da tesouraria").isEqualByComparingTo("2600.00");
    }

    @Test
    void fx09_dividaBaixaAoCambioDoDocumentoProporcional() {
        Purchase purchase = usdPurchase("100.00", "63.9055");
        assertThat(purchase.getBaseOutstanding()).isEqualByComparingTo("6390.55");

        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("40.00"), ACCOUNT_ID, null,
                new BigDecimal("65.0000"));

        // 40 × 63,9055 = 2556,22 baixa na dívida, não 40 × 65 = 2600
        assertThat(purchase.getBaseOutstanding()).isEqualByComparingTo("3834.33");
        assertThat(publishedEvents().get(0).supplierAmount()).as("baixa na dívida em moeda base")
                .isEqualByComparingTo("2556.22");
        assertThat(purchase.getOutstanding()).as("dívida na moeda do documento").isEqualByComparingTo("60.00");
    }

    @Test
    void fx09_cambioDoPagamentoPropostoPelaTabelaParaADataDoPagamento() {
        usdPurchase("100.00", "63.9055");
        when(exchangeRates.resolveDocumentRate(COMPANY_ID, "USD", LocalDate.now(), null))
                .thenReturn(new BigDecimal("62.5000"));

        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("40.00"), ACCOUNT_ID, null, null);

        verify(exchangeRates).resolveDocumentRate(eq(COMPANY_ID), eq("USD"), eq(LocalDate.now()), isNull());
        verify(finance).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("2500.00")), anyString());
    }

    @Test
    void fx09_semCambioParaADataExigeCambioEscritoENaoMexeEmNada() {
        Purchase purchase = usdPurchase("100.00", "63.9055");
        when(exchangeRates.resolveDocumentRate(COMPANY_ID, "USD", LocalDate.now(), null))
                .thenThrow(new BusinessRuleException("Escreva o câmbio do documento"));

        assertThatThrownBy(() -> payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("40.00"), ACCOUNT_ID,
                null, null)).isInstanceOf(BusinessRuleException.class);

        assertThat(purchase.getAmountPaid()).isEqualByComparingTo("0");
        verify(finance, never()).registerTransaction(anyLong(), anyString(), any(), anyString());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void fx09_liquidacaoBaixaExactamenteOSaldoEmMoedaBaseSemResiduos() {
        // 3 × 63,9055 = 191,7165 -> 191,72; três pagamentos de 1 USD (63,91 cada se arredondados um a um = 191,73)
        Purchase purchase = usdPurchase("3.00", "63.9055");
        for (int i = 0; i < 3; i++) {
            payments.registerSupplierPayment(PURCHASE_ID, BigDecimal.ONE, ACCOUNT_ID, null, new BigDecimal("64.0000"));
        }

        List<SupplierPaymentRegisteredEvent> paid = publishedEvents();
        BigDecimal reduced = paid.stream().map(SupplierPaymentRegisteredEvent::supplierAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(reduced).as("soma das baixas = total em moeda base").isEqualByComparingTo("191.72");
        assertThat(paid.get(2).supplierAmount()).as("a liquidação baixa o que restava").isEqualByComparingTo("63.91");
        assertThat(purchase.getBaseOutstanding()).isEqualByComparingTo("0");
        assertThat(purchase.getOutstanding()).isEqualByComparingTo("0");
    }

    @Test
    void fx09_valorAcimaDaDividaNaMoedaDoDocumentoERecusado() {
        usdPurchase("100.00", "63.9055");

        assertThatThrownBy(() -> payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("100.01"), ACCOUNT_ID,
                null, new BigDecimal("65.0000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("USD");
        verify(finance, never()).registerTransaction(anyLong(), anyString(), any(), anyString());
    }

    @Test
    void fx09_compraNaMoedaBaseIgnoraCambioEPagaComoHoje() {
        Purchase purchase = purchase("MZN", "1.0000", "200.00");

        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("150"), ACCOUNT_ID, null,
                new BigDecimal("65.0000"));

        verify(finance).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("150")), anyString());
        verify(exchangeRates, never()).resolveDocumentRate(anyLong(), anyString(), any(), any());
        SupplierPaymentRegisteredEvent event = publishedEvents().get(0);
        assertThat(event.supplierAmount()).isEqualByComparingTo(event.amount());
        assertThat(purchase.getOutstanding()).isEqualByComparingTo("50.00");
    }

    // ─── FX-10: lançamento do pagamento com diferença cambial (SPEC §3.4) ────────────────────────────────────────

    @Test
    void fx10_pagamentoMaisCaroDebitaAPerdaEm6601() {
        usdPurchase("100.00", "63.9055");
        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("40.00"), ACCOUNT_ID, null,
                new BigDecimal("65.0000"));

        JournalEntry entry = post(publishedEvents().get(0));

        // tesouraria 2600,00; dívida baixa 2556,22; perda 43,78
        assertThat(debit(entry, PgcNirfChart.FORNECEDORES)).isEqualByComparingTo("2556.22");
        assertThat(credit(entry, PgcNirfChart.BANCO)).isEqualByComparingTo("2600.00");
        assertThat(debit(entry, PgcNirfChart.EXCHANGE_LOSS)).isEqualByComparingTo("43.78");
        assertThat(lines(entry, PgcNirfChart.EXCHANGE_GAIN)).isEmpty();
        assertThat(entry.isBalanced()).isTrue();
    }

    @Test
    void fx10_pagamentoMaisBaratoCreditaOGanhoEm7601() {
        usdPurchase("100.00", "63.9055");
        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("40.00"), ACCOUNT_ID, null,
                new BigDecimal("62.5000"));

        JournalEntry entry = post(publishedEvents().get(0));

        // tesouraria 2500,00; dívida baixa 2556,22; ganho 56,22
        assertThat(debit(entry, PgcNirfChart.FORNECEDORES)).isEqualByComparingTo("2556.22");
        assertThat(credit(entry, PgcNirfChart.BANCO)).isEqualByComparingTo("2500.00");
        assertThat(credit(entry, PgcNirfChart.EXCHANGE_GAIN)).isEqualByComparingTo("56.22");
        assertThat(lines(entry, PgcNirfChart.EXCHANGE_LOSS)).isEmpty();
        assertThat(entry.isBalanced()).isTrue();
    }

    @Test
    void fx10_liquidacaoFechaOsFornecedoresSemResiduosEEquilibrada() {
        usdPurchase("3.00", "63.9055");
        for (int i = 0; i < 3; i++) {
            payments.registerSupplierPayment(PURCHASE_ID, BigDecimal.ONE, ACCOUNT_ID, null, new BigDecimal("64.0000"));
        }

        BigDecimal suppliersDebited = BigDecimal.ZERO;
        for (SupplierPaymentRegisteredEvent event : publishedEvents()) {
            JournalEntry entry = post(event);
            assertThat(entry.isBalanced()).isTrue();
            suppliersDebited = suppliersDebited.add(debit(entry, PgcNirfChart.FORNECEDORES));
        }
        assertThat(suppliersDebited).as("2201 debitado = crédito da compra em moeda base").isEqualByComparingTo("191.72");
    }

    @Test
    void fx10_semDiferencaNaoHaLinhaDeCambio() {
        purchase("MZN", "1.0000", "200.00");
        payments.registerSupplierPayment(PURCHASE_ID, new BigDecimal("150"), ACCOUNT_ID, null, null);

        JournalEntry entry = post(publishedEvents().get(0));

        assertThat(debit(entry, PgcNirfChart.FORNECEDORES)).isEqualByComparingTo("150");
        assertThat(credit(entry, PgcNirfChart.BANCO)).isEqualByComparingTo("150");
        assertThat(entry.getLines()).hasSize(2);
        assertThat(entry.isBalanced()).isTrue();
    }

    @Test
    void fx10_eventoAntigoSemValorDoFornecedorLancaComoHoje() {
        JournalEntry entry = post(new SupplierPaymentRegisteredEvent(COMPANY_ID, 92L, "CP-2026/1",
                LocalDate.of(2026, 11, 22), new BigDecimal("500.00"), true));

        assertThat(debit(entry, PgcNirfChart.FORNECEDORES)).isEqualByComparingTo("500.00");
        assertThat(credit(entry, PgcNirfChart.CAIXA)).isEqualByComparingTo("500.00");
        assertThat(entry.getLines()).hasSize(2);
    }

    private JournalEntry post(SupplierPaymentRegisteredEvent event) {
        PostingHelper helper = mock(PostingHelper.class);
        JournalService journal = mock(JournalService.class);
        when(helper.canPost(COMPANY_ID)).thenReturn(true);
        when(helper.account(eq(COMPANY_ID), anyString())).thenAnswer(call -> account(call.getArgument(1)));
        when(helper.cashAccount(eq(COMPANY_ID), anyBoolean()))
                .thenAnswer(call -> account((boolean) call.getArgument(1) ? PgcNirfChart.CAIXA : PgcNirfChart.BANCO));
        new ProcurementPostingHandler(helper, journal).postSupplierPayment(event);
        ArgumentCaptor<JournalEntry> saved = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journal).save(saved.capture(), eq(COMPANY_ID));
        return saved.getValue();
    }

    private static Account account(String code) {
        Account account = new Account();
        account.setCode(code);
        return account;
    }

    private static List<JournalLine> lines(JournalEntry entry, String code) {
        return entry.getLines().stream().filter(l -> code.equals(l.getAccount().getCode())).toList();
    }

    private static BigDecimal debit(JournalEntry entry, String code) {
        return lines(entry, code).stream().map(JournalLine::safeDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal credit(JournalEntry entry, String code) {
        return lines(entry, code).stream().map(JournalLine::safeCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SupplierPaymentRegisteredEvent> publishedEvents() {
        ArgumentCaptor<SupplierPaymentRegisteredEvent> captor =
                ArgumentCaptor.forClass(SupplierPaymentRegisteredEvent.class);
        verify(events, atLeastOnce()).publishEvent(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    private Purchase usdPurchase(String total, String rate) {
        return purchase("USD", rate, total);
    }

    private Purchase purchase(String currency, String rate, String total) {
        Company company = new Company();
        company.setId(COMPANY_ID);
        Supplier supplier = new Supplier();
        supplier.setId(7L);
        supplier.setName("Shenzhen Trading");
        supplier.setCompany(company);
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setPurchaseNumber("CP-2026/9");
        purchase.setCompany(company);
        purchase.setSupplier(supplier);
        purchase.setCurrency(currency);
        purchase.setExchangeRate(new BigDecimal(rate));
        purchase.setTaxAmount(BigDecimal.ZERO);
        purchase.applyTotal(new BigDecimal(total));
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        return purchase;
    }
}
