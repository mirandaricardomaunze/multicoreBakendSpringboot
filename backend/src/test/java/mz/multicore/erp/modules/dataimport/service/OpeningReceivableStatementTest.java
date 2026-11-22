package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.ClientAgingDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceDocumentKind;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.comercial.service.CreditControlService;
import mz.multicore.erp.modules.comercial.service.CustomerStatementService;
import mz.multicore.erp.modules.comercial.service.ReceivablesAgingCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-SI-04: o "Saldo Inicial" importado conta como qualquer factura na conta do cliente.
 * Entra no extracto de conta corrente como débito na data da factura antiga (não na data da importação), com o número
 * original visível; na antiguidade de saldos pelo vencimento; na exposição ao limite de crédito; e na mora
 * (SPEC §5.2).
 */
class OpeningReceivableStatementTest {

    private static final long COMPANY_ID = 7L;
    private static final long CLIENT_ID = 31L;
    private static final LocalDate OLD_INVOICE_DATE = LocalDate.of(2025, 10, 15);
    private static final LocalDate OLD_DUE_DATE = LocalDate.of(2025, 11, 14);

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
    private final CreditNoteRepository creditNoteRepository = mock(CreditNoteRepository.class);
    private final DebitNoteRepository debitNoteRepository = mock(DebitNoteRepository.class);
    private final ApprovalRequestLookup approvalRequestLookup = mock(ApprovalRequestLookup.class);
    private final Client client = new Client();

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        client.setId(CLIENT_ID);
        client.setName("Acme Lda");
        client.setTaxId("400111222");
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(receiptRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(creditNoteRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(debitNoteRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(approvalRequestLookup.listForDocument(anyLong(), any(), anyLong(), any())).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    /** Saldo Inicial como o {@code OpeningReceivableCommandService} o grava, importado hoje. */
    private Invoice openingBalance(String amount, LocalDate date, LocalDate due) {
        Invoice invoice = new Invoice();
        invoice.setId(900L);
        invoice.setDocumentKind(InvoiceDocumentKind.OPENING_BALANCE);
        invoice.setInvoiceNumber("SI-2026/1");
        invoice.setOriginalNumber("FT 2025/118");
        invoice.setClient(client);
        invoice.setDocumentDate(date);
        invoice.assignDueDate(date, due);
        invoice.setTotalBeforeTax(new BigDecimal(amount));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setCreatedAt(LocalDateTime.now());
        return invoice;
    }

    private void stubInvoices(List<Invoice> all) {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(all);
        when(invoiceRepository.findOutstandingWithClient(eq(COMPANY_ID), anyCollection())).thenReturn(all);
    }

    private CustomerStatementService statementService() {
        return new CustomerStatementService(clientRepository, invoiceRepository, receiptRepository,
                creditNoteRepository, debitNoteRepository);
    }

    @Test
    void openingBalanceIsADebitOnTheOldInvoiceDateWithItsOriginalNumber() {
        stubInvoices(List.of(openingBalance("500.00", OLD_INVOICE_DATE, OLD_DUE_DATE)));

        CustomerStatementDTO statement = statementService()
                .getStatement(CLIENT_ID, LocalDate.of(2025, 10, 1), LocalDate.now());

        assertThat(statement.lines()).hasSize(1);
        CustomerStatementLineDTO line = statement.lines().get(0);
        assertThat(line.date()).isEqualTo(OLD_INVOICE_DATE);
        assertThat(line.documentType()).isEqualTo("SI");
        assertThat(line.documentNumber()).isEqualTo("SI-2026/1");
        assertThat(line.description()).contains("Saldo Inicial").contains("FT 2025/118");
        assertThat(line.reference()).isEqualTo("FT 2025/118");
        assertThat(line.debit()).isEqualByComparingTo("500.00");
        assertThat(line.credit()).isEqualByComparingTo("0");
        assertThat(statement.closingBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void openingBalanceDatedBeforeThePeriodGoesIntoTheBalanceBroughtForward() {
        stubInvoices(List.of(openingBalance("500.00", OLD_INVOICE_DATE, OLD_DUE_DATE)));

        CustomerStatementDTO statement = statementService()
                .getStatement(CLIENT_ID, LocalDate.of(2026, 1, 1), LocalDate.now());

        assertThat(statement.openingBalance()).isEqualByComparingTo("500.00");
        assertThat(statement.lines()).isEmpty();
        assertThat(statement.closingBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void overdueOpeningBalanceIsInTheStatementOverdueAmount() {
        stubInvoices(List.of(openingBalance("500.00", OLD_INVOICE_DATE, OLD_DUE_DATE)));

        CustomerStatementDTO statement = statementService()
                .getStatement(CLIENT_ID, LocalDate.of(2025, 10, 1), LocalDate.now());

        assertThat(statement.overdueAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void openingBalanceIsAgedByItsDueDate() {
        stubInvoices(List.of(openingBalance("500.00", OLD_INVOICE_DATE, OLD_DUE_DATE)));
        ReceivablesAgingCalculator aging = new ReceivablesAgingCalculator(invoiceRepository);

        AgingSummaryDTO summary = aging.getAging(LocalDate.of(2026, 1, 20));

        assertThat(summary.total()).isEqualByComparingTo("500.00");
        assertThat(summary.overdueTotal()).isEqualByComparingTo("500.00");
        assertThat(summary.clients()).hasSize(1);
        ClientAgingDTO row = summary.clients().get(0);
        assertThat(row.de61a90()).isEqualByComparingTo("500.00");
        assertThat(row.oldestDueDate()).isEqualTo(OLD_DUE_DATE);
        assertThat(row.maxDaysOverdue()).isEqualTo(67);
        assertThat(summary.buckets())
                .filteredOn(b -> b.bucket() == AgingBucket.DE_61_A_90)
                .singleElement()
                .satisfies(b -> assertThat(b.invoiceCount()).isEqualTo(1));
    }

    @Test
    void openingBalanceNotYetDueIsCurrentInTheAging() {
        LocalDate today = LocalDate.now();
        stubInvoices(List.of(openingBalance("500.00", today.minusDays(10), today.plusDays(20))));
        ReceivablesAgingCalculator aging = new ReceivablesAgingCalculator(invoiceRepository);

        AgingSummaryDTO summary = aging.getAging(today);

        assertThat(summary.clients().get(0).corrente()).isEqualByComparingTo("500.00");
        assertThat(summary.overdueTotal()).isEqualByComparingTo("0");
    }

    @Test
    void openingBalanceCountsTowardsTheCreditLimitExposure() {
        LocalDate today = LocalDate.now();
        stubInvoices(List.of(openingBalance("500.00", today.minusDays(5), today.plusDays(25))));
        client.setCreditLimit(new BigDecimal("600.00"));
        CreditControlService credit = new CreditControlService(invoiceRepository, clientRepository,
                approvalRequestLookup, mock(ApprovalService.class));

        assertThatThrownBy(() -> credit.assertCreditAllowed(client, new BigDecimal("200.00"), today))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Limite de crédito excedido")
                .hasMessageContaining("500");
    }

    @Test
    void overdueOpeningBalanceBlocksNewCreditForLatePayment() {
        stubInvoices(List.of(openingBalance("500.00", OLD_INVOICE_DATE, OLD_DUE_DATE)));
        CreditControlService credit = new CreditControlService(invoiceRepository, clientRepository,
                approvalRequestLookup, mock(ApprovalService.class));

        assertThatThrownBy(() -> credit.assertCreditAllowed(client, new BigDecimal("50.00"), LocalDate.of(2026, 1, 20)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mora")
                .hasMessageContaining("67 dias");
    }
}
