package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.CreditNoteLine;
import mz.multicore.erp.modules.comercial.model.CreditNoteReason;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DebitNoteLine;
import mz.multicore.erp.modules.comercial.model.DebitNoteReason;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.ReceiptLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.service.ProductBatchLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DT-03 (SPEC §3.2): com todas as colunas das linhas escondidas, facturas, notas de crédito e de débito,
 * recibos e vendas POS continuam a mostrar emitente, adquirente (ou "Consumidor Final"), tipo/número/data,
 * total sem IVA, IVA por taxa, total com IVA, motivo de isenção e a indicação da via.
 */
class DocumentTemplateFiscalElementsTest {

    private static final DocumentColumnsDTO ALL_HIDDEN = new DocumentColumnsDTO(
            false, false, false, false, false, false, false, false, false, false, false, null);
    private static final String EXEMPTION = "Artigo 9.º do CIVA";

    private final LineRowMapper lineRowMapper = new LineRowMapper(new ProductBatchLookup(mock(ProductBatchRepository.class)));

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void facturaA4_mantemElementosFiscais_e_ivaPorTaxa() throws Exception {
        String text = extract(new InvoiceA4Renderer(lineRowMapper).render(invoice(client()), List.of("2.ª VIA — DUPLICADO")));

        assertIssuer(text);
        assertContains(text, "FATURA", "FT 2026/001", "04/10/2026", "VENCIMENTO", "Cliente Alfa Lda", "400888777",
                "Subtotal (sem IVA)", "IVA 16,0%", "Isento — " + EXEMPTION, "1.500,00", "1.660,00", "2.ª VIA — DUPLICADO");
    }

    @Test
    void notaDeCredito_comColunasEscondidas_mantemIvaPorTaxa_e_motivoDeIsencao() throws Exception {
        CreditNoteService service = mock(CreditNoteService.class);
        DocumentConfigService config = mock(DocumentConfigService.class);
        when(service.loadForPrint(100L)).thenReturn(creditNote());
        when(config.getColumns(eq(1L), any(DocumentType.class))).thenReturn(ALL_HIDDEN);

        String text = extract(new CreditNotePrintService(service, lineRowMapper, config).render(100L));

        assertIssuer(text);
        assertContains(text, "NC 2026/001", "04/10/2026", "NUIT: Consumidor Final", "Subtotal (sem IVA)",
                "IVA 16,0%", "Isento — " + EXEMPTION, "160,00", "TOTAL CREDITADO", "1.660,00",
                "1.ª VIA — ORIGINAL (Cliente)");
    }

    @Test
    void notaDeDebito_mantemIvaPorTaxa() throws Exception {
        String text = extract(new DebitNoteDocumentRenderer().render(debitNote(), List.of("1.ª VIA — ORIGINAL (Cliente)")));

        assertIssuer(text);
        assertContains(text, "ND 2026/001", "04/10/2026", "Cliente Alfa Lda", "NUIT: 400888777",
                "IVA 16,0%", "Isento de IVA", "32,00", "TOTAL DEBITADO", "282,00", "1.ª VIA — ORIGINAL (Cliente)");
    }

    @Test
    void vendaPos_semColunas_mostraConsumidorFinal_e_ivaPorTaxa() throws Exception {
        Invoice sale = invoice(null);
        sale.setSalesChannel(SalesChannel.POS);

        String text = extract(new ReceiptDocumentRenderer().renderDocument(sale, ALL_HIDDEN, List.of()));

        assertIssuer(text);
        assertContains(text, "RECIBO POS", "FT 2026/001", "04/10/2026", "Cliente: Consumidor Final",
                "NUIT Cliente: Consumidor Final", "Subtotal", "IVA 16,0%", "Isento — " + EXEMPTION,
                "1.660,00", "1.ª VIA — ORIGINAL (CLIENTE)");
    }

    @Test
    void recibo_mostraAdquirente_e_ivaPorTaxaDaFactura() throws Exception {
        Receipt receipt = new Receipt();
        receipt.setId(7L);
        receipt.setReceiptNumber("RC 2026/007");
        receipt.setCompany(company());
        receipt.setInvoice(invoice(null));
        receipt.setAmountPaid(new BigDecimal("1660.00"));
        receipt.setPaymentMethod("CASH");
        receipt.setReceiptDate(LocalDateTime.of(2026, 10, 5, 9, 0));
        ReceiptLookup lookup = mock(ReceiptLookup.class);
        when(lookup.find(7L)).thenReturn(Optional.of(receipt));

        String text = extract(new ReceiptDocumentPrintService(lookup).render(7L));

        assertIssuer(text);
        assertContains(text, "RC 2026/007", "05/10/2026", "Consumidor Final", "Subtotal (sem IVA)",
                "IVA 16,0%", "Isento — " + EXEMPTION, "TOTAL DA FATURA", "1.660,00");
    }

    private static void assertIssuer(String text) {
        assertContains(text, "Empresa Moçambique Lda", "400123456", "Av. Julius Nyerere");
    }

    private static void assertContains(String text, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(text.contains(fragment), "Elemento fiscal em falta: '" + fragment + "' em\n" + text);
        }
    }

    private static Company company() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Empresa Moçambique Lda");
        company.setTaxId("400123456");
        company.setAddress("Av. Julius Nyerere, Maputo");
        return company;
    }

    private static Client client() {
        Client client = new Client();
        client.setId(10L);
        client.setName("Cliente Alfa Lda");
        client.setTaxId("400888777");
        return client;
    }

    private static Product product(String name) {
        Product product = new Product();
        product.setName(name);
        product.setReference("REF-" + name.length());
        return product;
    }

    /** 1000 a 16% (IVA 160) + 500 isento com motivo legal: total 1660. */
    private static Invoice invoice(Client client) {
        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setInvoiceNumber("FT 2026/001");
        invoice.setCompany(company());
        invoice.setClient(client);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setCreatedAt(LocalDateTime.of(2026, 10, 4, 11, 0));
        invoice.setTotalBeforeTax(new BigDecimal("1500.00"));
        invoice.setTaxAmount(new BigDecimal("160.00"));
        invoice.setTotalAmount(new BigDecimal("1660.00"));
        List<InvoiceLine> lines = new ArrayList<>();
        lines.add(invoiceLine(invoice, "Cimento", "0.16", "10", "100.00", "1000.00", null));
        lines.add(invoiceLine(invoice, "Livro escolar", "0", "5", "100.00", "500.00", EXEMPTION));
        invoice.setLines(lines);
        return invoice;
    }

    private static InvoiceLine invoiceLine(Invoice invoice, String name, String rate, String qty, String price,
                                           String total, String exemption) {
        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProduct(product(name));
        line.setQuantity(new BigDecimal(qty));
        line.setUnitPrice(new BigDecimal(price));
        line.setTaxRate(new BigDecimal(rate));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal(total));
        line.setExemptionReason(exemption);
        return line;
    }

    private static CreditNote creditNote() {
        Client withoutTaxId = client();
        withoutTaxId.setTaxId(null);
        Invoice invoice = invoice(withoutTaxId);
        CreditNote note = new CreditNote();
        note.setId(100L);
        note.setNoteNumber("NC 2026/001");
        note.setCompany(company());
        note.setClient(withoutTaxId);
        note.setInvoice(invoice);
        note.setReason(CreditNoteReason.RETURN);
        note.setStatus(NoteStatus.APPROVED);
        note.setIssueDate(LocalDateTime.of(2026, 10, 4, 12, 0));
        note.setTotalBeforeTax(new BigDecimal("1500.00"));
        note.setTaxAmount(new BigDecimal("160.00"));
        note.setTotalAmount(new BigDecimal("1660.00"));
        List<CreditNoteLine> lines = new ArrayList<>();
        for (InvoiceLine source : invoice.getLines()) {
            CreditNoteLine line = new CreditNoteLine();
            line.setProduct(source.getProduct());
            line.setInvoiceLine(source);
            line.setQuantity(source.getQuantity());
            line.setUnitPrice(source.getUnitPrice());
            line.setTaxRate(source.getTaxRate());
            line.setDiscountPercentage(BigDecimal.ZERO);
            line.setLineTotal(source.getLineTotal());
            lines.add(line);
        }
        note.setLines(lines);
        return note;
    }

    /** 200 a 16% (IVA 32) + 50 isento: total 282. */
    private static DebitNote debitNote() {
        DebitNote note = new DebitNote();
        note.setId(200L);
        note.setNoteNumber("ND 2026/001");
        note.setCompany(company());
        note.setClient(client());
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("FT 2026/100");
        note.setInvoice(invoice);
        note.setReason(DebitNoteReason.FREIGHT);
        note.setStatus(NoteStatus.APPROVED);
        note.setIssueDate(LocalDateTime.of(2026, 10, 4, 12, 30));
        note.setTotalBeforeTax(new BigDecimal("250.00"));
        note.setTaxAmount(new BigDecimal("32.00"));
        note.setTotalAmount(new BigDecimal("282.00"));
        note.setLines(List.of(debitLine(note, "Frete", "200.00", "0.16", "232.00"),
                debitLine(note, "Taxa isenta", "50.00", "0", "50.00")));
        return note;
    }

    private static DebitNoteLine debitLine(DebitNote note, String description, String amount, String rate, String total) {
        DebitNoteLine line = new DebitNoteLine();
        line.setDebitNote(note);
        line.setDescription(description);
        line.setAmount(new BigDecimal(amount));
        line.setTaxRate(new BigDecimal(rate));
        line.setLineTotal(new BigDecimal(total));
        return line;
    }

    private static String extract(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
