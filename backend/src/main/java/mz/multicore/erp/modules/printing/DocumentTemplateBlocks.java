package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;

import java.awt.Color;
import java.util.List;

/**
 * Peças do modelo de documento da empresa (MODELOS_DOCUMENTO_SPEC §3.3, DT-04), partilhadas por todos os
 * documentos fiscais: cor de destaque, posição do logótipo, notas livres e bloco de dados bancários nos A4;
 * só logótipo, notas e rodapé nos talões térmicos. Nunca toca em conteúdo fiscal — só no aspecto.
 */
final class DocumentTemplateBlocks {

    private DocumentTemplateBlocks() {}

    /** Caixa máxima do logótipo, em pontos PDF: cabeçalho normal ou cabeçalho compacto da factura A4. */
    enum LogoSize {
        HEADER(150, 50),
        COMPACT(140, 45);

        private final int maxWidth;
        private final int maxHeight;

        LogoSize(int maxWidth, int maxHeight) {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }
    }

    /** Modelo efectivo: sem configuração (ou serviço simulado sem resposta) vale o modelo por omissão. */
    static DocumentTemplateDTO orDefault(DocumentTemplateDTO template) {
        return template == null ? DocumentTemplateDTO.defaults() : template;
    }

    /** Cor de destaque do modelo; sem cor própria, a cor actual do {@link PdfTheme}. */
    static Color accent(DocumentTemplateDTO template) {
        return PdfTheme.accentOrDefault(orDefault(template).accentColor());
    }

    static DocumentLogoPosition logoPosition(DocumentTemplateDTO template) {
        DocumentLogoPosition position = orDefault(template).logoPosition();
        return position == null ? DocumentLogoPosition.LEFT : position;
    }

    /**
     * Linha do logótipo centrado, a toda a largura do cabeçalho de {@code columns} colunas; só quando o
     * modelo pede {@code CENTER} e a empresa tem logótipo legível.
     */
    static void addCenteredLogoRow(PdfPTable header, int columns, Company company, DocumentTemplateDTO template,
                                   LogoSize size) {
        if (logoPosition(template) != DocumentLogoPosition.CENTER) return;
        Image logo = logo(company, size, Image.ALIGN_CENTER);
        if (logo == null) return;
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setColspan(columns);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.addElement(logo);
        header.addCell(cell);
    }

    /** Logótipo dentro da célula da empresa ({@code LEFT}) ou da célula do documento ({@code RIGHT}). */
    static void addLogoIfAt(PdfPCell cell, DocumentLogoPosition wanted, Company company, DocumentTemplateDTO template,
                            LogoSize size) {
        if (logoPosition(template) != wanted) return;
        int alignment = wanted == DocumentLogoPosition.RIGHT ? Image.ALIGN_RIGHT : Image.ALIGN_LEFT;
        Image logo = logo(company, size, alignment);
        if (logo != null) cell.addElement(logo);
    }

    /** Logótipo escalado; sem logótipo ou com bytes ilegíveis devolve {@code null} e o documento sai na mesma. */
    static Image logo(Company company, LogoSize size, int alignment) {
        if (company == null || company.getLogo() == null || company.getLogo().length == 0) return null;
        try {
            Image logo = Image.getInstance(company.getLogo());
            logo.scaleToFit(size.maxWidth, size.maxHeight);
            logo.setAlignment(alignment);
            logo.setSpacingAfter(4f);
            return logo;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Notas livres do modelo, impressas antes do rodapé; {@code null} quando não há notas. */
    static Paragraph notes(DocumentTemplateDTO template) {
        String notes = orDefault(template).notes();
        if (notes == null || notes.isBlank()) return null;
        Paragraph paragraph = new Paragraph(notes.trim(), PdfTheme.bodyFont());
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(4f);
        return paragraph;
    }

    /** Bloco "DADOS BANCÁRIOS" com as contas do modelo; {@code null} quando o modelo não tem contas. */
    static PdfPTable bankAccounts(DocumentTemplateDTO template) {
        List<String> lines = bankAccountLines(template);
        if (lines.isEmpty()) return null;
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.TOP);
        cell.setBorderColor(accent(template));
        cell.setPaddingTop(4f);
        cell.addElement(new Paragraph("DADOS BANCÁRIOS",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, accent(template))));
        lines.forEach(line -> cell.addElement(new Paragraph(line, PdfTheme.bodyFont())));
        table.addCell(cell);
        return table;
    }

    /** Fecho dos A4 que não têm bloco de pagamento próprio: notas do modelo e depois o bloco bancário. */
    static void addA4Closing(Document doc, DocumentTemplateDTO template) {
        Paragraph notes = notes(template);
        if (notes != null) doc.add(notes);
        PdfPTable banks = bankAccounts(template);
        if (banks != null) doc.add(banks);
    }

    /** Uma linha por conta do modelo: banco, titular e NIB/IBAN. */
    static List<String> bankAccountLines(DocumentTemplateDTO template) {
        return orDefault(template).bankAccounts().stream().map(DocumentTemplateBlocks::bankLine).toList();
    }

    /** Notas do modelo num talão térmico (sem cor nem dados bancários — SPEC §3.3). */
    static void thermalNotes(Document doc, DocumentTemplateDTO template) {
        String notes = orDefault(template).notes();
        if (notes == null || notes.isBlank()) return;
        for (String line : notes.trim().split("\\r?\\n")) {
            ThermalReceiptRenderer.centered(doc, line, PdfTheme.smallFont());
        }
    }

    private static String bankLine(DocumentBankAccountDTO account) {
        return account.bankName() + " · Titular: " + account.holder() + " · NIB/IBAN: " + account.accountNumber();
    }
}
