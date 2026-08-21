package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bloco "Cliente" dos documentos A4: identificação do cliente à esquerda, data e armazém à direita.
 *
 * <p>Existia escrito duas vezes — uma em {@code InvoicePrintService}, outra em
 * {@code OrderPrintService}, ~35 linhas idênticas. A encomenda A4 era igual à fatura por
 * coincidência de manutenção, não por construção; bastava alguém mexer num e esquecer o outro.
 * É a forma exacta do defeito que este projecto já apanhou três vezes — <b>a mesma regra em duas
 * portas</b>. Agora é uma só.
 */
public final class ClientBlockRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ClientBlockRenderer() {}

    /** Sem nome livre — o documento identifica o comprador pelo cliente registado. */
    public static PdfPTable build(Client client, LocalDateTime date, Warehouse warehouse) {
        return build(client, null, date, warehouse);
    }

    /**
     * @param buyerLabel nome escrito à mão para um comprador sem registo ({@code walkInName} na
     *        encomenda e na cotação, {@code customerName} na fatura). Quando existe, é <b>este</b>
     *        o nome impresso: foi o que o operador escreveu, e é a esta pessoa que o documento se
     *        dirige. Vazio ou nulo cai no cliente registado.
     */
    public static PdfPTable build(Client client, String buyerLabel, LocalDateTime date, Warehouse warehouse) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        table.addCell(clientCell(client, buyerLabel));
        table.addCell(metaCell(date, warehouse));
        return table;
    }

    private static PdfPCell clientCell(Client client, String buyerLabel) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.addElement(new Paragraph("Cliente", PdfTheme.subtitleFont()));

        String name = buyerLabel != null && !buyerLabel.isBlank()
                ? buyerLabel.trim()
                : (client == null ? null : client.getName());
        if (name != null && !name.isBlank()) {
            cell.addElement(new Paragraph(name, PdfTheme.bodyFont()));
        }
        if (client != null) {
            if (client.getTaxId() != null) {
                cell.addElement(new Paragraph("NUIT: " + client.getTaxId(), PdfTheme.bodyFont()));
            }
            if (client.getAddress() != null) {
                cell.addElement(new Paragraph(client.getAddress(), PdfTheme.bodyFont()));
            }
        }
        return cell;
    }

    private static PdfPCell metaCell(LocalDateTime date, Warehouse warehouse) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (date != null) {
            cell.addElement(rightAligned("Data: " + date.format(DATE_FMT)));
        }
        if (warehouse != null) {
            cell.addElement(rightAligned("Armazém: " + warehouse.getName()));
        }
        return cell;
    }

    private static Paragraph rightAligned(String text) {
        Paragraph paragraph = new Paragraph(text, PdfTheme.bodyFont());
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        return paragraph;
    }
}
