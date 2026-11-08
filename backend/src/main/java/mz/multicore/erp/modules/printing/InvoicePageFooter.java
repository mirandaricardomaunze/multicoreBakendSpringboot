package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import mz.multicore.erp.modules.company.model.Company;

import java.util.List;

/** Rodapé fiscal e identificação da via em cada página da factura A4. */
public class InvoicePageFooter extends PdfPageEventHelper {
    private final Company company;
    private final List<String> copies;
    private final String documentNumber;
    private int copyIndex;
    private int copyFirstPage;

    public InvoicePageFooter(Company company, List<String> copies, String documentNumber) {
        this.company = company;
        this.copies = copies;
        this.documentNumber = documentNumber;
    }

    /** Chamar depois de abrir a primeira página da via: uma via pode ocupar várias páginas. */
    void startCopy(int index) {
        copyIndex = index;
        copyFirstPage = 0;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (copyFirstPage == 0) copyFirstPage = writer.getPageNumber();
        int page = writer.getPageNumber() - copyFirstPage + 1;
        String simple = simpleCopy(copies.get(copyIndex));
        PdfContentByte canvas = writer.getDirectContent();
        float left = document.left();
        float right = document.right();
        float bottom = document.bottom() - 17f;
        if (page > 1) {
            show(canvas, Element.ALIGN_RIGHT, "Fatura " + documentNumber + " · " + simple,
                    right, document.top() + 8f, 8f);
        }
        canvas.setColorStroke(PdfTheme.BORDER);
        canvas.setLineWidth(0.5f);
        canvas.moveTo(left, bottom + 16f);
        canvas.lineTo(right, bottom + 16f);
        canvas.stroke();

        String name = company != null && company.getName() != null ? company.getName() : "Multicore ERP";
        String taxId = company != null && company.getTaxId() != null ? company.getTaxId() : "";
        String address = company != null && company.formattedAddress() != null
                ? company.formattedAddress() : "Maputo, Moçambique";
        show(canvas, Element.ALIGN_LEFT, "Processado por computador · Multicore ERP",
                left, bottom + 6f, 7.2f);
        show(canvas, Element.ALIGN_LEFT, name + " · NUIT " + taxId + " · " + address,
                left, bottom - 3f, 7.2f);
        show(canvas, Element.ALIGN_RIGHT, simple + " · Página " + page, right, bottom + 6f, 7.5f);
    }

    private void show(PdfContentByte canvas, int alignment, String text, float x, float y, float size) {
        ColumnText.showTextAligned(canvas, alignment,
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, size, PdfTheme.MUTED)), x, y, 0);
    }

    private String simpleCopy(String label) {
        if (label.contains("ORIGINAL")) return "Original";
        if (label.contains("DUPLICADO")) return "Duplicado";
        if (label.contains("TRIPLICADO")) return "Triplicado";
        if (label.contains("QUADRUPLICADO")) return "Quadruplicado";
        return label;
    }
}
