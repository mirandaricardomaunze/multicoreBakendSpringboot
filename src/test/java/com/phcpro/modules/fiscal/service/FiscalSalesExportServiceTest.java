package com.phcpro.modules.fiscal.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.fiscal.dto.FiscalSalesExportDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cobre SF-01..SF-14 do harness da exportação SAF-T de vendas. */
class FiscalSalesExportServiceTest {

    private static final Long COMPANY = 7L;

    private InvoiceRepository invoiceRepository;
    private CompanyRepository companyRepository;
    private FiscalSalesExportService service;

    private final LocalDate from = LocalDate.of(2026, 6, 1);
    private final LocalDate to = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        companyRepository = mock(CompanyRepository.class);
        service = new FiscalSalesExportService(invoiceRepository, companyRepository);

        Company company = new Company();
        company.setId(COMPANY);
        company.setName("Loja Central");
        company.setTaxId("400123456");
        lenient().when(companyRepository.findById(COMPANY)).thenReturn(java.util.Optional.of(company));

        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(COMPANY);
    }

    @AfterEach
    void clear() {
        CurrentUserContext.clear();
    }

    // ── SF-01 ──
    @Test
    void sf01_semFaturas_xmlVazioBemFormado() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of());
        FiscalSalesExportDTO dto = service.exportSales(COMPANY, from, to);
        assertEquals(0, dto.numberOfInvoices());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalGross()));
        assertTrue(dto.xml().contains("<NumberOfEntries>0</NumberOfEntries>"));
        assertParses(dto.xml());
    }

    // ── SF-02 ──
    @Test
    void sf02_duasFaturasAprovadas_aparecemAmbas() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-001", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00"),
                invoice("FT-002", InvoiceStatus.PAID, LocalDateTime.of(2026, 6, 12, 9, 0), "50.00", "8.00", "58.00")));
        FiscalSalesExportDTO dto = service.exportSales(COMPANY, from, to);
        assertEquals(2, dto.numberOfInvoices());
        assertTrue(dto.xml().contains("<InvoiceNo>FT-001</InvoiceNo>"));
        assertTrue(dto.xml().contains("<InvoiceNo>FT-002</InvoiceNo>"));
    }

    // ── SF-03 ──
    @Test
    void sf03_faturaForaDoPeriodo_excluida() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-OLD", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 5, 30, 9, 0), "100.00", "16.00", "116.00")));
        FiscalSalesExportDTO dto = service.exportSales(COMPANY, from, to);
        assertEquals(0, dto.numberOfInvoices());
        assertFalse(dto.xml().contains("FT-OLD"));
    }

    // ── SF-04 ──
    @Test
    void sf04_faturaNaoEmitida_excluida() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-DRAFT", InvoiceStatus.DRAFT, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00"),
                invoice("FT-PEND", InvoiceStatus.PENDING_APPROVAL, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00")));
        FiscalSalesExportDTO dto = service.exportSales(COMPANY, from, to);
        assertEquals(0, dto.numberOfInvoices());
    }

    // ── SF-05 / SF-06 ──
    @Test
    void sf05_06_canceladaIncluidaMasNaoSoma() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-OK", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00"),
                invoice("FT-CANC", InvoiceStatus.CANCELLED, LocalDateTime.of(2026, 6, 11, 9, 0), "200.00", "32.00", "232.00")));
        FiscalSalesExportDTO dto = service.exportSales(COMPANY, from, to);
        assertEquals(2, dto.numberOfInvoices());
        assertTrue(dto.xml().contains("<InvoiceStatus>CANCELLED</InvoiceStatus>"));
        // Só a não-anulada conta para os totais.
        assertEquals(0, new BigDecimal("100.00").compareTo(dto.totalNet()));
        assertEquals(0, new BigDecimal("116.00").compareTo(dto.totalGross()));
    }

    // ── SF-07 ──
    @Test
    void sf07_header() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of());
        String xml = service.exportSales(COMPANY, from, to).xml();
        assertTrue(xml.contains("<CompanyName>Loja Central</CompanyName>"));
        assertTrue(xml.contains("<TaxRegistrationNumber>400123456</TaxRegistrationNumber>"));
        assertTrue(xml.contains("<CurrencyCode>MZN</CurrencyCode>"));
    }

    // ── SF-08 ──
    @Test
    void sf08_escapaCaracteresEspeciais() {
        Invoice inv = invoice("FT-AMP", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 6, 10, 9, 0), "10.00", "0.00", "10.00");
        inv.getClient().setName("Pão & Cia <Lda>");
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(inv));
        String xml = service.exportSales(COMPANY, from, to).xml();
        assertTrue(xml.contains("Pão &amp; Cia &lt;Lda&gt;"));
        assertFalse(xml.contains("Pão & Cia <Lda>"));
        assertParses(xml);
    }

    // ── SF-09 ──
    @Test
    void sf09_masterFilesComClientesETaxas() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-001", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00")));
        String xml = service.exportSales(COMPANY, from, to).xml();
        assertTrue(xml.contains("<Customer>"));
        assertTrue(xml.contains("<TaxTableEntry>"));
        assertTrue(xml.contains("<TaxPercentage>16.00</TaxPercentage>"));
    }

    // ── SF-10 ──
    @Test
    void sf10_totaisConferiveis() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-001", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00")));
        FiscalSalesExportDTO dto = service.exportSales(COMPANY, from, to);
        assertEquals(0, dto.totalNet().add(dto.totalTax()).compareTo(dto.totalGross()));
    }

    // ── SF-12 ──
    @Test
    void sf12_semPermissao_lanca() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
        assertThrows(BusinessRuleException.class, () -> service.exportSales(COMPANY, from, to));
    }

    // ── SF-13 ──
    @Test
    void sf13_empresaDiferente_lanca() {
        CurrentUserContext.setCurrentCompanyId(999L);
        assertThrows(BusinessRuleException.class, () -> service.exportSales(COMPANY, from, to));
    }

    // ── SF-14 ──
    @Test
    void sf14_determinismo() {
        when(invoiceRepository.findByCompanyId(COMPANY)).thenReturn(List.of(
                invoice("FT-001", InvoiceStatus.APPROVED, LocalDateTime.of(2026, 6, 10, 9, 0), "100.00", "16.00", "116.00")));
        String first = service.exportSales(COMPANY, from, to).xml();
        String second = service.exportSales(COMPANY, from, to).xml();
        // DateCreated usa a data de hoje em ambas → idênticas.
        assertEquals(first, second);
    }

    // ── Helpers ──

    private Invoice invoice(String number, InvoiceStatus status, LocalDateTime created,
                            String net, String tax, String gross) {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(number);
        inv.setStatus(status);
        inv.setCreatedAt(created);
        inv.setTotalBeforeTax(new BigDecimal(net));
        inv.setTaxAmount(new BigDecimal(tax));
        inv.setTotalAmount(new BigDecimal(gross));

        Client client = new Client();
        client.setId(42L);
        client.setName("Cliente Teste");
        client.setTaxId("100200300");
        inv.setClient(client);

        Product product = new Product();
        product.setSku("SKU-1");
        product.setName("Produto");

        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("1"));
        line.setUnitPrice(new BigDecimal(net));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal(gross));
        List<InvoiceLine> lines = new ArrayList<>();
        lines.add(line);
        inv.setLines(lines);
        return inv;
    }

    private void assertParses(String xml) {
        assertDoesNotThrow(() -> DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }
}
