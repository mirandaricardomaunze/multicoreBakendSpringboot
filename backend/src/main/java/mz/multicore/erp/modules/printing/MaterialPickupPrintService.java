package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.pickup.dto.MaterialPickupLineDTO;
import mz.multicore.erp.modules.pickup.dto.PickupAccountDTO;
import mz.multicore.erp.modules.pickup.dto.PickupBalanceLineDTO;
import mz.multicore.erp.modules.pickup.model.MaterialPickup;
import mz.multicore.erp.modules.pickup.model.MaterialPickupStatus;
import mz.multicore.erp.modules.pickup.service.PickupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Pickup slip (LV) handed to the gate keeper: what leaves the yard now and what the client still has to
 * collect. It is an internal delivery document; the fiscal document remains the source invoice.
 */
@Service
public class MaterialPickupPrintService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PickupService pickupService;

    public MaterialPickupPrintService(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long pickupId) {
        PickupService.PickupSlip slip = pickupService.slip(companyId, pickupId);
        MaterialPickup pickup = slip.pickup();
        PickupAccountDTO account = slip.account();
        String status = pickup.getStatus() == MaterialPickupStatus.CANCELLED ? "ANULADO" : null;

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(pickup.getCompany(), "Talão de Levantamento", pickup.getNumber(), status));
            doc.add(infoTable(pickup, account));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(linesTable(pickup, account));
            doc.add(PdfDocumentBuilder.spacer(8f));
            Paragraph note = new Paragraph("Documento interno de entrega de material já faturado. Não substitui a fatura "
                    + account.summary().invoiceNumber() + ". O material por levantar continua à guarda da empresa.",
                    PdfTheme.smallFont());
            doc.add(note);
            if (status != null && pickup.getCancelReason() != null) {
                doc.add(new Paragraph("Motivo da anulação: " + pickup.getCancelReason(), PdfTheme.voidStampFont()));
            }
            doc.add(PdfDocumentBuilder.spacer(36f));
            doc.add(SignatureBlockRenderer.build("O Fiel de Armazém", "Recebi o material (nome, assinatura e data)"));
        });
    }

    private static PdfPTable infoTable(MaterialPickup pickup, PickupAccountDTO account) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{18f, 32f, 18f, 32f});
        } catch (Exception ignored) {
            // Widths are cosmetic; the default split is still readable.
        }
        addPair(table, "Cliente", account.summary().clientName());
        addPair(table, "Fatura", account.summary().invoiceNumber());
        addPair(table, "Levantado por", pickup.getCollectorName());
        addPair(table, "Documento", pickup.getCollectorDocument());
        addPair(table, "Viatura", pickup.getVehiclePlate());
        addPair(table, "Data", pickup.getPickedAt() == null ? null : DATE_TIME.format(pickup.getPickedAt()));
        addPair(table, "Armazém", account.summary().warehouseName());
        addPair(table, "Emitido por", pickup.getCreatedBy());
        return table;
    }

    private static PdfPTable linesTable(MaterialPickup pickup, PickupAccountDTO account) {
        Map<Long, BigDecimal> balanceAfter = new HashMap<>();
        for (PickupBalanceLineDTO line : account.lines()) balanceAfter.put(line.invoiceLineId(), line.balance());

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{16f, 44f, 10f, 15f, 15f});
        } catch (Exception ignored) {
            // Widths are cosmetic; the default split is still readable.
        }
        for (String header : new String[]{"Código", "Artigo", "Unid.", "Levantado agora", "Por levantar (à impressão)"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PdfTheme.tableHeaderFont()));
            cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
            cell.setBorderColor(PdfTheme.BORDER);
            cell.setPadding(5f);
            table.addCell(cell);
        }
        for (MaterialPickupLineDTO line : slipLines(pickup)) {
            table.addCell(body(line.productSku(), Element.ALIGN_LEFT, PdfTheme.tableBodyFont()));
            table.addCell(body(line.productName(), Element.ALIGN_LEFT, PdfTheme.tableBodyFont()));
            table.addCell(body(line.unitCode() == null ? "" : line.unitCode().toLowerCase(), Element.ALIGN_CENTER, PdfTheme.tableBodyFont()));
            table.addCell(body(DisplayNumbers.qty(line.quantity()), Element.ALIGN_RIGHT, PdfTheme.boldFont()));
            table.addCell(body(DisplayNumbers.qty(balanceAfter.getOrDefault(line.invoiceLineId(), BigDecimal.ZERO)),
                    Element.ALIGN_RIGHT, PdfTheme.tableBodyFont()));
        }
        return table;
    }

    private static java.util.List<MaterialPickupLineDTO> slipLines(MaterialPickup pickup) {
        return pickup.getLines().stream().map(l -> new MaterialPickupLineDTO(l.getInvoiceLine().getId(),
                l.getInvoiceLine().getProduct().getSku(), l.getInvoiceLine().getProduct().getName(),
                mz.multicore.erp.modules.units.service.ProductUnitService.baseUnitCodeOf(l.getInvoiceLine().getProduct()),
                l.getQuantity())).toList();
    }

    private static void addPair(PdfPTable table, String label, String value) {
        PdfPCell key = new PdfPCell(new Phrase(label, PdfTheme.smallFont()));
        key.setBorder(PdfPCell.NO_BORDER);
        key.setPadding(3f);
        table.addCell(key);
        PdfPCell val = new PdfPCell(new Phrase(value == null || value.isBlank() ? "—" : value, PdfTheme.boldFont()));
        val.setBorder(PdfPCell.NO_BORDER);
        val.setPadding(3f);
        table.addCell(val);
    }

    private static PdfPCell body(String text, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        return cell;
    }
}
