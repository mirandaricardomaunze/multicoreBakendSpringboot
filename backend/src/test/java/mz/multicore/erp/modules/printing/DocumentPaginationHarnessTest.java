package mz.multicore.erp.modules.printing;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Ver docs/DOCUMENTOS_PAGINACAO_SPEC.md. */
class DocumentPaginationHarnessTest {

    private static final List<String> COPIES = List.of(
            "1.ª VIA — ORIGINAL (Cliente)", "2.ª VIA — DUPLICADO (Contabilidade / Fisco)");

    private final InvoiceA4Renderer renderer =
            new InvoiceA4Renderer(new LineRowMapper(mock(ProductBatchRepository.class)));

    @Test
    @DisplayName("DP-01: A página 2 do original continua a ser o original")
    void dp01_secondPageKeepsCopy() throws Exception {
        List<String> pages = pages(renderer.render(invoice(40), COPIES));

        assertThat(pages.size()).isGreaterThan(2);
        assertThat(pages.get(0)).contains("Original · Página 1");
        assertThat(pages.get(1)).contains("Original · Página 2").doesNotContain("Duplicado");
    }

    @Test
    @DisplayName("DP-02: Cada via conta as suas páginas")
    void dp02_copyRestartsCounting() throws Exception {
        List<String> pages = pages(renderer.render(invoice(40), COPIES));

        String first = pages.stream().filter(page -> page.contains("2.ª VIA")).findFirst().orElseThrow();
        assertThat(first).contains("Duplicado · Página 1");
        assertThat(pages.get(pages.size() - 1)).contains("Duplicado").doesNotContain("Página 9");
    }

    @Test
    @DisplayName("DP-03: A página de continuação identifica o documento e as colunas")
    void dp03_continuationIsIdentified() throws Exception {
        String second = pages(renderer.render(invoice(40), COPIES)).get(1);

        assertThat(second).contains("FT 2026/900", "Designação", "Valor unit.");
    }

    @Test
    @DisplayName("DP-04: Uma linha continua a dar uma página por via")
    void dp04_singlePagePerCopy() throws Exception {
        List<String> pages = pages(renderer.render(invoice(1), COPIES));

        assertThat(pages).hasSize(2);
        assertThat(pages.get(1)).contains("Duplicado · Página 1");
    }

    @Test
    @DisplayName("DP-05 e 06: Só o lote real aparece no documento")
    void dp05_systemBatchHidden() {
        assertThat(LineRowMapper.isDisplayableBatch("AUTO-1793563341849")).isFalse();
        assertThat(LineRowMapper.isDisplayableBatch("L2026-001")).isTrue();
    }

    @Test
    @DisplayName("DP-07: Um total alto cabe na coluna do recibo térmico")
    void dp07_receiptTotalFits() {
        Font font = PdfTheme.boldFont();
        float width = font.getCalculatedBaseFont(false).getWidthPoint("166.869.318,95", font.getSize());
        float printable = 226f - PdfTheme.MARGIN_LEFT - PdfTheme.MARGIN_RIGHT;
        float[] columns = ReceiptDocumentRenderer.LINE_COLUMNS;
        float cellPadding = 8f;

        assertThat(width + cellPadding).isLessThan(printable * columns[1] / (columns[0] + columns[1]));
    }

    private static Invoice invoice(int lines) {
        Company company = new Company();
        company.setName("Multicore Moçambique Lda");
        company.setTaxId("400123456");
        Client client = new Client();
        client.setName("Cliente Teste Lda");
        client.setTaxId("400999888");

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("FT 2026/900");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setCreatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        invoice.setTotalBeforeTax(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("160.00"));
        invoice.setTotalAmount(new BigDecimal("1160.00"));
        List<InvoiceLine> rows = new ArrayList<>();
        for (int index = 0; index < lines; index++) {
            Product product = new Product();
            product.setId((long) index);
            product.setName("Arroz Agulha Longo Extra Selecionado Importado Saco de 25 Kilogramas #" + index);
            product.setReference("REF-" + index);
            product.setBarcode("690000000000" + index);
            InvoiceLine line = new InvoiceLine();
            line.setInvoice(invoice);
            line.setProduct(product);
            line.setQuantity(new BigDecimal("10"));
            line.setUnitPrice(new BigDecimal("100.00"));
            line.setTaxRate(new BigDecimal("0.16"));
            line.setDiscountPercentage(BigDecimal.ZERO);
            line.setLineTotal(new BigDecimal("1000.00"));
            rows.add(line);
        }
        invoice.setLines(rows);
        return invoice;
    }

    private static List<String> pages(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) pages.add(extractor.getTextFromPage(page));
            return pages;
        } finally {
            reader.close();
        }
    }
}
