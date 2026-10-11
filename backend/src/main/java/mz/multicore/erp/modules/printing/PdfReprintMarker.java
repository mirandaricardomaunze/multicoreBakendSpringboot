package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

final class PdfReprintMarker {
    private PdfReprintMarker() {}

    static byte[] mark(byte[] source, int copyNumber) {
        try {
            PdfReader reader = new PdfReader(source);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, output);
            BaseFont font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
            String label = "REIMPRESSAO - " + copyNumber + ".a VIA";
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                var size = reader.getPageSizeWithRotation(page);
                PdfContentByte canvas = stamper.getOverContent(page);
                PdfGState opacity = new PdfGState();
                opacity.setFillOpacity(0.17f);
                canvas.saveState();
                canvas.setGState(opacity);
                canvas.setColorFill(new Color(185, 28, 28));
                canvas.beginText();
                canvas.setFontAndSize(font, 34f);
                canvas.showTextAligned(Element.ALIGN_CENTER, label,
                        size.getWidth() / 2f, size.getHeight() / 2f, 35f);
                canvas.endText();
                canvas.restoreState();
            }
            stamper.close();
            reader.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível marcar a segunda via do documento.", ex);
        }
    }
}
