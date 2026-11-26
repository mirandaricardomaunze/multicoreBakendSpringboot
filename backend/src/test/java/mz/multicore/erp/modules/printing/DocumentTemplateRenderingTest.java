package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPRow;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.DebitNoteService;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.ReceiptLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentBankAccount;
import mz.multicore.erp.modules.documents.model.DocumentColumnConfig;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.repository.DocumentColumnConfigRepository;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.service.ProductBatchLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static mz.multicore.erp.modules.printing.FiscalDocumentFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DT-04 (MODELOS_DOCUMENTO_SPEC §3.3): os PDFs A4 (factura, nota de crédito, nota de débito, recibo) usam a cor,
 * o logótipo, as notas e as contas bancárias do modelo da empresa; os talões térmicos (factura simplificada e
 * recibo POS) usam só o logótipo, as notas e o rodapé — nunca a cor nem os dados bancários.
 *
 * <p>DT-05 (§3.3): a 2.ª via de um documento antigo sai com o modelo actual, sem mudar o conteúdo fiscal.
 */
class DocumentTemplateRenderingTest {

    /** #CC0000 = (0.8, 0, 0): contraste 5,9:1 com branco, aceite pelo DT-01. */
    private static final String ACCENT = "#CC0000";
    private static final String ACCENT_FILL = "0.8 0 0 rg";
    private static final String NOTES = "Mercadoria viaja por conta e risco do comprador.";
    private static final String BANK_LINE =
            "BCI · Titular: Empresa Moçambique Lda · NIB/IBAN: MZ59 0008 0000 1234 5678 9012 3";
    private static final DocumentTemplateDTO TEMPLATE = new DocumentTemplateDTO(ACCENT, DocumentLogoPosition.LEFT,
            NOTES, List.of(new DocumentBankAccountDTO("BCI", "Empresa Moçambique Lda",
                    "MZ59 0008 0000 1234 5678 9012 3")), true);

    /** Modelo alterado depois da emissão: #1F4E79 = (0.12157, 0.30588, 0.47451), outra posição, sem contas. */
    private static final String OTHER_NOTES = "Pagamento a 30 dias por transferência.";
    private static final String OTHER_ACCENT_FILL = "0.12157 0.30588 0.47451 rg";
    private static final DocumentTemplateDTO OTHER_TEMPLATE = new DocumentTemplateDTO("#1F4E79",
            DocumentLogoPosition.RIGHT, OTHER_NOTES, List.of(), true);

    private final LineRowMapper lineRowMapper = new LineRowMapper(new ProductBatchLookup(mock(ProductBatchRepository.class)));
    private final DocumentConfigService config = mock(DocumentConfigService.class);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        when(config.getTemplate(eq(1L), any(DocumentType.class))).thenReturn(TEMPLATE);
        when(config.getColumns(eq(1L), any(DocumentType.class))).thenReturn(columnsWithFooter("Volte sempre!"));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void facturaA4_usaCorLogotipoNotasEContasDoModelo() throws Exception {
        Invoice invoice = withLogo(invoice(client()));
        InvoiceLookup lookup = mock(InvoiceLookup.class);
        when(lookup.require(10L)).thenReturn(invoice);

        byte[] pdf = new InvoicePrintService(lookup, lineRowMapper, config).render(10L);

        assertTemplatedA4(pdf);
    }

    @Test
    void facturaA4_semModelo_usaCorDoTema_eNaoInventaNotasNemContas() throws Exception {
        byte[] pdf = new InvoiceA4Renderer(lineRowMapper).render(invoice(client()), List.of("1.ª VIA — ORIGINAL (Cliente)"));

        String text = text(pdf);
        assertFalse(text.contains(NOTES));
        assertFalse(text.contains("BCI"));
        assertFalse(content(pdf).contains(ACCENT_FILL), "Sem modelo não há cor de destaque própria");
    }

    @Test
    void notaDeCredito_usaModelo() throws Exception {
        CreditNoteService service = mock(CreditNoteService.class);
        var note = creditNote();
        note.setCompany(withLogo(note.getCompany()));
        when(service.loadForPrint(100L)).thenReturn(note);

        assertTemplatedA4(new CreditNotePrintService(service, lineRowMapper, config).render(100L));
    }

    @Test
    void notaDeDebito_usaModelo() throws Exception {
        DebitNoteService service = mock(DebitNoteService.class);
        var note = debitNote();
        note.setCompany(withLogo(note.getCompany()));
        when(service.loadForPrint(200L)).thenReturn(note);

        assertTemplatedA4(new DebitNotePrintService(service, new DebitNoteDocumentRenderer(config)).render(200L));
    }

    @Test
    void reciboA4_usaModelo() throws Exception {
        Receipt receipt = new Receipt();
        receipt.setId(7L);
        receipt.setReceiptNumber("RC 2026/007");
        receipt.setCompany(withLogo(company()));
        receipt.setInvoice(invoice(null));
        receipt.setAmountPaid(new BigDecimal("1660.00"));
        receipt.setPaymentMethod("CASH");
        receipt.setReceiptDate(LocalDateTime.of(2026, 10, 5, 9, 0));
        ReceiptLookup lookup = mock(ReceiptLookup.class);
        when(lookup.find(7L)).thenReturn(Optional.of(receipt));

        assertTemplatedA4(new ReceiptDocumentPrintService(lookup, config).render(7L));
    }

    @Test
    void facturaTermica_usaSoLogotipoNotasERodape() throws Exception {
        Invoice invoice = withLogo(invoice(client()));
        InvoiceLookup lookup = mock(InvoiceLookup.class);
        when(lookup.require(10L)).thenReturn(invoice);

        assertTemplatedThermal(new InvoicePrintService(lookup, lineRowMapper, config).renderThermal(10L));
    }

    @Test
    void reciboPos_usaSoLogotipoNotasERodape() throws Exception {
        Invoice sale = withLogo(invoice(null));
        sale.setSalesChannel(SalesChannel.POS);

        assertTemplatedThermal(new ReceiptDocumentRenderer(null).renderDocument(
                sale, columnsWithFooter("Volte sempre!"), List.of(), TEMPLATE));
    }

    @Test
    void cabecalho_colocaLogotipoNaPosicaoDoModelo() {
        Company company = withLogo(company());

        PdfPTable left = CompanyHeaderRenderer.build(company, "Nota de Débito", "ND 1", null, null,
                templateAt(DocumentLogoPosition.LEFT));
        assertEquals(1, left.getRows().size());
        assertInstanceOf(Image.class, firstElement(left.getRow(0).getCells()[0]));

        PdfPTable right = CompanyHeaderRenderer.build(company, "Nota de Débito", "ND 1", null, null,
                templateAt(DocumentLogoPosition.RIGHT));
        Element rightLogo = firstElement(right.getRow(0).getCells()[1]);
        assertInstanceOf(Image.class, rightLogo);
        assertEquals(Image.ALIGN_RIGHT, ((Image) rightLogo).getAlignment() & Image.ALIGN_RIGHT);
        assertFalse(firstElement(right.getRow(0).getCells()[0]) instanceof Image);

        PdfPTable center = CompanyHeaderRenderer.build(company, "Nota de Débito", "ND 1", null, null,
                templateAt(DocumentLogoPosition.CENTER));
        assertEquals(2, center.getRows().size(), "Logótipo centrado numa linha própria");
        PdfPRow logoRow = center.getRow(0);
        assertEquals(2, logoRow.getCells()[0].getColspan());
        assertInstanceOf(Image.class, firstElement(logoRow.getCells()[0]));
        assertFalse(firstElement(center.getRow(1).getCells()[0]) instanceof Image);
    }

    @Test
    void cabecalho_semLogotipo_naoMostraNada() {
        PdfPTable center = CompanyHeaderRenderer.build(company(), "Recibo", "RC 1", null, null,
                templateAt(DocumentLogoPosition.CENTER));
        assertEquals(1, center.getRows().size());
    }

    @Test
    void configService_devolveModeloGravado_ouOmissao() {
        DocumentColumnConfigRepository repository = mock(DocumentColumnConfigRepository.class);
        DocumentColumnConfig saved = new DocumentColumnConfig();
        saved.setCompanyId(1L);
        saved.setAccentColor(ACCENT);
        saved.setLogoPosition(DocumentLogoPosition.RIGHT);
        saved.setTemplateNotes(NOTES);
        saved.setShowQrCode(false);
        saved.getBankAccounts().add(new DocumentBankAccount("BCI", "Empresa Moçambique Lda", "MZ59"));
        when(repository.findByCompanyIdAndDocumentType(1L, DocumentType.COMMERCIAL)).thenReturn(Optional.of(saved));
        when(repository.findByCompanyIdAndDocumentType(1L, DocumentType.POS_RECEIPT)).thenReturn(Optional.empty());
        DocumentConfigService service = new DocumentConfigService(repository, mock(AuditLogService.class));

        assertEquals(new DocumentTemplateDTO(ACCENT, DocumentLogoPosition.RIGHT, NOTES,
                        List.of(new DocumentBankAccountDTO("BCI", "Empresa Moçambique Lda", "MZ59")), false),
                service.getTemplate(1L, DocumentType.COMMERCIAL));
        assertEquals(DocumentTemplateDTO.defaults(), service.getTemplate(1L, DocumentType.POS_RECEIPT));
    }

    /** DT-05: 2.ª via de uma factura emitida antes de o modelo existir sai com o modelo de hoje. */
    @Test
    void segundaVia_documentoAntigo_usaModeloActual() throws Exception {
        Invoice old = withLogo(invoice(client()));
        old.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 0));
        InvoiceLookup lookup = mock(InvoiceLookup.class);
        when(lookup.require(10L)).thenReturn(old);
        InvoicePrintService service = new InvoicePrintService(lookup, lineRowMapper, config);

        byte[] before = service.render(10L);
        when(config.getTemplate(eq(1L), any(DocumentType.class))).thenReturn(OTHER_TEMPLATE);
        byte[] after = service.render(10L);

        assertTemplatedA4(before);
        String text = text(after);
        assertTrue(text.contains("2.ª VIA — DUPLICADO"), "Indicação de 2.ª via em falta:\n" + text);
        assertTrue(text.contains(OTHER_NOTES), "2.ª via não usa as notas do modelo actual:\n" + text);
        assertFalse(text.contains(NOTES), "2.ª via ainda mostra as notas do modelo anterior");
        assertFalse(text.contains("BCI"), "2.ª via ainda mostra as contas do modelo anterior");
        assertTrue(content(after).contains(OTHER_ACCENT_FILL), "2.ª via não usa a cor do modelo actual");
        assertFalse(content(after).contains(ACCENT_FILL), "2.ª via ainda usa a cor do modelo anterior");
    }

    /** DT-05: mudar o modelo só muda o aspecto — o conteúdo fiscal da 2.ª via é o mesmo, palavra a palavra. */
    @Test
    void segundaVia_conteudoFiscalNaoMudaComOModelo() throws Exception {
        Invoice old = withLogo(invoice(client()));
        old.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 0));
        InvoiceLookup lookup = mock(InvoiceLookup.class);
        when(lookup.require(10L)).thenReturn(old);
        InvoicePrintService service = new InvoicePrintService(lookup, lineRowMapper, config);

        String withFirst = fiscalText(service.render(10L), NOTES, BANK_LINE);
        when(config.getTemplate(eq(1L), any(DocumentType.class))).thenReturn(OTHER_TEMPLATE);
        String withSecond = fiscalText(service.render(10L), OTHER_NOTES);

        assertEquals(withFirst, withSecond, "O conteúdo fiscal da 2.ª via mudou com o modelo");
        assertTrue(withSecond.contains("01/03/2024"), "Data de emissão original em falta:\n" + withSecond);
        assertTrue(withSecond.contains(EXEMPTION), "Motivo de isenção congelado em falta:\n" + withSecond);
        assertEquals(new BigDecimal("1660.00"), old.getTotalAmount(), "Imprimir não pode alterar o documento");
        assertEquals(new BigDecimal("160.00"), old.getTaxAmount(), "Imprimir não pode alterar o documento");
        assertEquals(LocalDateTime.of(2024, 3, 1, 9, 0), old.getCreatedAt(), "Imprimir não pode alterar o documento");
    }

    /** DT-05: as notas de crédito antigas também saem com o modelo actual em todas as vias. */
    @Test
    void segundaVia_notaDeCreditoAntiga_usaModeloActual() throws Exception {
        CreditNoteService notes = mock(CreditNoteService.class);
        var note = creditNote();
        note.setCompany(withLogo(note.getCompany()));
        note.setIssueDate(LocalDateTime.of(2024, 3, 2, 10, 0));
        when(notes.loadForPrint(100L)).thenReturn(note);
        when(config.getTemplate(eq(1L), any(DocumentType.class))).thenReturn(OTHER_TEMPLATE);

        byte[] pdf = new CreditNotePrintService(notes, lineRowMapper, config).render(100L);

        String text = text(pdf);
        assertTrue(text.contains("2.ª VIA — DUPLICADO"), "Indicação de 2.ª via em falta:\n" + text);
        assertTrue(text.contains("02/03/2024"), "Data de emissão original em falta:\n" + text);
        assertTrue(text.contains(OTHER_NOTES), "Nota de crédito antiga sem as notas do modelo actual");
        assertTrue(content(pdf).contains(OTHER_ACCENT_FILL), "Nota de crédito antiga sem a cor do modelo actual");
    }

    /** Texto da factura sem os fragmentos que pertencem ao modelo (notas e bloco bancário). */
    private static String fiscalText(byte[] pdf, String... templateFragments) throws Exception {
        String text = text(pdf);
        for (String fragment : templateFragments) {
            text = text.replace(fragment, "");
        }
        return text.replace("DADOS BANCÁRIOS", "").replaceAll("\\s+", " ").trim();
    }

    private static void assertTemplatedA4(byte[] pdf) throws Exception {
        String text = text(pdf);
        assertTrue(text.contains(NOTES), "Notas do modelo em falta:\n" + text);
        assertTrue(text.contains(BANK_LINE), "Conta bancária do modelo em falta:\n" + text);
        String content = content(pdf);
        assertTrue(content.contains(ACCENT_FILL), "Cor de destaque do modelo não aplicada");
        assertTrue(content.contains(" Do"), "Logótipo da empresa não impresso");
    }

    private static void assertTemplatedThermal(byte[] pdf) throws Exception {
        String text = text(pdf);
        assertTrue(text.contains(NOTES), "Notas do modelo em falta no talão:\n" + text);
        assertTrue(text.contains("Volte sempre!"), "Rodapé do modelo em falta no talão:\n" + text);
        assertFalse(text.contains("BCI"), "Talão térmico não leva dados bancários");
        assertFalse(text.contains("DADOS BANCÁRIOS"), "Talão térmico não leva dados bancários");
        String content = content(pdf);
        assertFalse(content.contains(ACCENT_FILL), "Talão térmico não usa cor de destaque");
        assertTrue(content.contains(" Do"), "Logótipo da empresa não impresso no talão");
    }

    private static DocumentTemplateDTO templateAt(DocumentLogoPosition position) {
        return new DocumentTemplateDTO(null, position, null, List.of(), true);
    }

    private static DocumentColumnsDTO columnsWithFooter(String footer) {
        return new DocumentColumnsDTO(true, true, true, true, true, true, true, true, true, true, true, footer);
    }

    /**
     * Primeiro elemento da célula; o OpenPDF embrulha imagens numa tabela de uma célula, por isso
     * essa tabela conta como a imagem que contém.
     */
    private static Element firstElement(PdfPCell cell) {
        if (cell.getCompositeElements() == null || cell.getCompositeElements().isEmpty()) return null;
        Element first = (Element) cell.getCompositeElements().get(0);
        if (first instanceof PdfPTable wrapper && wrapper.getRows().size() == 1) {
            PdfPCell inner = wrapper.getRow(0).getCells()[0];
            if (inner != null && inner.getImage() != null) return inner.getImage();
        }
        return first;
    }

    private static Invoice withLogo(Invoice invoice) {
        invoice.setCompany(withLogo(invoice.getCompany()));
        return invoice;
    }

    private static Company withLogo(Company company) {
        try {
            BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            company.setLogo(out.toByteArray());
            return company;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Texto extraído com quebras de linha normalizadas (blocos estreitos partem linhas longas). */
    private static String text(byte[] pdf) throws Exception {
        return extract(pdf).replaceAll("\\s+", " ");
    }

    /** Operadores de desenho de todas as páginas (cores de preenchimento, imagens). */
    private static String content(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder content = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                content.append(new String(reader.getPageContent(page), StandardCharsets.ISO_8859_1)).append('\n');
            }
            return content.toString();
        } finally {
            reader.close();
        }
    }
}
