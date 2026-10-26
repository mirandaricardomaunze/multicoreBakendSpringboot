package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Construtor dos blocos visuais de rota, armazéns e transportador para documentos de transferência.
 */
@Component
public class StockTransferPrintRouteBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PdfPTable buildHorizontalRule() {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingAfter(10f);
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderColor(PdfTheme.BRAND);
        cell.setBorderWidthBottom(1.2f);
        cell.setFixedHeight(2f);
        rule.addCell(cell);
        return rule;
    }

    public PdfPTable buildProfessionalRouteBlock(StockTransfer transfer) {
        PdfPTable table = new PdfPTable(new float[]{25f, 25f, 22f, 28f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(8f);

        table.addCell(infoCell("ARMAZÉM DE ORIGEM",
                transfer.getOriginWarehouse().getName(),
                transfer.getOriginWarehouse().getLocation(),
                "Responsável: " + valueOrLine(transfer.getResponsible())));
        table.addCell(infoCell("ARMAZÉM DE DESTINO",
                transfer.getDestinationWarehouse().getName(),
                transfer.getDestinationWarehouse().getLocation(),
                "Responsável: ____________________"));
        table.addCell(infoCell("DATA DE EMISSÃO",
                transfer.getTransferDate().format(DATE_FMT),
                "ESTADO",
                transfer.getStatus().getLabel()));
        table.addCell(infoCell("TRANSPORTADOR",
                valueOrLine(transfer.getDriverName()),
                "MATRÍCULA",
                valueOrLine(firstNonBlank(transfer.getVehiclePlate(), transfer.getVehicle()))));
        return table;
    }

    public PdfPTable buildPickingRouteBlock(StockTransfer transfer) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell origin = new PdfPCell();
        origin.setBorder(PdfPCell.NO_BORDER);
        origin.addElement(new Paragraph("Local de Recolha (Origem)", PdfTheme.subtitleFont()));
        origin.addElement(new Paragraph(transfer.getOriginWarehouse().getName(), PdfTheme.boldFont()));
        if (transfer.getOriginWarehouse().getLocation() != null) {
            origin.addElement(new Paragraph(transfer.getOriginWarehouse().getLocation(), PdfTheme.smallFont()));
        }
        if (transfer.getDriverName() != null && !transfer.getDriverName().isBlank()) {
            origin.addElement(new Paragraph("Motorista Previsto: " + transfer.getDriverName(), PdfTheme.bodyFont()));
        }
        if (transfer.getVehiclePlate() != null && !transfer.getVehiclePlate().isBlank()) {
            origin.addElement(new Paragraph("Viatura Prevista: " + transfer.getVehiclePlate(), PdfTheme.bodyFont()));
        }
        table.addCell(origin);

        PdfPCell dest = new PdfPCell();
        dest.setBorder(PdfPCell.NO_BORDER);
        dest.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph destTitle = new Paragraph("Armazém de Destino", PdfTheme.subtitleFont());
        destTitle.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(destTitle);
        Paragraph destName = new Paragraph(transfer.getDestinationWarehouse().getName(), PdfTheme.boldFont());
        destName.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(destName);
        Paragraph date = new Paragraph("Data Emissão: " + transfer.getTransferDate().format(DATE_FMT), PdfTheme.bodyFont());
        date.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(date);
        Paragraph status = new Paragraph("Estado: " + transfer.getStatus().getLabel(), PdfTheme.bodyFont());
        status.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(status);
        table.addCell(dest);

        return table;
    }

    private PdfPCell infoCell(String label, String value, String secondaryLabel, String secondaryValue) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingRight(12f);
        cell.addElement(new Paragraph(label, PdfTheme.tableHeaderFont()));
        cell.addElement(new Paragraph(valueOrLine(value), PdfTheme.boldFont()));
        if (secondaryLabel != null && !secondaryLabel.isBlank()) {
            cell.addElement(new Paragraph(secondaryLabel, PdfTheme.tableHeaderFont()));
        }
        if (secondaryValue != null && !secondaryValue.isBlank()) {
            cell.addElement(new Paragraph(secondaryValue, PdfTheme.bodyFont()));
        }
        return cell;
    }

    public static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    public static String valueOrLine(String value) {
        return value == null || value.isBlank() ? "____________________" : value;
    }
}
