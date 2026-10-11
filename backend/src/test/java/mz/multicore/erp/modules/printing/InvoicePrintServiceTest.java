package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoicePrintServiceTest {

    private InvoiceRepository invoiceRepository;
    private LineRowMapper lineRowMapper;
    private DocumentConfigService documentConfigService;
    private InvoicePrintService printService;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        lineRowMapper = new LineRowMapper(mock(ProductBatchRepository.class));
        documentConfigService = mock(DocumentConfigService.class);
        printService = new InvoicePrintService(invoiceRepository, lineRowMapper, documentConfigService);

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void render_geraQuatroViasNominais() throws Exception {
        Invoice invoice = sampleInvoice();
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(documentConfigService.getColumns(eq(1L), any(DocumentType.class))).thenReturn(
                mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO.all()
        );

        byte[] pdf = printService.render(10L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(4, reader.getNumberOfPages(), "A fatura deve ter 4 vias nominais (4 páginas)");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("FT 2026/001"), "Devia conter o número da fatura: " + text);
        assertTrue(text.contains("1.ª VIA — ORIGINAL (Cliente)"), "Devia conter 1.ª Via Original: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO (Contabilidade / Fisco)"), "Devia conter 2.ª Via Duplicado: " + text);
        assertTrue(text.contains("3.ª VIA — TRIPLICADO (Arquivo / Cobrança)"), "Devia conter 3.ª Via Triplicado: " + text);
        assertTrue(text.contains("4.ª VIA — QUADRUPLICADO (Arquivo Geral)"), "Devia conter 4.ª Via Quadruplicado: " + text);
        assertTrue(text.contains("Cliente Teste Lda"), "Devia conter o cliente: " + text);
    }

    private static Invoice sampleInvoice() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Empresa Moçambique Lda");
        company.setTaxId("400123456");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(5L);
        warehouse.setName("Armazém Principal");

        Client client = new Client();
        client.setId(20L);
        client.setName("Cliente Teste Lda");
        client.setTaxId("400999888");

        Product product = new Product();
        product.setId(100L);
        product.setName("Cimento Portland 50kg");
        product.setReference("CIM-01");

        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setInvoiceNumber("FT 2026/001");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setCreatedAt(LocalDateTime.of(2026, 10, 4, 11, 0));
        invoice.setTotalBeforeTax(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("160.00"));
        invoice.setTotalAmount(new BigDecimal("1160.00"));

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("1000.00"));
        invoice.setLines(List.of(line));

        return invoice;
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
