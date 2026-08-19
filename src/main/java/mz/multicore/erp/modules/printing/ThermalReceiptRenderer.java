package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import mz.multicore.erp.modules.company.model.Company;

/**
 * O que faz de um talão de 80 mm um talão: cabeçalho da empresa, separadores pontilhados e
 * células com borda inferior a pontilhado.
 *
 * <p>Existiam <b>dois</b> documentos térmicos no sistema, escritos em alturas diferentes, e
 * tinham divergido: o recibo do POS levava logótipo, NUIT, morada, telefone, email e separadores
 * pontilhados; a guia de separação não levava nada disso — saía com uma tabela de bordas simples
 * e sem qualquer identificação da empresa. Duas impressoras térmicas na mesma loja a cuspir dois
 * desenhos diferentes.
 *
 * <p>O equivalente térmico do {@link CompanyHeaderRenderer}, que faz o mesmo pelos A4.
 */
public final class ThermalReceiptRenderer {

    private ThermalReceiptRenderer() {}

    /**
     * Cabeçalho: logótipo, nome, NUIT, morada, telefone, email e separador.
     *
     * <p>À prova de falha — sem logótipo, com logótipo ilegível ou sem qualquer contacto, o talão
     * sai na mesma. Um documento operacional nunca deve deixar de ser impresso por causa da
     * decoração.
     */
    public static void companyHeader(Document doc, Company company) {
        logo(doc, company);
        centered(doc, company == null ? "Empresa" : company.getName(), PdfTheme.subtitleFont());
        if (company != null) {
            if (company.getTaxId() != null) centered(doc, "NUIT: " + company.getTaxId(), PdfTheme.smallFont());
            if (notBlank(company.getAddress())) centered(doc, company.getAddress(), PdfTheme.smallFont());
            if (notBlank(company.getPhone())) centered(doc, "Tel: " + company.getPhone(), PdfTheme.smallFont());
            if (notBlank(company.getEmail())) centered(doc, company.getEmail(), PdfTheme.smallFont());
        }
        dottedLine(doc);
    }

    /** Linha separadora pontilhada a toda a largura. */
    public static void dottedLine(Document doc) {
        DottedLineSeparator separator = new DottedLineSeparator();
        separator.setGap(2.2f);
        separator.setLineWidth(0.7f);
        separator.setLineColor(PdfTheme.BORDER);
        Paragraph paragraph = new Paragraph();
        paragraph.setSpacingBefore(2f);
        paragraph.setSpacingAfter(2f);
        paragraph.add(new Chunk(separator));
        doc.add(paragraph);
    }

    public static void centered(Document doc, String text, Font font) {
        aligned(doc, text, font, Element.ALIGN_CENTER);
    }

    public static void right(Document doc, String text, Font font) {
        aligned(doc, text, font, Element.ALIGN_RIGHT);
    }

    /** Célula sem bordas laterais, com a borda inferior pontilhada característica do talão. */
    public static void cell(PdfPTable table, String text, Font font, int alignment, boolean header) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);   // a borda inferior é desenhada pelo evento
        cell.setCellEvent(DOTTED_BOTTOM);
        cell.setPadding(3f);
        if (header) cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        table.addCell(cell);
    }

    /** Desenha a borda inferior de cada célula a pontilhado. */
    public static final PdfPCellEvent DOTTED_BOTTOM = (cell, position, canvases) -> {
        PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
        canvas.saveState();
        canvas.setLineWidth(0.6f);
        canvas.setLineDash(1f, 2f, 0f);
        canvas.setColorStroke(PdfTheme.BORDER);
        canvas.moveTo(position.getLeft(), position.getBottom());
        canvas.lineTo(position.getRight(), position.getBottom());
        canvas.stroke();
        canvas.restoreState();
    };

    private static void aligned(Document doc, String text, Font font, int alignment) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        doc.add(paragraph);
    }

    private static void logo(Document doc, Company company) {
        if (company == null || company.getLogo() == null || company.getLogo().length == 0) return;
        try {
            com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(company.getLogo());
            logo.scaleToFit(160f, 60f);
            logo.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
            doc.add(logo);
        } catch (Exception ignored) {
            // logótipo ilegível — o talão sai na mesma, sem imagem.
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
