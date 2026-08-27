package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ED-19..21 — o talão de separação passou a ser um talão como o do POS.
 *
 * <p>Ver docs/ENCOMENDA_DUAS_VIAS_SPEC.md §1.2. Antes desta iteração, este documento saía sem
 * qualquer identificação da empresa — nem logótipo, nem NUIT, nem morada, nem telefone — enquanto
 * o recibo do POS, impresso na mesma loja e muitas vezes na mesma impressora, levava tudo isso.
 *
 * <p>Estes casos lêem o <b>texto do PDF</b>, não o código que o gera: é a diferença entre provar
 * que sai impresso e confiar que sai.
 */
class OrderPickingPrintServiceTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test // ED-19
    void oTalaoDeSeparacaoIdentificaAEmpresaComoOReciboDoPOS() throws Exception {
        String text = renderText(company(true));

        assertTrue(text.contains("Multicore Loja Central"), text);
        assertTrue(text.contains("400123456"), "falta o NUIT: " + text);
        assertTrue(text.contains("Av. 25 de Setembro"), "falta a morada: " + text);
        assertTrue(text.contains("84 000 0000"), "falta o telefone: " + text);
    }

    @Test // ED-19 (continua a ser a guia de separação)
    void mantemOQueEProprioDaGuiaDeSeparacao() throws Exception {
        String text = renderText(company(false));

        assertTrue(text.contains("GUIA DE SEPARACAO"), text);
        assertTrue(text.contains("EC-2026/9"), text);
        assertTrue(text.contains("Arroz"), text);
        assertTrue(text.contains("PESO BRUTO TOTAL"), text);
        assertTrue(text.contains("Separado por"), text);
        assertTrue(text.contains("Conferido por"), text);
    }

    @Test // ED-21
    void semLogotipoOuComLogotipoIlegivelOTalaoSaiNaMesma() throws Exception {
        Company semLogo = company(false);

        Company logoCorrompido = company(false);
        logoCorrompido.setLogo(new byte[]{1, 2, 3, 4});   // não é uma imagem

        // Um documento operacional não pode deixar de ser impresso por causa da decoração.
        assertTrue(renderText(semLogo).contains("GUIA DE SEPARACAO"));
        assertTrue(renderText(logoCorrompido).contains("GUIA DE SEPARACAO"));
    }

    @Test // ED-21 (reimpressão)
    void aReimpressaoIdentificaSeComoTal() throws Exception {
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findById(9L)).thenReturn(Optional.of(order(company(false))));
        CurrentUserContext.setCurrentCompanyId(1L);

        byte[] pdf = new OrderPickingPrintService(orders).render(9L, true);

        assertTrue(extract(pdf).contains("REIMPRESSAO"), extract(pdf));
    }

    private String renderText(Company company) throws Exception {
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findById(9L)).thenReturn(Optional.of(order(company)));
        CurrentUserContext.setCurrentCompanyId(1L);

        byte[] pdf = new OrderPickingPrintService(orders).render(9L, false);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        return extract(pdf);
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

    private static Company company(boolean withContacts) {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Loja Central");
        if (withContacts) {
            company.setTaxId("400123456");
            company.setAddress("Av. 25 de Setembro, Maputo");
            company.setPhone("84 000 0000");
        }
        return company;
    }

    private static Order order(Company company) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(3L);
        warehouse.setName("Armazem Central");

        Product product = new Product();
        product.setName("Arroz agulha saco de 25 kg");
        product.setReference("ARZ-25");
        product.setGrossUnitWeightKg(new BigDecimal("25.000"));

        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("4"));

        Order order = new Order();
        order.setId(9L);
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setOrderNumber("EC-2026/9");
        order.setCreatedAt(LocalDateTime.of(2026, 8, 18, 9, 15));
        order.setKind(mz.multicore.erp.modules.comercial.model.OrderKind.PICKING_REQUEST);

        mz.multicore.erp.modules.comercial.model.Client client =
                new mz.multicore.erp.modules.comercial.model.Client();
        client.setName("Mercearia Bom Preco");
        order.setClient(client);
        order.addLine(line);
        return order;
    }
}
