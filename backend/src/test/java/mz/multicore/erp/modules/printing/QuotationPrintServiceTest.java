package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuotationPrintServiceTest {

    private QuotationRepository quotationRepository;
    private LineRowMapper lineRowMapper;
    private DocumentConfigService documentConfigService;
    private QuotationPrintService printService;

    @BeforeEach
    void setUp() {
        quotationRepository = mock(QuotationRepository.class);
        lineRowMapper = new LineRowMapper(new mz.multicore.erp.modules.inventory.service.ProductBatchLookup(mock(ProductBatchRepository.class)));
        documentConfigService = mock(DocumentConfigService.class);
        printService = new QuotationPrintService(new mz.multicore.erp.modules.comercial.service.OrderLookup(
                mock(mz.multicore.erp.modules.comercial.repository.OrderRepository.class), quotationRepository,
                mock(mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository.class)),
                lineRowMapper, documentConfigService);

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void render_cabecalhoGrelhaMetadadosEColunasDeBarras() throws Exception {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Portugal Lda");
        company.setTaxId("501982736");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setName("Armazém Central");

        Client client = new Client();
        client.setId(3L);
        client.setName("Cliente Alpha");
        client.setTaxId("123456789");
        client.setAddress("—"); // Traço sentinela não deve sair impresso

        Product product = new Product();
        product.setId(10L);
        product.setName("Computador Portátil Core i7");
        product.setBarcode("5601234567890");
        product.setReference("LAP-007");

        Quotation quotation = new Quotation();
        quotation.setId(200L);
        quotation.setQuotationNumber("CT-2026/001");
        quotation.setCompany(company);
        quotation.setClient(client);
        quotation.setWarehouse(warehouse);
        quotation.setStatus(QuotationStatus.SENT);
        quotation.setQuotationDate(LocalDateTime.of(2026, 10, 18, 10, 0));
        quotation.setValidUntil(LocalDate.now().plusDays(15));
        quotation.setTotalBeforeTax(new BigDecimal("1000.00"));
        quotation.setTaxAmount(new BigDecimal("160.00"));
        quotation.setTotalAmount(new BigDecimal("1160.00"));

        QuotationLine line = new QuotationLine();
        line.setQuotation(quotation);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("1.000"));
        line.setUnitPrice(new BigDecimal("1000.00"));
        line.setTaxRate(new BigDecimal("16.00"));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("1000.00"));
        quotation.setLines(List.of(line));

        when(quotationRepository.findByIdWithLinesAndCompanyId(200L, 1L)).thenReturn(Optional.of(quotation));
        when(documentConfigService.getColumns(eq(1L), any(DocumentType.class))).thenReturn(DocumentColumnsDTO.all());

        byte[] pdf = printService.render(200L);
        String text = extractText(pdf);

        // Documento e estado
        assertTrue(text.contains("COTAÇÃO"), "Devia conter COTAÇÃO: " + text);
        assertTrue(text.contains("CT-2026/001"), "Devia conter número da cotação: " + text);
        assertTrue(text.contains("Estado: Enviada"), "Devia conter estado: " + text);

        // Cliente e metadados
        assertTrue(text.contains("PROPOSTA PARA"), "Devia conter cabeçalho do cliente: " + text);
        assertTrue(text.contains("Cliente Alpha"), "Devia conter nome do cliente: " + text);
        assertTrue(text.contains("NUIT: 123456789"), "Devia conter NUIT: " + text);
        assertFalse(text.contains("NUIT: 123456789\n—"), "Não deve conter traço de endereço solto");

        assertTrue(text.contains("DATA DA PROPOSTA"), "Devia conter grelha: " + text);
        assertTrue(text.contains("VALIDADE"), "Devia conter validade: " + text);
        assertTrue(text.contains("COND. PAGAMENTO"), "Devia conter cond. pagamento: " + text);
        assertTrue(text.contains("REFERÊNCIA"), "Devia conter referência: " + text);

        // Tabela canónica com código de barras
        assertTrue(text.contains("Código de Barras"), "Devia conter coluna de Código de Barras: " + text);
        assertTrue(text.contains("5601234567890"), "Devia conter código de barras do produto: " + text);
        assertTrue(text.contains("Computador Portátil Core i7"), "Devia conter designação do produto: " + text);

        // Totais e assinaturas
        assertTrue(text.contains("TOTAL A PAGAR"), "Devia conter TOTAL A PAGAR: " + text);
        assertTrue(text.contains("Pela empresa"), "Devia conter bloco de assinatura: " + text);
        assertTrue(text.contains("Aceite pelo cliente"), "Devia conter bloco de aceite do cliente: " + text);
        assertTrue(text.contains("Este documento é uma proposta comercial"), "Devia conter disclaimer: " + text);
    }

    @Test
    void render_avisoQuandoCotacaoCaducada() throws Exception {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Portugal Lda");

        Quotation quotation = new Quotation();
        quotation.setId(201L);
        quotation.setQuotationNumber("CT-2026/002");
        quotation.setCompany(company);
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setWalkInName("Consumidor Balcão");
        quotation.setQuotationDate(LocalDateTime.now().minusDays(30));
        quotation.setValidUntil(LocalDate.now().minusDays(5)); // Caducada
        quotation.setTotalBeforeTax(new BigDecimal("50.00"));
        quotation.setTaxAmount(BigDecimal.ZERO);
        quotation.setTotalAmount(new BigDecimal("50.00"));
        quotation.setLines(List.of());

        when(quotationRepository.findByIdWithLinesAndCompanyId(201L, 1L)).thenReturn(Optional.of(quotation));
        when(documentConfigService.getColumns(eq(1L), any(DocumentType.class))).thenReturn(DocumentColumnsDTO.all());

        byte[] pdf = printService.render(201L);
        String text = extractText(pdf);

        assertTrue(text.contains("ATENÇÃO: esta cotação já caducou"), "Devia alertar que caducou: " + text);
        assertTrue(text.contains("Consumidor Balcão"), "Devia conter nome livre: " + text);
    }

    @Test
    void render_comNotasECondicoesERodapeConfigurado() throws Exception {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Portugal Lda");

        Quotation quotation = new Quotation();
        quotation.setId(202L);
        quotation.setQuotationNumber("CT-2026/003");
        quotation.setCompany(company);
        quotation.setStatus(QuotationStatus.SENT);
        quotation.setQuotationDate(LocalDateTime.now());
        quotation.setValidUntil(LocalDate.now().plusDays(30));
        quotation.setPaymentTerms("30 dias da entrega");
        quotation.setDeliveryTerms("Entrega no domicílio do cliente");
        quotation.setNotes("Preço inclui montagem e calibração no local.");
        quotation.setTotalBeforeTax(BigDecimal.ZERO);
        quotation.setTaxAmount(BigDecimal.ZERO);
        quotation.setTotalAmount(BigDecimal.ZERO);
        quotation.setLines(List.of());

        DocumentColumnsDTO columns = new DocumentColumnsDTO(
                true, true, true, true, true, true, true, true, true, true, true,
                "Multicore ERP - Documento processado por computador."
        );

        when(quotationRepository.findByIdWithLinesAndCompanyId(202L, 1L)).thenReturn(Optional.of(quotation));
        when(documentConfigService.getColumns(eq(1L), any(DocumentType.class))).thenReturn(columns);

        byte[] pdf = printService.render(202L);
        String text = extractText(pdf);

        assertTrue(text.contains("30 dias da entrega"), text);
        assertTrue(text.contains("Entrega no domicílio do cliente"), text);
        assertTrue(text.contains("Preço inclui montagem e calibração"), text);
        assertTrue(text.contains("Multicore ERP - Documento processado por computador."), text);
    }

    @Test
    void renderThermal_geraReciboValido() throws Exception {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Portugal Lda");

        Product product = new Product();
        product.setId(10L);
        product.setName("Artigo Térmico");

        Quotation quotation = new Quotation();
        quotation.setId(203L);
        quotation.setQuotationNumber("CT-2026/004");
        quotation.setCompany(company);
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setQuotationDate(LocalDateTime.now());
        quotation.setValidUntil(LocalDate.now().plusDays(7));
        quotation.setTotalBeforeTax(new BigDecimal("100.00"));
        quotation.setTaxAmount(BigDecimal.ZERO);
        quotation.setTotalAmount(new BigDecimal("100.00"));

        QuotationLine line = new QuotationLine();
        line.setQuotation(quotation);
        line.setProduct(product);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setLineTotal(new BigDecimal("100.00"));
        quotation.setLines(List.of(line));

        when(quotationRepository.findByIdWithLinesAndCompanyId(203L, 1L)).thenReturn(Optional.of(quotation));

        byte[] thermalPdf = printService.renderThermal(203L);
        assertNotNull(thermalPdf);
        assertTrue(thermalPdf.length > 0);

        String text = extractText(thermalPdf);
        assertTrue(text.contains("COTAÇÃO COMERCIAL (80mm)"), text);
        assertTrue(text.contains("CT-2026/004"), text);
    }

    private static String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder sb = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int p = 1; p <= reader.getNumberOfPages(); p++) {
                sb.append(extractor.getTextFromPage(p)).append('\n');
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }
}
