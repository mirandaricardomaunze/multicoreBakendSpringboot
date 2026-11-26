package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Documentos fiscais de exemplo partilhados pelos testes do modelo de documento (DT-03, DT-04, DT-05). */
final class FiscalDocumentFixtures {

    static final String EXEMPTION = "Artigo 9.º do CIVA";

    private FiscalDocumentFixtures() {}

    static Company company() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Empresa Moçambique Lda");
        company.setTaxId("400123456");
        company.setAddress("Av. Julius Nyerere, Maputo");
        return company;
    }

    static Client client() {
        Client client = new Client();
        client.setId(10L);
        client.setName("Cliente Alfa Lda");
        client.setTaxId("400888777");
        return client;
    }

    static Product product(String name) {
        Product product = new Product();
        product.setName(name);
        product.setReference("REF-" + name.length());
        return product;
    }

    /** 1000 a 16% (IVA 160) + 500 isento com motivo legal: total 1660. */
    static Invoice invoice(Client client) {
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

    static InvoiceLine invoiceLine(Invoice invoice, String name, String rate, String qty, String price,
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

    static CreditNote creditNote() {
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
    static DebitNote debitNote() {
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

    static DebitNoteLine debitLine(DebitNote note, String description, String amount, String rate, String total) {
        DebitNoteLine line = new DebitNoteLine();
        line.setDebitNote(note);
        line.setDescription(description);
        line.setAmount(new BigDecimal(amount));
        line.setTaxRate(new BigDecimal(rate));
        line.setLineTotal(new BigDecimal(total));
        return line;
    }

    static String extract(byte[] pdf) throws Exception {
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
