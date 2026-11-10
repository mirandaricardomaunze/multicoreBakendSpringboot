package mz.multicore.erp.modules.comercial.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.accounting.service.CommercialPostingHandler;
import mz.multicore.erp.modules.accounting.service.JournalService;
import mz.multicore.erp.modules.accounting.service.PostingHelper;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.printing.InvoicePdfProbe;
import mz.multicore.erp.modules.printing.LineRowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ver docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md. */
class ComercialDocumentosHarnessTest {

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final CreditNoteRepository creditNoteRepository = mock(CreditNoteRepository.class);
    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final CreditNoteQueryService queryService = mock(CreditNoteQueryService.class);
    private final CreditNoteCreationService creationService = new CreditNoteCreationService(
            creditNoteRepository, invoiceRepository, warehouseRepository, numbers, queryService, java.util.List.of());

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("CD-01: Nota de crédito reduz a dívida e, se a cobrir toda, liquida a fatura")
    void cd01_creditNoteReducesDebt() {
        Invoice invoice = invoice(InvoiceStatus.APPROVED);

        invoice.applyNoteAdjustment(new BigDecimal("-400.00"));
        assertThat(invoice.outstandingAmount()).isEqualByComparingTo("600.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.APPROVED);

        invoice.applyNoteAdjustment(new BigDecimal("-600.00"));
        assertThat(invoice.outstandingAmount()).isEqualByComparingTo("0");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("CD-02: Nota de débito reabre a cobrança de uma fatura paga")
    void cd02_debitNoteReopensCollection() {
        Invoice invoice = invoice(InvoiceStatus.PAID);
        invoice.setAmountPaid(new BigDecimal("1000.00"));

        invoice.applyNoteAdjustment(new BigDecimal("116.00"));

        assertThat(invoice.outstandingAmount()).isEqualByComparingTo("116.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("CD-03: Nota de crédito credita o que o cliente pagou, com o desconto da fatura")
    void cd03_creditNoteHonoursDiscount() {
        Invoice invoice = invoice(InvoiceStatus.APPROVED);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(call -> call.getArgument(0));

        creationService.create(request(new CreateCreditNoteLineRequest(100L, new BigDecimal("5"))));

        ArgumentCaptor<CreditNote> saved = ArgumentCaptor.forClass(CreditNote.class);
        verify(creditNoteRepository).save(saved.capture());
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("450.00");
        assertThat(saved.getValue().getLines().get(0).getDiscountPercentage()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("CD-04: Fatura anulada não aceita nota de crédito")
    void cd04_cancelledInvoiceRefused() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice(InvoiceStatus.CANCELLED)));

        assertThatThrownBy(() -> creationService.create(request(new CreateCreditNoteLineRequest(100L, BigDecimal.ONE))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("fatura válida");
    }

    @Test
    @DisplayName("CD-05: Armazém de outra empresa é recusado na devolução")
    void cd05_foreignWarehouseRefused() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice(InvoiceStatus.APPROVED)));
        Company other = new Company();
        other.setId(2L);
        Warehouse warehouse = new Warehouse();
        warehouse.setCompany(other);
        when(warehouseRepository.findById(9L)).thenReturn(Optional.of(warehouse));

        CreateCreditNoteRequest request = new CreateCreditNoteRequest(10L, "RETURN", 9L, null,
                List.of(new CreateCreditNoteLineRequest(100L, BigDecimal.ONE)));

        assertThatThrownBy(() -> creationService.create(request))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("não pertence");
    }

    @Test
    @DisplayName("CD-06: Repetir a mesma linha não ultrapassa o que foi vendido")
    void cd06_repeatedLineCannotExceedSoldQuantity() {
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice(InvoiceStatus.APPROVED)));

        assertThatThrownBy(() -> creationService.create(request(
                new CreateCreditNoteLineRequest(100L, new BigDecimal("6")),
                new CreateCreditNoteLineRequest(100L, new BigDecimal("6")))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("excede");
    }

    @Test
    @DisplayName("CD-07: Anular uma venda estorna o lançamento, com débitos e créditos trocados")
    void cd07_cancellationReversesJournal() {
        PostingHelper helper = mock(PostingHelper.class);
        JournalService journal = mock(JournalService.class);
        when(helper.canPost(1L)).thenReturn(true);
        Account clients = new Account();
        Account sales = new Account();
        JournalEntry posted = new JournalEntry();
        posted.addLine(JournalLine.debit(clients, new BigDecimal("116.00"), "A receber"));
        posted.addLine(JournalLine.credit(sales, new BigDecimal("116.00"), "Vendas"));
        when(journal.findByDocument(1L, JournalSource.INVOICE, 10L)).thenReturn(Optional.of(posted));

        new CommercialPostingHandler(helper, journal)
                .postInvoiceCancellation(new InvoiceCancelledEvent(1L, 10L, "FT-1", LocalDate.of(2026, 11, 1)));

        ArgumentCaptor<JournalEntry> saved = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journal).save(saved.capture(), eq(1L));
        JournalEntry reversal = saved.getValue();
        assertThat(reversal.getSource()).isEqualTo(JournalSource.INVOICE_CANCELLATION);
        assertThat(reversal.getLines().get(0).getCredit()).isEqualByComparingTo("116.00");
        assertThat(reversal.getLines().get(1).getDebit()).isEqualByComparingTo("116.00");
    }

    @Test
    @DisplayName("CD-08: Fatura impressa não declara certificação nem conta bancária inventada")
    void cd08_invoiceHasNoInventedFiscalData() throws Exception {
        String text = pdfText(new InvoicePdfProbe(mock(LineRowMapper.class)).render(invoice(InvoiceStatus.APPROVED)));

        assertThat(text).doesNotContain("certificado", "Millennium", "BIMXMZMA", "APPROVED");
    }

    @Test
    @DisplayName("CD-09: Totais explicam o desconto e mostram a base de cada taxa de IVA")
    void cd09_totalsReconcile() throws Exception {
        String text = pdfText(new InvoicePdfProbe(mock(LineRowMapper.class)).render(invoice(InvoiceStatus.APPROVED)));

        assertThat(text).contains("Total ilíquido", "Desconto comercial", "Subtotal (sem IVA)");
    }

    private static CreateCreditNoteRequest request(CreateCreditNoteLineRequest... lines) {
        return new CreateCreditNoteRequest(10L, "ERROR", null, null, List.of(lines));
    }

    private static Invoice invoice(InvoiceStatus status) {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore");
        Client client = new Client();
        client.setName("Cliente");
        Product product = new Product();
        product.setName("Arroz");
        InvoiceLine line = new InvoiceLine();
        line.setId(100L);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setDiscountPercentage(new BigDecimal("10"));
        line.setTaxRate(BigDecimal.ZERO);
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setInvoiceNumber("FT-1");
        invoice.setStatus(status);
        invoice.setTotalBeforeTax(new BigDecimal("1000.00"));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setCreatedAt(LocalDateTime.of(2026, 11, 1, 8, 0));
        invoice.addLine(line);
        return invoice;
    }

    private static String pdfText(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(pdf)) {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        }
    }
}
