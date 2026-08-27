package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Bloco "Condições" partilhado pela Cotação e pela Encomenda A4 — pagamento, prazo de entrega,
 * data prevista e observações.
 *
 * <p>São o mesmo bloco porque são o mesmo acordo: a encomenda herda da cotação exactamente estas
 * condições. Escrevê-lo duas vezes era garantir que um dia divergiam.
 *
 * <p>Só sai o que estiver preenchido — condições em branco não ocupam espaço no documento, que é o
 * que mantém limpo o A4 de quem cria encomendas à mão, sem acordo prévio.
 */
public final class CommercialTermsRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CommercialTermsRenderer() {}

    /**
     * @return o bloco, ou {@code null} quando não há nada para mostrar — o chamador não adiciona
     *         nada nesse caso (uma tabela vazia deixaria um espaço morto no documento).
     */
    public static PdfPTable build(String paymentTerms, String deliveryTerms,
                                   LocalDate expectedDeliveryDate, String notes) {
        if (!hasAny(paymentTerms, deliveryTerms, notes) && expectedDeliveryDate == null) {
            return null;
        }
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.addElement(new Paragraph("Condições", PdfTheme.subtitleFont()));
        addIfPresent(cell, "Pagamento: ", paymentTerms);
        addIfPresent(cell, "Prazo de entrega: ", deliveryTerms);
        if (expectedDeliveryDate != null) {
            cell.addElement(new Paragraph("Entrega prevista: " + expectedDeliveryDate.format(DATE_FMT),
                    PdfTheme.boldFont()));
        }
        addIfPresent(cell, "Observações: ", notes);
        table.addCell(cell);
        return table;
    }

    private static void addIfPresent(PdfPCell cell, String label, String value) {
        if (notBlank(value)) {
            cell.addElement(new Paragraph(label + value, PdfTheme.bodyFont()));
        }
    }

    private static boolean hasAny(String... values) {
        for (String v : values) {
            if (notBlank(v)) return true;
        }
        return false;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
