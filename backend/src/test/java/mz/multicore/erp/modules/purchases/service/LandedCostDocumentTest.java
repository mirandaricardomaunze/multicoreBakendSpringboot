package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostDocumentDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.LandedCostDocument;
import mz.multicore.erp.modules.purchases.model.LandedCostType;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseDocumentKind;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.LandedCostDocumentRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LC-01 and LC-02 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1): the "Custos de Importação" document is
 * numbered {@code CI-yyyy/n} per company and links to one or more received ({@code COMPLETED}) purchases of the
 * active company; a purchase may receive several CI documents. Each cost line (type, supplier, currency and rate,
 * value, VAT) creates a debt to its own supplier, listed in accounts payable and paid with the normal supplier
 * payment action.
 */
class LandedCostDocumentTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTHER_COMPANY_ID = 2L;
    private static final Long CARRIER_ID = 30L;
    private static final Long BROKER_ID = 31L;
    private static final Long CUSTOMS_ID = 32L;
    private static final Long VAT16_ID = 7L;
    private static final Long ACCOUNT_ID = 5L;

    private final LandedCostDocumentRepository documents = mock(LandedCostDocumentRepository.class);
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final SupplierLookup suppliers = mock(SupplierLookup.class);
    private final TaxRateLookup taxRates = mock(TaxRateLookup.class);
    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
    private final PurchaseCurrencyResolver currencyResolver = new PurchaseCurrencyResolver(exchangeRates);
    private final LandedCostDocumentService service = new LandedCostDocumentService(documents, purchases, numbers,
            new LandedCostChargeResolver(suppliers, currencyResolver, taxRates));

    private final List<Purchase> savedPayables = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("comprador", "MANAGER");
        when(documents.save(any(LandedCostDocument.class))).thenAnswer(invocation -> {
            LandedCostDocument document = invocation.getArgument(0);
            document.setId(50L);
            return document;
        });
        when(purchases.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase payable = invocation.getArgument(0);
            if (payable.getId() == null) {
                payable.setId(900L + savedPayables.size());
                savedPayables.add(payable);
            }
            return payable;
        });
        stubSupplier(CARRIER_ID, "Transportes Maputo");
        stubSupplier(BROKER_ID, "Despachante Lda");
        stubSupplier(CUSTOMS_ID, "Autoridade Tributária");
        TaxRate vat16 = new TaxRate();
        vat16.setId(VAT16_ID);
        vat16.setRate(new BigDecimal("0.1600"));
        when(taxRates.requireForCompany(VAT16_ID, COMPANY_ID)).thenReturn(vat16);
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class), any()))
                .thenAnswer(call -> call.getArgument(3) == null ? new BigDecimal("63.9000") : call.getArgument(3));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void lc01_serieCi() {
        assertEquals("CI", DocumentSeries.LANDED_COST);
        assertThat(DocumentSeries.ALL).contains(DocumentSeries.LANDED_COST);
    }

    @Test
    void lc01_numeraNaSerieCiELigaAsComprasRecebidasDaEmpresa() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        stubPurchase(11L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/5");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1");

        LandedCostDocumentDTO dto = service.register(request(
                List.of(10L, 11L, 10L), LandedCostAllocationMethod.PESO, LocalDate.of(2026, 11, 20)));

        assertEquals(50L, dto.id());
        assertEquals("CI-2026/1", dto.documentNumber());
        assertEquals(LocalDate.of(2026, 11, 20), dto.documentDate());
        assertEquals(LandedCostAllocationMethod.PESO, dto.allocationMethod());
        assertThat(dto.purchaseIds()).containsExactly(10L, 11L);
        assertThat(dto.purchaseNumbers()).containsExactly("V/FT-2026/4", "V/FT-2026/5");
    }

    @Test
    void lc01_metodoPorOmissaoValorEDataDeHoje() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1");

        LandedCostDocumentDTO dto = service.register(request(List.of(10L), null, null));

        assertEquals(LandedCostAllocationMethod.VALOR, dto.allocationMethod());
        assertEquals(LocalDate.now(), dto.documentDate());
    }

    @Test
    void lc01_umaCompraPodeReceberVariosDocumentosCi() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1", "CI-2026/2");

        LandedCostDocumentDTO first = service.register(request(List.of(10L), null, null));
        LandedCostDocumentDTO second = service.register(request(List.of(10L), null, null));

        assertEquals("CI-2026/1", first.documentNumber());
        assertEquals("CI-2026/2", second.documentNumber());
        assertThat(second.purchaseIds()).containsExactly(10L);
    }

    @Test
    void lc01_semComprasEhErro() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(request(List.of(), null, null)));
        assertThat(error.getMessage()).contains("pelo menos uma compra");
        verify(numbers, never()).next(any());
    }

    @Test
    void lc01_compraDeOutraEmpresaNaoEhEncontrada() {
        stubPurchase(10L, OTHER_COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(request(List.of(10L), null, null)));
        assertThat(error.getMessage()).contains("não encontrada");
        verify(documents, never()).save(any());
    }

    @Test
    void lc01_compraInexistenteNaoEhEncontrada() {
        when(purchases.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.register(request(List.of(99L), null, null)));
        verify(documents, never()).save(any());
    }

    @Test
    void lc01_compraAnuladaEhRecusada() {
        stubPurchase(10L, COMPANY_ID, "CANCELLED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(request(List.of(10L), null, null)));
        assertThat(error.getMessage()).contains("V/FT-2026/4").contains("recebida");
        verify(numbers, never()).next(any());
    }

    @Test
    void lc01_saldoInicialFornecedorNaoEhCompraRecebida() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.OPENING_BALANCE, "SIF-2026/1");

        assertThrows(BusinessRuleException.class,
                () -> service.register(request(List.of(10L), null, null)));
        verify(documents, never()).save(any());
    }

    @Test
    void lc01_perfilSemPermissaoDeComprasEhRecusado() {
        CurrentUserContext.setCurrentUser("caixa", "SELLER");
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        assertThrows(RuntimeException.class,
                () -> service.register(request(List.of(10L), null, null)));
        verify(documents, never()).save(any());
    }

    @Test
    void lc02_cadaLinhaGeraDividaAoSeuFornecedor() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/3");

        LandedCostDocumentDTO dto = service.register(new CreateLandedCostRequest(List.of(10L), null,
                LocalDate.of(2026, 11, 20), List.of(
                        new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, "USD",
                                new BigDecimal("64.0000"), new BigDecimal("100.00"), null),
                        new LandedCostChargeRequest(LandedCostType.DESPACHANTE, BROKER_ID, null, null,
                                new BigDecimal("5000.00"), VAT16_ID),
                        new LandedCostChargeRequest(LandedCostType.IVA_ADUANEIRO, CUSTOMS_ID, null, null,
                                new BigDecimal("1200.00"), null))));

        assertThat(savedPayables).hasSize(3);
        assertThat(savedPayables).allSatisfy(payable -> {
            assertEquals(PurchaseDocumentKind.LANDED_COST, payable.getDocumentKind());
            assertEquals(COMPANY_ID, payable.getCompany().getId());
            assertThat(payable.getWarehouse()).as("não mexe no stock").isNull();
            assertThat(payable.getLines()).isEmpty();
            assertEquals(LocalDate.of(2026, 11, 20).atStartOfDay(), payable.getPurchaseDate());
            assertEquals("COMPLETED", payable.getStatus());
            assertEquals("comprador", payable.getCreatedBy());
        });

        Purchase freight = savedPayables.get(0);
        assertEquals("CI-2026/3-1", freight.getPurchaseNumber());
        assertEquals("Transportes Maputo", freight.getSupplier().getName());
        assertEquals("USD", freight.getCurrency());
        assertThat(freight.getExchangeRate()).isEqualByComparingTo("64.0000");
        assertThat(freight.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(freight.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(freight.getBaseOutstanding()).as("dívida em moeda base").isEqualByComparingTo("6400.00");
        assertThat(freight.getDescription()).contains("CI-2026/3").contains("Frete");

        Purchase broker = savedPayables.get(1);
        assertEquals("CI-2026/3-2", broker.getPurchaseNumber());
        assertEquals("MZN", broker.getCurrency());
        assertThat(broker.getTotalAmount()).as("serviço com IVA 16%").isEqualByComparingTo("5800.00");
        assertThat(broker.getTaxAmount()).isEqualByComparingTo("800.00");

        Purchase customsVat = savedPayables.get(2);
        assertEquals("Autoridade Tributária", customsVat.getSupplier().getName());
        assertThat(customsVat.getTotalAmount()).isEqualByComparingTo("1200.00");
        assertThat(customsVat.getTaxAmount()).as("IVA aduaneiro é todo imposto").isEqualByComparingTo("1200.00");

        assertThat(dto.charges()).extracting(LandedCostChargeDTO::payableNumber)
                .containsExactly("CI-2026/3-1", "CI-2026/3-2", "CI-2026/3-3");
        LandedCostChargeDTO freightLine = dto.charges().get(0);
        assertEquals(LandedCostType.FRETE, freightLine.type());
        assertEquals(CARRIER_ID, freightLine.supplierId());
        assertThat(freightLine.baseCostAmount()).isEqualByComparingTo("6400.00");
        LandedCostChargeDTO brokerLine = dto.charges().get(1);
        assertThat(brokerLine.vatRatePercent()).isEqualByComparingTo("16");
        assertThat(brokerLine.totalAmount()).isEqualByComparingTo("5800.00");
        assertThat(brokerLine.baseCostAmount()).isEqualByComparingTo("5000.00");
        assertThat(brokerLine.baseVatAmount()).isEqualByComparingTo("800.00");

        ArgumentCaptor<LandedCostDocument> saved = ArgumentCaptor.forClass(LandedCostDocument.class);
        verify(documents).save(saved.capture());
        assertThat(saved.getValue().getCharges()).extracting(c -> c.getPayable().getPurchaseNumber())
                .containsExactly("CI-2026/3-1", "CI-2026/3-2", "CI-2026/3-3");
    }

    @Test
    void lc02_cambioPropostoPelaTabelaQuandoNaoEscrito() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/4");

        LandedCostDocumentDTO dto = service.register(new CreateLandedCostRequest(List.of(10L), null, null, List.of(
                new LandedCostChargeRequest(LandedCostType.SEGURO, CARRIER_ID, "USD", null,
                        new BigDecimal("10.00"), null))));

        assertThat(dto.charges().get(0).exchangeRate()).isEqualByComparingTo("63.9000");
        assertThat(savedPayables.get(0).getExchangeRate()).isEqualByComparingTo("63.9000");
    }

    @Test
    void lc02_semLinhasDeCustoEhErro() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(new CreateLandedCostRequest(List.of(10L), null, null, List.of())));
        assertThat(error.getMessage()).contains("linha de custo");
        verify(numbers, never()).next(any());
        verify(purchases, never()).save(any());
    }

    @Test
    void lc02_fornecedorDeOutraEmpresaEhRecusado() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(suppliers.requireForCompany(99L, COMPANY_ID))
                .thenThrow(new BusinessRuleException("Fornecedor não encontrado."));

        assertThrows(BusinessRuleException.class, () -> service.register(charge(
                new LandedCostChargeRequest(LandedCostType.FRETE, 99L, null, null, new BigDecimal("10.00"), null))));
        verify(numbers, never()).next(any());
        verify(purchases, never()).save(any());
    }

    @Test
    void lc02_valorZeroOuSemTipoEhRecusado() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");

        assertThrows(BusinessRuleException.class, () -> service.register(charge(
                new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null, BigDecimal.ZERO, null))));
        assertThrows(BusinessRuleException.class, () -> service.register(charge(
                new LandedCostChargeRequest(null, CARRIER_ID, null, null, BigDecimal.TEN, null))));
        verify(purchases, never()).save(any());
    }

    @Test
    void lc02_taxaDeIvaTemDeSerDaEmpresaENaoSeAplicaAoIvaAduaneiro() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(taxRates.requireForCompany(8L, COMPANY_ID)).thenThrow(new BusinessRuleException("Taxa não encontrada."));

        assertThrows(BusinessRuleException.class, () -> service.register(charge(
                new LandedCostChargeRequest(LandedCostType.DESPACHANTE, BROKER_ID, null, null, BigDecimal.TEN, 8L))));
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.register(charge(
                new LandedCostChargeRequest(LandedCostType.IVA_ADUANEIRO, CUSTOMS_ID, null, null, BigDecimal.TEN,
                        VAT16_ID))));
        assertThat(error.getMessage()).contains("IVA aduaneiro");
        verify(purchases, never()).save(any());
    }

    @Test
    void lc02_dividaDeCustoDeImportacaoNaoEhCompraRecebida() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.LANDED_COST, "CI-2026/1-1");

        assertThrows(BusinessRuleException.class, () -> service.register(request(List.of(10L), null, null)));
        verify(documents, never()).save(any());
    }

    @Test
    void lc02_dividaApareceNasContasAPagarEPagaComAAccaoNormal() {
        stubPurchase(10L, COMPANY_ID, "COMPLETED", PurchaseDocumentKind.PURCHASE, "V/FT-2026/4");
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/5");
        service.register(charge(new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, "USD",
                new BigDecimal("64.0000"), new BigDecimal("100.00"), null)));
        Purchase freight = savedPayables.get(0);
        when(purchases.findByCompanyId(COMPANY_ID)).thenReturn(List.of(freight));
        when(purchases.findById(freight.getId())).thenReturn(Optional.of(freight));

        FinanceService finance = mock(FinanceService.class);
        when(finance.accountType(ACCOUNT_ID)).thenReturn(TreasuryAccountType.BANK);
        when(finance.registerTransaction(anyLong(), anyString(), any(), anyString()))
                .thenAnswer(call -> new TreasuryTransactionDTO(91L, ACCOUNT_ID, "Banco", "CREDIT",
                        call.getArgument(2), call.getArgument(3), LocalDateTime.of(2026, 11, 22, 10, 0)));
        SupplierPaymentService payments = new SupplierPaymentService(purchases, finance,
                mock(ApplicationEventPublisher.class), mock(AuditLogService.class),
                new DirectPurchaseService(null, new DirectPurchaseQueryService(purchases)), currencyResolver);

        List<PayableDTO> payables = payments.findPayablesByCompany(COMPANY_ID);
        assertThat(payables).hasSize(1);
        assertEquals("CI-2026/5-1", payables.get(0).purchaseNumber());
        assertEquals("Transportes Maputo", payables.get(0).supplierName());
        assertThat(payables.get(0).outstanding()).isEqualByComparingTo("100.00");

        payments.registerSupplierPayment(freight.getId(), new BigDecimal("100.00"), ACCOUNT_ID, null,
                new BigDecimal("65.0000"));

        assertThat(freight.getOutstanding()).isEqualByComparingTo("0");
        verify(finance).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("6500.00")),
                org.mockito.ArgumentMatchers.contains("Custos de Importação CI-2026/5-1"));
    }

    private static CreateLandedCostRequest request(List<Long> purchaseIds, LandedCostAllocationMethod method,
                                                   LocalDate date) {
        return new CreateLandedCostRequest(purchaseIds, method, date, List.of(
                new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null,
                        new BigDecimal("1000.00"), null)));
    }

    private static CreateLandedCostRequest charge(LandedCostChargeRequest line) {
        return new CreateLandedCostRequest(List.of(10L), null, null, List.of(line));
    }

    private void stubSupplier(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        when(suppliers.requireForCompany(id, COMPANY_ID)).thenReturn(supplier);
    }

    private void stubPurchase(Long id, Long companyId, String status, PurchaseDocumentKind kind, String number) {
        Company company = new Company();
        company.setId(companyId);
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setCompany(company);
        purchase.setStatus(status);
        purchase.setDocumentKind(kind);
        purchase.setPurchaseNumber(number);
        when(purchases.findById(id)).thenReturn(Optional.of(purchase));
    }
}
