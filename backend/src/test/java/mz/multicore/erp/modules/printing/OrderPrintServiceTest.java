package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderPrintServiceTest {

    private OrderRepository orderRepository;
    private LineRowMapper lineRowMapper;
    private DocumentConfigService documentConfigService;
    private OrderPrintService printService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        lineRowMapper = new LineRowMapper(mock(ProductBatchRepository.class));
        documentConfigService = mock(DocumentConfigService.class);
        printService = new OrderPrintService(orderRepository, lineRowMapper, documentConfigService);

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void render_cabecalhoContemEstadoEOcultaPesoZeroEEnderecoInvalido() throws Exception {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Portugal Lda");
        company.setTaxId("501982736");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setName("Armazém Lisboa Central");

        Client client = new Client();
        client.setId(3L);
        client.setName("Miranda");
        client.setTaxId("000000000");
        client.setAddress("—"); // Traço sentinela não deve sair impresso

        Product product = new Product();
        product.setId(10L);
        product.setName("Arroz Agulha 5kg");
        product.setBarcode("5601000000017");
        product.setReference("MRC-0001");
        product.setGrossUnitWeightKg(BigDecimal.ZERO); // Peso zero não deve aparecer como linha isolada

        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber("EC-2026/1");
        order.setCompany(company);
        order.setClient(client);
        order.setWarehouse(warehouse);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.of(2026, 10, 8, 20, 50));
        order.setTotalBeforeTax(new BigDecimal("550.00"));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("550.00"));

        OrderLine line = new OrderLine();
        line.setOrder(order);
        line.setProduct(product);
        line.setBatchNumber("LEGACY-1");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("550.00"));
        line.setTaxRate(BigDecimal.ZERO);
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("550.00"));
        order.setLines(List.of(line));

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(documentConfigService.getColumns(eq(1L), any(DocumentType.class))).thenReturn(DocumentColumnsDTO.all());

        byte[] pdf = printService.render(100L);
        String text = extractText(pdf);

        // Estado no documento
        assertTrue(text.contains("EC-2026/1"), text);
        assertTrue(text.contains("Estado:"), text);
        assertTrue(text.contains("Por facturar") || text.contains("Pendente de aprovação"), text);

        // O traço sentinela de endereço não deve ser impresso
        assertFalse(text.contains("NUIT: 000000000\n—") || text.contains("000000000\n—"), "Não deve conter traço solto: " + text);

        // Peso zero não deve ser impresso
        assertFalse(text.contains("Peso bruto total: 0.000 kg"), "Não deve imprimir peso zero: " + text);

        // Validade fictícia e lote legado não devem poluir a descrição
        assertFalse(text.contains("Val: 31/12/9999"), "Não deve conter data fictícia 9999: " + text);
        assertFalse(text.contains("LEGACY-1"), "Não deve conter lote LEGACY-1 na descrição: " + text);
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
