package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.customorders.dto.CustomOrderDTO;
import mz.multicore.erp.modules.customorders.dto.CustomOrderLineDTO;
import mz.multicore.erp.modules.customorders.model.DeliveryMode;
import mz.multicore.erp.modules.customorders.service.CustomOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Kitchen sheet of a custom order: when and how it leaves, what to write, the reference photo and the items. An
 * internal document for the bakery or butcher bench; the commercial document remains the order.
 */
@Service
public class CustomOrderSheetPrintService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new java.util.Locale("pt", "MZ"));

    private final CustomOrderService customOrderService;

    public CustomOrderSheetPrintService(CustomOrderService customOrderService) {
        this.customOrderService = customOrderService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long orderId) {
        CustomOrderService.Sheet sheet = customOrderService.sheet(orderId, companyId);
        CustomOrderDTO view = sheet.view();
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(sheet.order().getCompany(), "Ficha de Encomenda", view.orderNumber(), null));
            doc.add(info(view));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(lines(view));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(new Paragraph("Personalização", PdfTheme.boldFont()));
            doc.add(new Paragraph(view.personalization() == null ? "Sem personalização." : view.personalization(), PdfTheme.bodyFont()));
            byte[] image = sheet.customization() == null ? null : sheet.customization().getImageData();
            if (image != null && image.length > 0) {
                try {
                    Image photo = Image.getInstance(image);
                    photo.scaleToFit(320f, 260f);
                    photo.setAlignment(Element.ALIGN_CENTER);
                    doc.add(PdfDocumentBuilder.spacer(8f));
                    doc.add(new Paragraph("Fotografia de referência", PdfTheme.boldFont()));
                    doc.add(photo);
                } catch (Exception unreadable) {
                    doc.add(new Paragraph("Fotografia de referência não legível.", PdfTheme.smallFont()));
                }
            }
            doc.add(PdfDocumentBuilder.spacer(30f));
            doc.add(SignatureBlockRenderer.build("Preparado por", "Entregue a (nome, assinatura e data)"));
        });
    }

    private static PdfPTable info(CustomOrderDTO v) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{18f, 32f, 18f, 32f});
        } catch (Exception ignored) {
            // Widths are cosmetic; the default split is still readable.
        }
        pair(table, "Cliente", v.clientName());
        pair(table, "Contacto", v.contactPhone());
        pair(table, "Entrega", v.deliveryDate() == null ? null : DATE.format(v.deliveryDate()));
        pair(table, "Hora", v.deliveryTime() == null ? "Sem hora marcada" : v.deliveryTime().toString());
        pair(table, "Modo", v.mode() == DeliveryMode.DELIVERY ? "Entrega na morada" : "Levanta na loja");
        pair(table, "Morada", v.deliveryAddress());
        pair(table, "Total", DisplayNumbers.money(v.total()) + " MT");
        pair(table, "Sinal pago", DisplayNumbers.money(v.advancePaid()) + " MT");
        return table;
    }

    private static PdfPTable lines(CustomOrderDTO v) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{80f, 20f});
        } catch (Exception ignored) {
            // Widths are cosmetic; the default split is still readable.
        }
        for (String header : new String[]{"Artigo", "Quantidade"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PdfTheme.tableHeaderFont()));
            cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
            cell.setBorderColor(PdfTheme.BORDER);
            cell.setPadding(5f);
            table.addCell(cell);
        }
        for (CustomOrderLineDTO line : v.lines()) {
            table.addCell(cell(line.productName(), Element.ALIGN_LEFT));
            table.addCell(cell(DisplayNumbers.qty(line.quantity()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private static void pair(PdfPTable table, String label, String value) {
        PdfPCell key = new PdfPCell(new Phrase(label, PdfTheme.smallFont()));
        key.setBorderColor(PdfTheme.BORDER);
        key.setPadding(4f);
        table.addCell(key);
        PdfPCell val = new PdfPCell(new Phrase(value == null || value.isBlank() ? "—" : value, PdfTheme.boldFont()));
        val.setBorderColor(PdfTheme.BORDER);
        val.setPadding(4f);
        table.addCell(val);
    }

    private static PdfPCell cell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, PdfTheme.tableBodyFont()));
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(4f);
        return cell;
    }
}
