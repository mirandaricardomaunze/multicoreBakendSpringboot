package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
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
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.ReceiptLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.service.ProductBatchLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static mz.multicore.erp.modules.printing.FiscalDocumentFixtures.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DT-03 (SPEC §3.2): com todas as colunas das linhas escondidas, facturas, notas de crédito e de débito,
 * recibos e vendas POS continuam a mostrar emitente, adquirente (ou "Consumidor Final"), tipo/número/data,
 * total sem IVA, IVA por taxa, total com IVA, motivo de isenção e a indicação da via.
 */
class DocumentTemplateFiscalElementsTest {

    private static final DocumentColumnsDTO ALL_HIDDEN = new DocumentColumnsDTO(
            false, false, false, false, false, false, false, false, false, false, false, null);

    private final LineRowMapper lineRowMapper = new LineRowMapper(new ProductBatchLookup(mock(ProductBatchRepository.class)));

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
    void facturaA4_mantemElementosFiscais_e_ivaPorTaxa() throws Exception {
        String text = extract(new InvoiceA4Renderer(lineRowMapper).render(invoice(client()), List.of("2.ª VIA — DUPLICADO")));

        assertIssuer(text);
        assertContains(text, "FATURA", "FT 2026/001", "04/10/2026", "VENCIMENTO", "Cliente Alfa Lda", "400888777",
                "Subtotal (sem IVA)", "IVA 16,0%", "Isento — " + EXEMPTION, "1.500,00", "1.660,00", "2.ª VIA — DUPLICADO");
    }

    @Test
    void notaDeCredito_comColunasEscondidas_mantemIvaPorTaxa_e_motivoDeIsencao() throws Exception {
        CreditNoteService service = mock(CreditNoteService.class);
        DocumentConfigService config = mock(DocumentConfigService.class);
        when(service.loadForPrint(100L)).thenReturn(creditNote());
        when(config.getColumns(eq(1L), any(DocumentType.class))).thenReturn(ALL_HIDDEN);

        String text = extract(new CreditNotePrintService(service, lineRowMapper, config).render(100L));

        assertIssuer(text);
        assertContains(text, "NC 2026/001", "04/10/2026", "NUIT: Consumidor Final", "Subtotal (sem IVA)",
                "IVA 16,0%", "Isento — " + EXEMPTION, "160,00", "TOTAL CREDITADO", "1.660,00",
                "1.ª VIA — ORIGINAL (Cliente)");
    }

    @Test
    void notaDeDebito_mantemIvaPorTaxa() throws Exception {
        String text = extract(new DebitNoteDocumentRenderer(null).render(debitNote(), List.of("1.ª VIA — ORIGINAL (Cliente)")));

        assertIssuer(text);
        assertContains(text, "ND 2026/001", "04/10/2026", "Cliente Alfa Lda", "NUIT: 400888777",
                "IVA 16,0%", "Isento de IVA", "32,00", "TOTAL DEBITADO", "282,00", "1.ª VIA — ORIGINAL (Cliente)");
    }

    @Test
    void vendaPos_semColunas_mostraConsumidorFinal_e_ivaPorTaxa() throws Exception {
        Invoice sale = invoice(null);
        sale.setSalesChannel(SalesChannel.POS);

        String text = extract(new ReceiptDocumentRenderer(null).renderDocument(sale, ALL_HIDDEN, List.of()));

        assertIssuer(text);
        assertContains(text, "RECIBO POS", "FT 2026/001", "04/10/2026", "Cliente: Consumidor Final",
                "NUIT Cliente: Consumidor Final", "Subtotal", "IVA 16,0%", "Isento — " + EXEMPTION,
                "1.660,00", "1.ª VIA — ORIGINAL (CLIENTE)");
    }

    @Test
    void recibo_mostraAdquirente_e_ivaPorTaxaDaFactura() throws Exception {
        Receipt receipt = new Receipt();
        receipt.setId(7L);
        receipt.setReceiptNumber("RC 2026/007");
        receipt.setCompany(company());
        receipt.setInvoice(invoice(null));
        receipt.setAmountPaid(new BigDecimal("1660.00"));
        receipt.setPaymentMethod("CASH");
        receipt.setReceiptDate(LocalDateTime.of(2026, 10, 5, 9, 0));
        ReceiptLookup lookup = mock(ReceiptLookup.class);
        when(lookup.find(7L)).thenReturn(Optional.of(receipt));

        String text = extract(new ReceiptDocumentPrintService(lookup, null).render(7L));

        assertIssuer(text);
        assertContains(text, "RC 2026/007", "05/10/2026", "Consumidor Final", "Subtotal (sem IVA)",
                "IVA 16,0%", "Isento — " + EXEMPTION, "TOTAL DA FATURA", "1.660,00");
    }

    private static void assertIssuer(String text) {
        assertContains(text, "Empresa Moçambique Lda", "400123456", "Av. Julius Nyerere");
    }

    private static void assertContains(String text, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(text.contains(fragment), "Elemento fiscal em falta: '" + fragment + "' em\n" + text);
        }
    }
}
