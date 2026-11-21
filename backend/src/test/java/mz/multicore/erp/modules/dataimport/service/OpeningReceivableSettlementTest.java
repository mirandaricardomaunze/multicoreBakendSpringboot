package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.dto.ReceiptDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceDocumentKind;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.CustomerStatementService;
import mz.multicore.erp.modules.comercial.service.InvoiceSettlementService;
import mz.multicore.erp.modules.comercial.service.ReceivablesService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.financeira.service.TreasuryAccountLookup;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-SI-05: o "Saldo Inicial" importado liquida-se com recibos normais (série RC), total
 * ou parcialmente, como uma factura: pelo recibo do ecrã comercial e pela liquidação a partir da tesouraria. O valor
 * entra na conta de tesouraria, o estado passa a parcialmente pago / pago, o recibo aparece a crédito no extracto e o
 * movimento de tesouraria identifica o Saldo Inicial pelo número SI e pelo número original (SPEC §5.2).
 */
class OpeningReceivableSettlementTest {

    private static final long COMPANY_ID = 7L;
    private static final long CLIENT_ID = 31L;
    private static final long SI_ID = 900L;
    private static final long TREASURY_ID = 20L;

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final TreasuryAccountRepository treasuryAccountRepository = mock(TreasuryAccountRepository.class);
    private final DocumentNumberService documentNumberService = mock(DocumentNumberService.class);
    private final FinanceService financeService = mock(FinanceService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final List<Receipt> savedReceipts = new ArrayList<>();

    private final Company company = new Company();
    private final Client client = new Client();
    private Invoice openingBalance;
    private ComercialService comercialService;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        company.setId(COMPANY_ID);
        client.setId(CLIENT_ID);
        client.setName("Acme Lda");
        openingBalance = openingBalance("500.00");

        TreasuryAccount account = new TreasuryAccount();
        account.setId(TREASURY_ID);
        account.setName("Caixa Geral");
        account.setCompany(company);
        when(treasuryAccountRepository.findByIdAndCompanyId(TREASURY_ID, COMPANY_ID)).thenReturn(Optional.of(account));
        when(invoiceRepository.lockByIdAndCompanyId(SI_ID, COMPANY_ID)).thenReturn(Optional.of(openingBalance));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        int[] seq = {0};
        when(documentNumberService.next(DocumentSeries.RECEIPT)).thenAnswer(inv -> "RC-2026/" + (++seq[0]));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> {
            Receipt receipt = inv.getArgument(0);
            if (receipt.getId() == null) {
                receipt.setId(50L + savedReceipts.size());
                receipt.setCreatedAt(LocalDateTime.now());
                savedReceipts.add(receipt);
            }
            return receipt;
        });
        when(financeService.accountType(TREASURY_ID)).thenReturn(TreasuryAccountType.CASH);

        comercialService = new ComercialService(clientRepository, mock(ProductRepository.class),
                mock(ProductCategoryRepository.class), invoiceRepository, mock(ApprovalService.class),
                new CompanyLookup(mock(CompanyRepository.class)), new WarehouseLookup(mock(WarehouseRepository.class)),
                mock(InventoryService.class), receiptRepository, financeService,
                new TreasuryAccountLookup(treasuryAccountRepository), mock(OrderRepository.class),
                mock(OrderLineRepository.class), mock(WalkInClientProvider.class), documentNumberService,
                mock(AuditLogService.class), new TaxRateLookup(mock(TaxRateRepository.class)),
                mock(ReceivablesService.class), eventPublisher);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    /** Saldo Inicial como o {@code OpeningReceivableCommandService} o grava. */
    private Invoice openingBalance(String amount) {
        Invoice invoice = new Invoice();
        invoice.setId(SI_ID);
        invoice.setDocumentKind(InvoiceDocumentKind.OPENING_BALANCE);
        invoice.setInvoiceNumber("SI-2026/1");
        invoice.setOriginalNumber("FT 2025/118");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setDocumentDate(LocalDate.of(2025, 10, 15));
        invoice.assignDueDate(LocalDate.of(2025, 10, 15), LocalDate.of(2025, 11, 14));
        invoice.setTotalBeforeTax(new BigDecimal(amount));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setCreatedAt(LocalDateTime.now().minusDays(1));
        return invoice;
    }

    @Test
    void partialReceiptLeavesTheOpeningBalancePartiallyPaidWithTheRestStillOwed() {
        ReceiptDTO receipt = comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("200.00"));

        assertThat(receipt.receiptNumber()).isEqualTo("RC-2026/1");
        assertThat(receipt.invoiceNumber()).isEqualTo("SI-2026/1");
        assertThat(openingBalance.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(openingBalance.getAmountPaid()).isEqualByComparingTo("200.00");
        assertThat(openingBalance.outstandingAmount()).isEqualByComparingTo("300.00");
        verify(financeService).registerTransaction(eq(TREASURY_ID), eq("DEBIT"), eq(new BigDecimal("200.00")), anyString());
    }

    @Test
    void secondReceiptSettlesTheRestAndTheOpeningBalanceIsPaid() {
        comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("200.00"));
        comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("300.00"));

        assertThat(openingBalance.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(openingBalance.outstandingAmount()).isEqualByComparingTo("0");
        assertThat(savedReceipts).extracting(Receipt::getReceiptNumber).containsExactly("RC-2026/1", "RC-2026/2");
    }

    @Test
    void receiptAboveWhatIsStillOwedIsRefused() {
        assertThatThrownBy(() -> comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("500.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("excede o saldo em dívida");
        verify(receiptRepository, never()).save(any());
        verify(financeService, never()).registerTransaction(any(), any(), any(), any());
    }

    @Test
    void treasuryMovementNamesTheOpeningBalanceAndItsOriginalNumber() {
        comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("200.00"));

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(financeService).registerTransaction(eq(TREASURY_ID), eq("DEBIT"), any(), description.capture());
        assertThat(description.getValue())
                .contains("Saldo Inicial SI-2026/1")
                .contains("FT 2025/118")
                .contains("RC-2026/1")
                .doesNotContain("Fatura");
    }

    @Test
    void settlingFromTheTreasuryWithoutAmountReceivesEverythingStillOwed() {
        comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("120.00"));
        InvoiceSettlementService settlement = new InvoiceSettlementService(invoiceRepository, comercialService, financeService);

        ReceiptDTO receipt = settlement.settle(SI_ID, TREASURY_ID, null);

        assertThat(receipt.amountPaid()).isEqualByComparingTo("380.00");
        assertThat(receipt.paymentMethod()).isEqualTo("CASH");
        assertThat(openingBalance.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void cancellingAReceiptPutsTheAmountBackOnTheOpeningBalance() {
        comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("200.00"));
        Receipt first = savedReceipts.get(0);
        when(receiptRepository.lockByIdAndCompanyId(first.getId(), COMPANY_ID)).thenReturn(Optional.of(first));

        comercialService.cancelReceipt(first.getId(), "Cheque devolvido");

        assertThat(openingBalance.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
        assertThat(openingBalance.outstandingAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void receiptIsACreditAgainstTheOpeningBalanceInTheStatement() {
        comercialService.createReceipt(SI_ID, TREASURY_ID, "CASH", new BigDecimal("200.00"));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(openingBalance));
        when(receiptRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.copyOf(savedReceipts));
        CreditNoteRepository creditNotes = mock(CreditNoteRepository.class);
        DebitNoteRepository debitNotes = mock(DebitNoteRepository.class);
        when(creditNotes.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(debitNotes.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        CustomerStatementDTO statement = new CustomerStatementService(clientRepository, invoiceRepository,
                receiptRepository, creditNotes, debitNotes)
                .getStatement(CLIENT_ID, LocalDate.of(2025, 10, 1), LocalDate.now());

        assertThat(statement.lines()).extracting(CustomerStatementLineDTO::documentType).containsExactly("SI", "RC");
        CustomerStatementLineDTO receiptLine = statement.lines().get(1);
        assertThat(receiptLine.documentNumber()).isEqualTo("RC-2026/1");
        assertThat(receiptLine.reference()).isEqualTo("SI-2026/1");
        assertThat(receiptLine.credit()).isEqualByComparingTo("200.00");
        assertThat(statement.closingBalance()).isEqualByComparingTo("300.00");
    }
}
