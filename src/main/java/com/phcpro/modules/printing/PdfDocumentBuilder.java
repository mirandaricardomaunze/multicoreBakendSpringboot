package com.phcpro.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Lifecycle helper for any PDF: opens a Document, adds a footer, runs the
 * caller's body, and returns the resulting bytes. Removes try/finally noise
 * from every print service (DRY).
 */
public final class PdfDocumentBuilder {

    private static final DateTimeFormatter FOOTER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PdfDocumentBuilder() {}

    public static byte[] build(Rectangle pageSize, Consumer<Document> body) {
        Document document = new Document(pageSize,
                PdfTheme.MARGIN_LEFT, PdfTheme.MARGIN_RIGHT,
                PdfTheme.MARGIN_TOP, PdfTheme.MARGIN_BOTTOM);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new FooterStamp());
            document.open();
            body.accept(document);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new PdfGenerationException("Falha a gerar PDF: " + e.getMessage(), e);
        }
    }

    public static byte[] buildA4(Consumer<Document> body) {
        return build(PageSize.A4, body);
    }

    /** 80mm × 297mm thermal-style receipt strip. */
    public static byte[] buildReceipt(Consumer<Document> body) {
        Rectangle r = new Rectangle(226f, 841f); // 80mm wide × A4 tall (trim by content)
        return build(r, body);
    }

    public static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", PdfTheme.bodyFont());
        p.setSpacingAfter(height);
        return p;
    }

    /** Page-level footer with "Gerado em ..." and the page number. */
    private static final class FooterStamp extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer, Document document) {
            String stamp = "Gerado em " + LocalDateTime.now().format(FOOTER_DATE) + " — Página " + writer.getPageNumber();
            Paragraph p = new Paragraph(stamp, PdfTheme.smallFont());
            p.setAlignment(Element.ALIGN_RIGHT);
            try {
                com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_RIGHT,
                        new com.lowagie.text.Phrase(stamp, PdfTheme.smallFont()),
                        document.right(),
                        document.bottom() - 12,
                        0
                );
            } catch (Exception ignored) {}
        }
    }
}
