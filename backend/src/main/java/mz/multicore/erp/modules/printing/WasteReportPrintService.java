package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.WasteSummaryDTO;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class WasteReportPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StockWasteRepository wasteRepository;
    private final StockWasteService wasteService;
    private final CompanyService companyService;

    public WasteReportPrintService(
            StockWasteRepository wasteRepository,
            StockWasteService wasteService,
            CompanyService companyService
    ) {
        this.wasteRepository = wasteRepository;
        this.wasteService = wasteService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, LocalDate start, LocalDate end) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        LocalDate effectiveStart = start != null ? start : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();

        LocalDateTime startDt = effectiveStart.atStartOfDay();
        LocalDateTime endDt = effectiveEnd.atTime(23, 59, 59);

        List<StockWaste> records = wasteRepository.findByCompanyIdAndPeriod(companyId, startDt, endDt);
        WasteSummaryDTO summary = wasteService.getSummary(companyId, effectiveStart, effectiveEnd);

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Relatório de Quebras e Perdas de Stock",
                    "REL-QKB-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            ));
            doc.add(buildMetaBlock(effectiveStart, effectiveEnd));
            doc.add(PdfDocumentBuilder.spacer(4f));
            doc.add(buildKpiBlock(summary));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(buildItemsTable(records));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildReasonBreakdownTable(summary.costByReason()));
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(buildSignaturesBlock());
        });
    }

    private PdfPTable buildMetaBlock(LocalDate start, LocalDate end) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(8f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Período de Análise:", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("De " + start.format(DAY_FMT) + " até " + end.format(DAY_FMT), PdfTheme.bodyFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("Emitido em: " + LocalDateTime.now().format(DATE_FMT), PdfTheme.bodyFont()));
        String user = CurrentUserContext.getUsername();
        right.addElement(new Paragraph("Operador: " + (user != null && !user.isBlank() ? user : "SISTEMA"), PdfTheme.bodyFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildKpiBlock(WasteSummaryDTO summary) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}
        table.setSpacingAfter(6f);

        addKpiCard(table, "Perda Financeira", MoneyFormat.format(summary.totalWasteCost()));
        addKpiCard(table, "Qtd Total Quebrada", summary.totalWasteQuantity().stripTrailingZeros().toPlainString() + " un");
        addKpiCard(table, "Taxa Desperdício", summary.wasteRatePercentage().toPlainString() + "% s/ vendas");
        addKpiCard(table, "Total Ocorrências", String.valueOf(summary.totalRecordsCount()));

        return table;
    }

    private void addKpiCard(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        cell.setPadding(6f);
        cell.setBorderColor(PdfTheme.BORDER);
        Paragraph pLabel = new Paragraph(label, PdfTheme.smallFont());
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);
        Paragraph pValue = new Paragraph(value, PdfTheme.boldFont());
        pValue.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pValue);
        table.addCell(cell);
    }

    private PdfPTable buildItemsTable(List<StockWaste> records) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{14f, 24f, 16f, 18f, 10f, 18f, 12f}); } catch (Exception ignored) {}

        addHeaderCell(table, "Data");
        addHeaderCell(table, "Produto");
        addHeaderCell(table, "Armazém");
        addHeaderCell(table, "Motivo");
        addHeaderCell(table, "Qtd", Element.ALIGN_RIGHT);
        addHeaderCell(table, "Total Perda", Element.ALIGN_RIGHT);
        addHeaderCell(table, "Estado", Element.ALIGN_CENTER);

        boolean alt = false;
        for (StockWaste w : records) {
            String dateStr = w.getCreatedAt() != null ? w.getCreatedAt().format(DAY_FMT) : "-";
            String prodStr = w.getProduct() != null ? w.getProduct().getName() : "-";
            String whStr = w.getWarehouse() != null ? w.getWarehouse().getName() : "-";
            String reasonStr = w.getReason() != null ? w.getReason().getDescription() : "-";
            String qtyStr = w.getQuantity() != null ? w.getQuantity().stripTrailingZeros().toPlainString() : "0";
            String costStr = w.getTotalCost() != null ? MoneyFormat.format(w.getTotalCost()) : "0,00 MT";
            String statusStr = w.getStatus() != null ? w.getStatus().getDescription() : "-";

            addDataCell(table, dateStr, Element.ALIGN_LEFT, alt);
            addDataCell(table, prodStr, Element.ALIGN_LEFT, alt);
            addDataCell(table, whStr, Element.ALIGN_LEFT, alt);
            addDataCell(table, reasonStr, Element.ALIGN_LEFT, alt);
            addDataCell(table, qtyStr, Element.ALIGN_RIGHT, alt);
            addDataCell(table, costStr, Element.ALIGN_RIGHT, alt);
            addDataCell(table, statusStr, Element.ALIGN_CENTER, alt);

            alt = !alt;
        }

        if (records.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Nenhum registo de quebra no período selecionado.", PdfTheme.bodyFont()));
            empty.setColspan(7);
            empty.setPadding(10f);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
        }

        return table;
    }

    private PdfPTable buildReasonBreakdownTable(Map<WasteReason, BigDecimal> costByReason) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        try { table.setWidths(new float[]{65f, 35f}); } catch (Exception ignored) {}

        addHeaderCell(table, "Motivo da Quebra / Perda");
        addHeaderCell(table, "Total Acumulado", Element.ALIGN_RIGHT);

        boolean alt = false;
        if (costByReason != null) {
            for (Map.Entry<WasteReason, BigDecimal> entry : costByReason.entrySet()) {
                if (entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    addDataCell(table, entry.getKey().getDescription(), Element.ALIGN_LEFT, alt);
                    addDataCell(table, MoneyFormat.format(entry.getValue()), Element.ALIGN_RIGHT, alt);
                    alt = !alt;
                }
            }
        }

        return table;
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph line1 = new Paragraph("________________________________________", PdfTheme.bodyFont());
        line1.setAlignment(Element.ALIGN_CENTER);
        Paragraph role1 = new Paragraph("Responsável pelo Armazém / Stock", PdfTheme.boldFont());
        role1.setAlignment(Element.ALIGN_CENTER);
        left.addElement(line1);
        left.addElement(role1);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph line2 = new Paragraph("________________________________________", PdfTheme.bodyFont());
        line2.setAlignment(Element.ALIGN_CENTER);
        Paragraph role2 = new Paragraph("Direcção / Gerência Geral", PdfTheme.boldFont());
        role2.setAlignment(Element.ALIGN_CENTER);
        right.addElement(line2);
        right.addElement(role2);

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        addHeaderCell(table, text, Element.ALIGN_LEFT);
    }

    private void addHeaderCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.BRAND);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, int align, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.smallFont()));
        if (alt) {
            cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        cell.setBorderColor(PdfTheme.BORDER);
        table.addCell(cell);
    }
}
