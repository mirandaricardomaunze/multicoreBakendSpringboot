package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.InventoryItemDTO;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InventoryPhysicalCountingPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompanyService companyService;

    public InventoryPhysicalCountingPrintService(CompanyService companyService) {
        this.companyService = companyService;
    }

    public byte[] render(InventorySessionDTO session, Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Dossiê de Inventário Físico & Reconciliação de Stock",
                    session.inventoryNumber()
            ));

            doc.add(buildHeaderMeta(session));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildSummaryCards(session));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(buildItemsTable(session.items(), session.blindCounting()));
            doc.add(PdfDocumentBuilder.spacer(12f));
            doc.add(buildSignaturesBlock());
        });
    }

    private PdfPTable buildHeaderMeta(InventorySessionDTO session) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Descrição: " + session.description(), PdfTheme.boldFont()));
        left.addElement(new Paragraph("Estado: " + session.status().getDescription() +
                (session.blindCounting() ? " (Contagem Cega)" : ""), PdfTheme.bodyFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("Data Início: " + (session.startDate() != null ? session.startDate().format(DATE_FMT) : "-"), PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Data Fecho: " + (session.endDate() != null ? session.endDate().format(DATE_FMT) : "Em Aberto"), PdfTheme.bodyFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildSummaryCards(InventorySessionDTO session) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        table.addCell(createCardCell("Total de Itens", String.valueOf(session.totalItems())));
        table.addCell(createCardCell("Sobras (MT)", String.format("%,.2f", session.totalSurplusValue())));
        table.addCell(createCardCell("Faltas (MT)", String.format("%,.2f", session.totalDeficitValue())));
        table.addCell(createCardCell("Impacto Líquido (MT)", String.format("%,.2f", session.netFinancialImpact())));

        return table;
    }

    private PdfPCell createCardCell(String title, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        cell.setPadding(6f);
        cell.addElement(new Paragraph(title, PdfTheme.smallFont()));
        cell.addElement(new Paragraph(value, PdfTheme.boldFont()));
        return cell;
    }

    private PdfPTable buildItemsTable(List<InventoryItemDTO> items, boolean isBlind) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{14f, 26f, 12f, 12f, 12f, 12f, 12f}); } catch (Exception ignored) {}

        table.addCell(headerCell("Código"));
        table.addCell(headerCell("Produto"));
        table.addCell(headerCell("Esperado"));
        table.addCell(headerCell("Contado"));
        table.addCell(headerCell("Diferença"));
        table.addCell(headerCell("Custo (MT)"));
        table.addCell(headerCell("Impacto (MT)"));

        boolean alt = false;
        for (InventoryItemDTO item : items) {
            java.awt.Color bg = alt ? PdfTheme.TOTAL_ROW_BG : java.awt.Color.WHITE;

            table.addCell(bodyCell(item.productCode(), bg, Element.ALIGN_LEFT));
            table.addCell(bodyCell(item.productName(), bg, Element.ALIGN_LEFT));
            table.addCell(bodyCell(isBlind ? "***" : String.format("%,.2f", item.expectedQuantity()), bg, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(String.format("%,.2f", item.countedQuantity()), bg, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(isBlind ? "***" : String.format("%+,.2f", item.difference()), bg, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(String.format("%,.2f", item.unitCost()), bg, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(isBlind ? "***" : String.format("%+,.2f", item.financialImpact()), bg, Element.ALIGN_RIGHT));

            alt = !alt;
        }

        return table;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell bodyCell(String text, java.awt.Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", PdfTheme.bodyFont()));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.setPadding(10f);
        left.addElement(new Paragraph("__________________________________________", PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Responsável da Contagem (Fiel de Armazém)", PdfTheme.smallFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setPadding(10f);
        right.addElement(new Paragraph("__________________________________________", PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Aprovação / Gerência Geral", PdfTheme.smallFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }
}
