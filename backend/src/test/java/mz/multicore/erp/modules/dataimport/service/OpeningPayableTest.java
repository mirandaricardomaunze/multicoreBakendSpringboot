package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.events.SupplierPaymentRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;
import mz.multicore.erp.modules.fiscal.service.FiscalSummaryService;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierReturnRepository;
import mz.multicore.erp.modules.purchases.service.DirectPurchaseQueryService;
import mz.multicore.erp.modules.purchases.service.DirectPurchaseService;
import mz.multicore.erp.modules.purchases.service.SupplierPaymentService;
import org.springframework.context.ApplicationEventPublisher;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.numbering.model.DocumentSequence;
import mz.multicore.erp.modules.numbering.repository.DocumentSequenceRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateOpeningPayableRequest;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseDocumentKind;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.service.OpeningPayableCommandService;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-SIF-01 e IMP-SIF-02: cada dívida a fornecedor importada é um documento "Saldo Inicial Fornecedor"
 * numerado na série própria {@code SIF-aaaa/n} da empresa activa, com as mesmas regras do SI (SPEC §5.3 → §5.2):
 * chave fornecedor + {@code numero_original} ({@code JÁ EXISTE} / repetida no ficheiro → {@code ERRO}), valor &gt; 0,
 * vencimento ≥ data, data não futura, fornecedor existente na empresa. O SIF aparece nas contas a pagar e paga-se com
 * o pagamento normal a fornecedor, total ou parcialmente; não é documento fiscal (fora do IVA dedutível).
 */
class OpeningPayableTest {

    private static final String HEADER =
            "nuit_fornecedor;numero_original;data;vencimento;valor_em_divida;descricao";
    private static final long COMPANY_ID = 7L;
    private static final int YEAR = LocalDate.now().getYear();

    private final SupplierLookup supplierLookup = mock(SupplierLookup.class);
    private final OpeningPayableImportService service = new OpeningPayableImportService(supplierLookup);

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final CompanyLookup companyLookup = mock(CompanyLookup.class);
    private final DocumentSequenceRepository sequenceRepository = mock(DocumentSequenceRepository.class);
    private final OpeningPayableCommandService commandService = new OpeningPayableCommandService(
            purchaseRepository, supplierLookup, companyLookup, new DocumentNumberService(sequenceRepository));
    private final OpeningPayableWriter writer = new OpeningPayableWriter(commandService);

    private final Supplier grossista = supplier(41L, "400111222", "Grossista Lda");
    private final Supplier fabrica = supplier(42L, "400333444", "Fábrica Central");
    private final Company company = new Company();

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        company.setId(COMPANY_ID);
        when(supplierLookup.findForCompanyByTaxId(anyString(), eq(COMPANY_ID))).thenReturn(Optional.empty());
        when(supplierLookup.findForCompanyByTaxId(eq("400111222"), eq(COMPANY_ID))).thenReturn(Optional.of(grossista));
        when(supplierLookup.findForCompanyByTaxId(eq("400333444"), eq(COMPANY_ID))).thenReturn(Optional.of(fabrica));
        when(supplierLookup.requireForCompany(eq(41L), eq(COMPANY_ID))).thenReturn(grossista);
        when(supplierLookup.requireForCompany(eq(42L), eq(COMPANY_ID))).thenReturn(fabrica);
        when(companyLookup.require(COMPANY_ID)).thenReturn(company);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentSequence sequence = new DocumentSequence();
        sequence.setCompanyId(COMPANY_ID);
        sequence.setSeries(DocumentSeries.OPENING_PAYABLE);
        sequence.setYear(YEAR);
        sequence.setLastNumber(0);
        when(sequenceRepository.lockByCompanyAndSeriesAndYear(COMPANY_ID, DocumentSeries.OPENING_PAYABLE, YEAR))
                .thenReturn(Optional.of(sequence));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static Supplier supplier(Long id, String taxId, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setTaxId(taxId);
        supplier.setName(name);
        return supplier;
    }

    private static byte[] file(String... lines) {
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    private ImportPreviewDTO preview(String... lines) {
        return service.preview(file(lines));
    }

    private ImportRowDTO singleRow(String line) {
        ImportPreviewDTO preview = preview(HEADER, line);
        assertThat(preview.rows()).hasSize(1);
        return preview.rows().get(0);
    }

    // ---- numeração e chave ----

    @Test
    void seriesIsSifAndCreatedInAdvanceWithTheOthers() {
        assertThat(DocumentSeries.OPENING_PAYABLE).isEqualTo("SIF");
        assertThat(DocumentSeries.ALL).contains(DocumentSeries.OPENING_PAYABLE);
    }

    @Test
    void newDebtIsNovoAndCarriesSupplierOriginalNumberDatesAmountAndDescription() {
        ImportPlan<CreateOpeningPayableRequest> plan = service.plan(file(HEADER,
                "400111222;FC 2025/77;10/10/2025;2025-11-09;8.300,40;Mercadoria de Outubro"));

        assertThat(plan.lines()).hasSize(1);
        assertThat(plan.lines().get(0).row().status()).isEqualTo(ImportRowStatus.NOVO);
        CreateOpeningPayableRequest request = plan.lines().get(0).item();
        assertThat(request.supplierId()).isEqualTo(41L);
        assertThat(request.originalNumber()).isEqualTo("FC 2025/77");
        assertThat(request.documentDate()).isEqualTo(LocalDate.of(2025, 10, 10));
        assertThat(request.dueDate()).isEqualTo(LocalDate.of(2025, 11, 9));
        assertThat(request.amount()).isEqualByComparingTo("8300.40");
        assertThat(request.description()).isEqualTo("Mercadoria de Outubro");
    }

    @Test
    void alreadyImportedSupplierAndOriginalNumberIsJaExisteAndAnotherSupplierIsADifferentKey() {
        when(supplierLookup.existsOpeningPayable(COMPANY_ID, 41L, "FC 2025/77")).thenReturn(true);

        ImportPreviewDTO preview = preview(HEADER,
                "400111222;FC 2025/77;10/10/2025;09/11/2025;1000;",
                "400333444;FC 2025/77;10/10/2025;09/11/2025;500;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.JA_EXISTE, ImportRowStatus.NOVO);
    }

    @Test
    void sameSupplierAndOriginalNumberTwiceInTheFileIsErrorInBoth() {
        ImportPreviewDTO preview = preview(HEADER,
                "400111222;FC 2025/77;10/10/2025;09/11/2025;1000;",
                "400333444;FC 2025/90;10/10/2025;09/11/2025;500;",
                " 400111222 ; FC 2025/77 ;20/10/2025;20/11/2025;300;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.NOVO, ImportRowStatus.ERRO);
        ImportRowDTO first = preview.rows().get(0);
        assertThat(first.column()).isEqualTo("numero_original");
        assertThat(first.message()).contains("repetid").contains("2, 4");
    }

    @Test
    void writesOneSifDocumentPerNewDebtNumberedPerCompanyWithoutLinesTaxOrWarehouse() {
        when(supplierLookup.existsOpeningPayable(COMPANY_ID, 41L, "FC 2025/50")).thenReturn(true);

        ImportPreviewDTO written = writer.write(service.plan(file(HEADER,
                "400111222;FC 2025/50;01/09/2025;01/10/2025;999;",
                "400111222;FC 2025/77;10/10/2025;09/11/2025;8.300,40;Mercadoria de Outubro",
                "400333444;F-12;03/11/2025;03/12/2025;450,00;")));

        assertThat(written.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.JA_EXISTE, ImportRowStatus.NOVO, ImportRowStatus.NOVO);
        ArgumentCaptor<Purchase> saved = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(saved.capture());
        List<Purchase> documents = saved.getAllValues();

        Purchase first = documents.get(0);
        assertThat(first.getPurchaseNumber()).isEqualTo("SIF-" + YEAR + "/1");
        assertThat(first.getDocumentKind()).isEqualTo(PurchaseDocumentKind.OPENING_BALANCE);
        assertThat(first.getOriginalNumber()).isEqualTo("FC 2025/77");
        assertThat(first.getSupplier()).isSameAs(grossista);
        assertThat(first.getCompany()).isSameAs(company);
        assertThat(first.getPurchaseDate().toLocalDate()).isEqualTo(LocalDate.of(2025, 10, 10));
        assertThat(first.getDueDate()).isEqualTo(LocalDate.of(2025, 11, 9));
        assertThat(first.getTotalAmount()).isEqualByComparingTo("8300.40");
        assertThat(first.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(first.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(first.getOutstanding()).isEqualByComparingTo("8300.40");
        assertThat(first.getLines()).isEmpty();
        assertThat(first.getWarehouse()).isNull();
        assertThat(first.getStatus()).isEqualTo("COMPLETED");
        assertThat(first.getDescription()).isEqualTo("Mercadoria de Outubro");

        assertThat(documents.get(1).getPurchaseNumber()).isEqualTo("SIF-" + YEAR + "/2");
        assertThat(documents.get(1).getOriginalNumber()).isEqualTo("F-12");
        assertThat(documents.get(1).getSupplier()).isSameAs(fabrica);
    }

    @Test
    void ordinaryPurchasesKeepThePurchaseKind() {
        Purchase purchase = new Purchase();

        assertThat(purchase.getDocumentKind()).isEqualTo(PurchaseDocumentKind.PURCHASE);
        assertThat(purchase.getOriginalNumber()).isNull();
    }

    @Test
    void fileWithErrorsWritesNothing() {
        ImportPlan<CreateOpeningPayableRequest> plan = service.plan(file(HEADER,
                "400111222;FC 1;10/10/2025;09/11/2025;1000;",
                "400333444;FC 2;10/10/2025;09/11/2025;0;"));

        assertThatThrownBy(() -> writer.write(plan)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nada foi gravado");
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void commandRefusesAnAlreadyImportedKeyEvenIfThePreviewMissedIt() {
        when(purchaseRepository.existsByCompanyIdAndSupplierIdAndOriginalNumber(COMPANY_ID, 41L, "FC 2025/77"))
                .thenReturn(true);

        assertThatThrownBy(() -> commandService.create(new CreateOpeningPayableRequest(41L, "FC 2025/77",
                LocalDate.of(2025, 10, 10), LocalDate.of(2025, 11, 9), new BigDecimal("10"), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("FC 2025/77");
        verify(purchaseRepository, never()).save(any());
    }

    // ---- valores e datas ----

    @Test
    void zeroOrNegativeAmountIsErrorOnTheAmountColumn() {
        for (String amount : List.of("0", "0,00", "-150")) {
            ImportRowDTO row = singleRow("400111222;FC 1;10/10/2025;09/11/2025;" + amount + ";");

            assertThat(row.status()).as(amount).isEqualTo(ImportRowStatus.ERRO);
            assertThat(row.column()).isEqualTo("valor_em_divida");
            assertThat(row.message()).contains("maior que zero");
        }
    }

    @Test
    void dueDateBeforeTheDocumentDateIsErrorAndOnTheDateIsAccepted() {
        ImportRowDTO before = singleRow("400111222;FC 1;10/10/2025;09/10/2025;1000;");
        assertThat(before.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(before.column()).isEqualTo("vencimento");
        assertThat(before.message()).contains("anterior");

        assertThat(singleRow("400111222;FC 1;10/10/2025;10/10/2025;1000;").status())
                .isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void futureDocumentDateIsErrorAndTodayIsAccepted() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ImportRowDTO future = singleRow("400111222;FC 1;" + tomorrow + ";" + tomorrow.plusDays(30) + ";1000;");
        assertThat(future.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(future.column()).isEqualTo("data");
        assertThat(future.message()).contains("futura");

        LocalDate today = LocalDate.now();
        assertThat(singleRow("400111222;FC 1;" + today + ";" + today.plusDays(30) + ";1000;").status())
                .isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void missingOrMalformedValuesAreErrorsOfTheirColumn() {
        assertThat(singleRow(";FC 1;10/10/2025;09/11/2025;1000;").column()).isEqualTo("nuit_fornecedor");
        assertThat(singleRow("400111222;;10/10/2025;09/11/2025;1000;").column()).isEqualTo("numero_original");
        assertThat(singleRow("400111222;FC 1;31-12-2025;09/11/2025;1000;").column()).isEqualTo("data");
        assertThat(singleRow("400111222;FC 1;10/10/2025;;1000;").column()).isEqualTo("vencimento");
        assertThat(singleRow("400111222;FC 1;10/10/2025;09/11/2025;abc;").column()).isEqualTo("valor_em_divida");
    }

    @Test
    void unknownSupplierIsErrorAndIsNeverKeyed() {
        ImportRowDTO row = singleRow("499999999;FC 1;10/10/2025;09/11/2025;1000;");

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo("nuit_fornecedor");
        assertThat(row.message()).contains("importe primeiro os fornecedores");
        verify(supplierLookup, never()).existsOpeningPayable(anyLong(), anyLong(), anyString());
    }

    @Test
    void commandRefusesNonPositiveAmountFutureDateAndDueDateBeforeTheDate() {
        LocalDate date = LocalDate.of(2025, 10, 10);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> commandService.create(new CreateOpeningPayableRequest(41L, "FC 1",
                date, date.plusDays(30), BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> commandService.create(new CreateOpeningPayableRequest(41L, "FC 1",
                tomorrow, tomorrow.plusDays(30), BigDecimal.TEN, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("futura");
        assertThatThrownBy(() -> commandService.create(new CreateOpeningPayableRequest(41L, "FC 1",
                date, date.minusDays(1), BigDecimal.TEN, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("anterior");
        verify(purchaseRepository, never()).save(any());
    }

    // ---- IMP-SIF-02: contas a pagar e pagamento a fornecedor ----

    private static final long ACCOUNT_ID = 3L;

    private final FinanceService financeService = mock(FinanceService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final SupplierPaymentService paymentService = new SupplierPaymentService(purchaseRepository,
            financeService, eventPublisher, mock(AuditLogService.class),
            new DirectPurchaseService(null, new DirectPurchaseQueryService(purchaseRepository)));

    private Purchase openingPayable(long id, String number, String originalNumber, String amount) {
        Purchase document = new Purchase();
        document.setId(id);
        document.setDocumentKind(PurchaseDocumentKind.OPENING_BALANCE);
        document.setCompany(company);
        document.setSupplier(grossista);
        document.setPurchaseNumber(number);
        document.setOriginalNumber(originalNumber);
        document.setPurchaseDate(LocalDate.of(2025, 10, 10).atStartOfDay());
        document.setDueDate(LocalDate.of(2025, 11, 9));
        document.setTotalAmount(new BigDecimal(amount));
        document.setTaxAmount(BigDecimal.ZERO);
        return document;
    }

    private void stubTreasury() {
        when(financeService.registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> new TreasuryTransactionDTO(900L, ACCOUNT_ID, "Caixa", "CREDIT",
                        invocation.getArgument(2), invocation.getArgument(3), LocalDateTime.now()));
        when(financeService.accountType(ACCOUNT_ID)).thenReturn(TreasuryAccountType.BANK);
    }

    @Test
    void openingPayableIsListedInPayablesWithItsOutstandingAndPaidOnesAreNot() {
        Purchase open = openingPayable(51L, "SIF-" + YEAR + "/1", "FC 2025/77", "8300.40");
        Purchase paid = openingPayable(52L, "SIF-" + YEAR + "/2", "FC 2025/78", "100.00");
        paid.setAmountPaid(new BigDecimal("100.00"));
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(open, paid));

        List<PayableDTO> payables = paymentService.findPayablesByCompany(COMPANY_ID);

        assertThat(payables).hasSize(1);
        PayableDTO payable = payables.get(0);
        assertThat(payable.purchaseId()).isEqualTo(51L);
        assertThat(payable.purchaseNumber()).isEqualTo("SIF-" + YEAR + "/1");
        assertThat(payable.supplierName()).isEqualTo("Grossista Lda");
        assertThat(payable.totalAmount()).isEqualByComparingTo("8300.40");
        assertThat(payable.amountPaid()).isEqualByComparingTo("0");
        assertThat(payable.outstanding()).isEqualByComparingTo("8300.40");
        assertThat(payable.purchaseDate().toLocalDate()).isEqualTo(LocalDate.of(2025, 10, 10));
        assertThat(payable.businessUnitName()).isNull();
    }

    @Test
    void partialPaymentLeavesTheRestOutstandingAndTheSecondPaymentSettlesIt() {
        Purchase document = openingPayable(51L, "SIF-" + YEAR + "/1", "FC 2025/77", "1000.00");
        when(purchaseRepository.findById(51L)).thenReturn(Optional.of(document));
        stubTreasury();

        PurchaseDTO afterPartial = paymentService.registerSupplierPayment(51L, new BigDecimal("400.00"), ACCOUNT_ID, null);
        assertThat(afterPartial.amountPaid()).isEqualByComparingTo("400.00");
        assertThat(afterPartial.warehouseId()).isNull();
        assertThat(document.getOutstanding()).isEqualByComparingTo("600.00");

        paymentService.registerSupplierPayment(51L, new BigDecimal("600.00"), ACCOUNT_ID, "TRF 12");
        assertThat(document.getOutstanding()).isEqualByComparingTo("0");
        verify(financeService, times(2)).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), any(BigDecimal.class), anyString());

        assertThatThrownBy(() -> paymentService.registerSupplierPayment(51L, BigDecimal.ONE, ACCOUNT_ID, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("totalmente paga");
    }

    @Test
    void paymentAboveTheOutstandingIsRefusedAndNothingMoves() {
        Purchase document = openingPayable(51L, "SIF-" + YEAR + "/1", "FC 2025/77", "1000.00");
        when(purchaseRepository.findById(51L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> paymentService.registerSupplierPayment(51L, new BigDecimal("1000.01"), ACCOUNT_ID, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("excede");
        assertThat(document.getAmountPaid()).isEqualByComparingTo("0");
        verifyNoInteractions(financeService);
    }

    @Test
    void treasuryMovementNamesTheOpeningPayableAndItsOriginalNumberAndTheEventCarriesTheSifNumber() {
        Purchase document = openingPayable(51L, "SIF-" + YEAR + "/1", "FC 2025/77", "1000.00");
        when(purchaseRepository.findById(51L)).thenReturn(Optional.of(document));
        stubTreasury();

        paymentService.registerSupplierPayment(51L, new BigDecimal("250.00"), ACCOUNT_ID, "TRF 12");

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(financeService).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"),
                eq(new BigDecimal("250.00")), description.capture());
        assertThat(description.getValue())
                .contains("Grossista Lda")
                .contains("Saldo Inicial Fornecedor SIF-" + YEAR + "/1 (FC 2025/77)")
                .contains("ref. TRF 12")
                .doesNotContain("Compra SIF");
        ArgumentCaptor<SupplierPaymentRegisteredEvent> event =
                ArgumentCaptor.forClass(SupplierPaymentRegisteredEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().purchaseNumber()).isEqualTo("SIF-" + YEAR + "/1");
        assertThat(event.getValue().amount()).isEqualByComparingTo("250.00");
    }

    @Test
    void ordinaryPurchasesKeepTheirPaymentLabel() {
        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber("CP-" + YEAR + "/9");

        assertThat(purchase.isPurchase()).isTrue();
        assertThat(purchase.paymentLabel()).isEqualTo("Compra CP-" + YEAR + "/9");
        assertThat(openingPayable(51L, "SIF-" + YEAR + "/1", "FC 2025/77", "1").isPurchase()).isFalse();
    }

    @Test
    void openingPayableIsNotADeductibleVatPurchase() {
        LocalDateTime june = LocalDateTime.of(2026, 6, 15, 10, 0);
        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber("CP-2026/1");
        purchase.setSupplier(fabrica);
        purchase.setPurchaseDate(june);
        purchase.setTotalAmount(new BigDecimal("116.00"));
        purchase.setTaxAmount(new BigDecimal("16.00"));
        Purchase opening = openingPayable(51L, "SIF-2026/1", "FC 2025/77", "500.00");
        opening.setPurchaseDate(june);
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(purchase, opening));
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        when(invoices.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        FiscalSummaryService vat = new FiscalSummaryService(new InvoiceLookup(invoices),
                new InvoiceNoteLookup(mock(CreditNoteRepository.class), mock(DebitNoteRepository.class)),
                new SupplierLookup(mock(SupplierRepository.class), purchaseRepository,
                        mock(PurchaseOrderRepository.class), mock(SupplierReturnRepository.class)));

        IvaSummaryDTO summary = vat.computeMonth(COMPANY_ID, 2026, 6);

        assertThat(summary.purchases()).extracting(IvaSummaryDTO.IvaLineDTO::documentNumber)
                .containsExactly("CP-2026/1");
        assertThat(summary.purchasesBase()).isEqualByComparingTo("100.00");
        assertThat(summary.inputTax()).isEqualByComparingTo("16.00");
    }
}
