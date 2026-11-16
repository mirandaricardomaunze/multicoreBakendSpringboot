package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.fiscal.dto.CreateTaxRateRequest;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.fiscal.service.TaxRateService;
import mz.multicore.erp.modules.inventory.service.ProductBatchLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Legal text of the invoice (docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md, "Texto legal da fatura").
 * An invoice that does not charge VAT must state the specific legal reason; these cases read the PDF text.
 */
class InvoiceLegalTextHarnessTest {

    private static final String FOOD = "Código do IVA, art. 9.º, n.º 1, al. teste (bens alimentares)";
    private static final String HEALTH = "Código do IVA, art. 9.º, n.º 1, al. teste (saúde)";

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void lt01_exemptGroupPrintsItsLegalReasonAndNeverTheGenericArticle() {
        var rows = List.of(row("0.16", null), row("0", FOOD), row("0", HEALTH), row("0", FOOD));

        var labels = TotalsBlockRenderer.calculateBreakdowns(rows).stream()
                .map(TotalsBlockRenderer.TaxBreakdown::taxRateLabel).toList();

        assertThat(labels).containsExactly("IVA 16,0%", "Isento — " + FOOD, "Isento — " + HEALTH);
        assertThat(labels).noneMatch(l -> l.contains("Art. 9/10"));
    }

    @Test
    void lt02_withoutConfiguredReasonNoArticleIsInvented() {
        assertThat(TotalsBlockRenderer.exemptLabel(null)).isEqualTo("Isento de IVA");
        assertThat(TotalsBlockRenderer.exemptLabel("  ")).isEqualTo("Isento de IVA");
    }

    @Test
    void lt03_reasonIsFrozenOnTheLineAtIssue() throws Exception {
        InvoiceLine line = line(product(FOOD), "0");
        persist(line);

        line.getProduct().getTaxRate().setLegalBasis("texto alterado depois");

        assertThat(line.getExemptionReason()).isEqualTo(FOOD);
        assertThat(line.effectiveExemptionReason()).isEqualTo(FOOD);
    }

    @Test
    void lt04_taxedLineHasNoReasonAndOldLinesUseTheCurrentBasis() throws Exception {
        InvoiceLine taxed = line(product(FOOD), "0.16");
        persist(taxed);
        assertThat(taxed.getExemptionReason()).isNull();
        assertThat(taxed.effectiveExemptionReason()).isNull();

        InvoiceLine beforeV135 = line(product(HEALTH), "0");
        assertThat(beforeV135.effectiveExemptionReason()).isEqualTo(HEALTH);
    }

    @Test
    void lt05_thermalInvoiceShowsRatePerLineReasonAndBuyerAddress() throws Exception {
        String text = text(new InvoiceThermalRenderer().render(invoice()));

        assertThat(text).contains("IVA 16%").contains("Isento").contains("Isento — " + FOOD.substring(0, 20));
        assertThat(text).contains("400123456").contains("Morada: Av. 25 de Setembro");
    }

    @Test
    void lt06_a4InvoicePrintsReasonAndKeepsTheLegalWarranty() throws Exception {
        InvoiceA4Renderer renderer = new InvoiceA4Renderer(new LineRowMapper(mock(ProductBatchLookup.class)));
        String text = text(renderer.render(invoice(), List.of("1.ª VIA — ORIGINAL (Cliente)")));

        assertThat(text).contains("Isento —").doesNotContain("Art. 9/10");
        assertThat(text.replaceAll("\\s+", " ")).contains("sem prejuízo da garantia legal");
    }

    @Test
    void lt07_vatFreeRateWithoutLegalBasisIsRefused() {
        TaxRateRepository repository = mock(TaxRateRepository.class);
        when(repository.findByCodeAndCompaniesId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(repository.findByCode(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        CompanyService companies = mock(CompanyService.class);
        when(companies.getCurrentCompanyReference(anyLong())).thenReturn(new Company());
        CurrentUserContext.setCurrentCompanyId(1L);
        TaxRateService service = new TaxRateService(repository, companies);

        assertThatThrownBy(() -> service.create(new CreateTaxRateRequest("ISE", "Isento", "IVA_EXEMPT", BigDecimal.ZERO, " ")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("base legal");
        assertThatThrownBy(() -> service.create(new CreateTaxRateRequest("EXP", "Exportação", "IVA_ZERO", BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(service.create(new CreateTaxRateRequest("ISE2", "Isento", "IVA_EXEMPT", BigDecimal.ZERO, FOOD)))
                .isNotNull();
        assertThat(service.create(new CreateTaxRateRequest("N16", "Normal", "IVA_STANDARD", new BigDecimal("0.16"), null)))
                .isNotNull();
    }

    private static LineItemsTableRenderer.Row row(String rate, String reason) {
        return new LineItemsTableRenderer.Row(null, null, "Artigo", null, BigDecimal.ONE, null, null,
                new BigDecimal("100"), new BigDecimal(rate), BigDecimal.ZERO, new BigDecimal("100"), reason);
    }

    private static Product product(String legalBasis) {
        TaxRate rate = new TaxRate();
        rate.setLegalBasis(legalBasis);
        Product product = new Product();
        product.setName("Arroz 5kg");
        product.setTaxRate(rate);
        return product;
    }

    private static InvoiceLine line(Product product, String rate) {
        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("550"));
        line.setTaxRate(new BigDecimal(rate));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("1100"));
        return line;
    }

    /** Runs the JPA {@code @PrePersist} callback the way the persistence provider does at issue. */
    private static void persist(InvoiceLine line) throws Exception {
        Method callback = InvoiceLine.class.getDeclaredMethod("snapshotExemptionReason");
        callback.setAccessible(true);
        callback.invoke(line);
    }

    private static Invoice invoice() throws Exception {
        Company company = new Company();
        company.setName("Loja Teste");
        company.setTaxId("400999999");
        Client client = new Client();
        client.setName("Mercearia Bom Preco");
        client.setTaxId("400123456");
        client.setAddress("Av. 25 de Setembro, Maputo");

        InvoiceLine exempt = line(product(FOOD), "0");
        persist(exempt);
        InvoiceLine taxed = line(product(null), "0.16");
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("FT-2026/99");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setCreatedAt(LocalDateTime.of(2026, 11, 14, 10, 0));
        invoice.setTotalBeforeTax(new BigDecimal("2200"));
        invoice.setTaxAmount(new BigDecimal("176"));
        invoice.setTotalAmount(new BigDecimal("2376"));
        invoice.getLines().addAll(List.of(exempt, taxed));
        return invoice;
    }

    private static String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }
}
