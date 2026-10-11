package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
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

class DeliveryGuidesPrintServiceTest {

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
    void deliveryGuide_geraQuatroViasNominais() throws Exception {
        DeliveryGuideRepository repo = mock(DeliveryGuideRepository.class);
        DocumentConfigService config = mock(DocumentConfigService.class);
        LineRowMapper mapper = new LineRowMapper(mock(ProductBatchRepository.class));

        DeliveryGuide guide = sampleDeliveryGuide();
        when(repo.findByIdWithLinesAndCompanyId(500L, 1L)).thenReturn(Optional.of(guide));
        when(config.getColumns(eq(1L), any(DocumentType.class))).thenReturn(DocumentColumnsDTO.all());

        DeliveryGuidePrintService service = new DeliveryGuidePrintService(repo, mapper, config);
        byte[] pdf = service.render(500L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(4, reader.getNumberOfPages(), "A guia de remessa deve ter 4 vias nominais (4 páginas)");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("GD 2026/050"), "Deve conter número da guia: " + text);
        assertTrue(text.contains("1.ª VIA — ORIGINAL (Destinatário / Cliente)"), "Deve conter 1.ª Via Original: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO (Trânsito e Fiscalização / Viatura)"), "Deve conter 2.ª Via Duplicado: " + text);
        assertTrue(text.contains("3.ª VIA — TRIPLICADO (Expedição / Arquivo de Origem)"), "Deve conter 3.ª Via Triplicado: " + text);
        assertTrue(text.contains("4.ª VIA — QUADRUPLICADO (Comprovativo de Recepção e Devolução)"), "Deve conter 4.ª Via Quadruplicado: " + text);
    }

    @Test
    void guideRemittance_geraQuatroViasNominais() throws Exception {
        InvoiceRepository invoiceRepo = mock(InvoiceRepository.class);
        DocumentConfigService config = mock(DocumentConfigService.class);
        LineRowMapper mapper = new LineRowMapper(mock(ProductBatchRepository.class));

        Invoice invoice = sampleInvoice();
        when(invoiceRepo.findById(10L)).thenReturn(Optional.of(invoice));
        when(config.getColumns(eq(1L), any(DocumentType.class))).thenReturn(DocumentColumnsDTO.all());

        GuideRemittancePrintService service = new GuideRemittancePrintService(invoiceRepo, mapper, config);
        byte[] pdf = service.render(10L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(4, reader.getNumberOfPages(), "A guia de remessa por fatura deve ter 4 vias nominais (4 páginas)");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("GR-FT 2026/001"), "Deve conter número da guia derivada: " + text);
        assertTrue(text.contains("1.ª VIA — ORIGINAL (Destinatário / Cliente)"), "Deve conter 1.ª Via Original: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO (Trânsito e Fiscalização / Viatura)"), "Deve conter 2.ª Via Duplicado: " + text);
        assertTrue(text.contains("3.ª VIA — TRIPLICADO (Expedição / Arquivo de Origem)"), "Deve conter 3.ª Via Triplicado: " + text);
        assertTrue(text.contains("4.ª VIA — QUADRUPLICADO (Comprovativo de Recepção e Devolução)"), "Deve conter 4.ª Via Quadruplicado: " + text);
    }

    private static Company sampleCompany() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Empresa Moçambique Lda");
        company.setTaxId("400123456");
        company.setAddress("Av. 24 de Julho, Maputo");
        return company;
    }

    private static DeliveryGuide sampleDeliveryGuide() {
        Company company = sampleCompany();
        Client client = new Client();
        client.setId(30L);
        client.setName("Cliente Beta Lda");
        client.setTaxId("400555666");
        client.setAddress("Estrada Nacional 4, Matola");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setName("Armazém Matola");

        DeliveryGuide guide = new DeliveryGuide();
        guide.setId(500L);
        guide.setGuideNumber("GD 2026/050");
        guide.setCompany(company);
        guide.setClient(client);
        guide.setWarehouse(warehouse);
        guide.setOrderId(100L);
        guide.setOrderNumber("ENC 2026/012");
        guide.setGuideDate(LocalDateTime.of(2026, 10, 4, 14, 0));

        Product product = new Product();
        product.setId(10L);
        product.setName("Ferro de Construção 12mm");
        product.setReference("FER-12");

        DeliveryGuideLine line = new DeliveryGuideLine();
        line.setGuide(guide);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("50"));
        line.setUnitPrice(new BigDecimal("800.00"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal("40000.00"));
        guide.setLines(List.of(line));

        return guide;
    }

    private static Invoice sampleInvoice() {
        Company company = sampleCompany();
        Client client = new Client();
        client.setId(30L);
        client.setName("Cliente Beta Lda");
        client.setTaxId("400555666");
        client.setAddress("Estrada Nacional 4, Matola");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setName("Armazém Matola");

        Product product = new Product();
        product.setId(10L);
        product.setName("Ferro de Construção 12mm");
        product.setReference("FER-12");

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
