package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReceiptPrintServiceTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void vatRateLabelShowsTaxedAndExemptLines() {
        assertEquals("IVA: 16%", ReceiptPrintService.vatRateLabel(new BigDecimal("0.16")));
        assertEquals("IVA: 5%", ReceiptPrintService.vatRateLabel(new BigDecimal("0.0500")));
        assertEquals("IVA: Isento", ReceiptPrintService.vatRateLabel(BigDecimal.ZERO));
    }

    @Test
    void lineDetailsStacksQuantityAndPriceBelowDescription() {
        InvoiceLine line = new InvoiceLine();
        line.setQuantity(new BigDecimal("2.500"));
        line.setUnitPrice(new BigDecimal("125.00"));

        assertEquals("2.5 x 125,00 MT",
                ReceiptPrintService.lineDetailsLabel(line, DocumentColumnsDTO.all()));
        DocumentColumnsDTO quantityOnly = new DocumentColumnsDTO(
                true, true, true, true, true, false, true, true, null);
        assertEquals("Qtd: 2.5", ReceiptPrintService.lineDetailsLabel(line, quantityOnly));
    }

    @Test
    void rendersReadableEightyMillimetrePreview() throws Exception {
        // O serviço resolve a empresa pelo contexto. Fora de um pedido HTTP não há contexto, por isso
        // declara-se — antes o teste passava porque o contexto assumia a empresa 1 sozinho.
        CurrentUserContext.setCurrentCompanyId(1L);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        PaymentEntryRepository payments = mock(PaymentEntryRepository.class);
        DocumentConfigService config = mock(DocumentConfigService.class);
        Invoice invoice = previewInvoice();
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(payments.findByInvoiceIdOrderByPaidAtAsc(1L)).thenReturn(List.of());
        when(config.getColumns(1L, mz.multicore.erp.modules.documents.model.DocumentType.POS_RECEIPT))
                .thenReturn(DocumentColumnsDTO.all());

        byte[] pdf = new ReceiptPrintService(invoices, payments, config).render(1L);

        assertTrue(pdf.length > 1_000);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        Path preview = Path.of("target", "receipt-layout-preview.pdf");
        Files.createDirectories(preview.getParent());
        Files.write(preview, pdf);
    }

    private static Invoice previewInvoice() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Loja Central");
        company.setTaxId("400123456");
        company.setAddress("Av. 25 de Setembro, Maputo");

        Product product = new Product();
        product.setName("Óleo alimentar premium garrafa familiar de cinco litros");
        product.setReference("OLEO-5L-PREMIUM");

        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("1250.00"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal("2900.00"));

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setCompany(company);
        invoice.setInvoiceNumber("FT 2026/000123");
        invoice.setCustomerName("Consumidor Final");
        invoice.setCreatedBy("operador.caixa");
        invoice.setCreatedAt(LocalDateTime.of(2026, 8, 5, 19, 30));
        invoice.setTotalBeforeTax(new BigDecimal("2500.00"));
        invoice.setTaxAmount(new BigDecimal("400.00"));
        invoice.setTotalAmount(new BigDecimal("2900.00"));
        invoice.addLine(line);
        return invoice;
    }
}
