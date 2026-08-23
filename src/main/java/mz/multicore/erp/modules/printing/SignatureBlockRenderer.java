package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Bloco de assinaturas dos documentos A4: duas linhas, com o rótulo de cada lado.
 *
 * <p>Estava escrito <b>duas vezes</b> — {@code DeliveryGuidePrintService} e
 * {@code QuotationPrintService}, o mesmo {@code signatureCell} copiado — e a encomenda profissional
 * ia escrevê-lo uma terceira. É a forma exacta do defeito que o {@link ClientBlockRenderer} já
 * corrigiu: documentos iguais por coincidência de manutenção, não por construção.
 *
 * <p>Só os rótulos mudam entre documentos, porque só isso é que muda mesmo:
 * <ul>
 *   <li>Guia de Remessa — "O Expedidor" / "Recebi em conformidade (Destinatário)"</li>
 *   <li>Cotação — "Pela empresa" / "Aceite pelo cliente (nome, assinatura e data)"</li>
 *   <li>Encomenda — "Pela empresa" / "Confirmação do cliente"</li>
 * </ul>
 */
public final class SignatureBlockRenderer {

    private SignatureBlockRenderer() {}

    public static PdfPTable build(String leftLabel, String rightLabel) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell(leftLabel));
        table.addCell(signatureCell(rightLabel));
        return table;
    }

    private static PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);
        cell.addElement(new Paragraph("____________________________", PdfTheme.bodyFont()));
        cell.addElement(new Paragraph(label, PdfTheme.smallFont()));
        return cell;
    }
}
