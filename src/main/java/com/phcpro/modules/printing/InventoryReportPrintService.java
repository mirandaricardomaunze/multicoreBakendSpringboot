package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.service.CompanyService;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Professional inventory report (Inventário de Stock). One service, one
 * responsibility: turn stock data for a company / warehouse into a PDF that
 * matches the brand. Reuses CompanyHeaderRenderer + PdfDocumentBuilder so the
 * layout stays consistent with every other printable document.
 */
@Service
public class InventoryReportPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyService companyService;

    public InventoryReportPrintService(
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            CompanyService companyService
    ) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyService = companyService;
    }

    /**
     * @param companyId  required
     * @param warehouseId optional — when supplied, filters the report to a
     *                    single warehouse and shows it as a subtitle.
     */
    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long warehouseId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        Warehouse warehouseFilter = null;
        List<Stock> stocks;
        if (warehouseId != null) {
            warehouseFilter = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
            stocks = stockRepository.findByWarehouseId(warehouseId);
        } else {
            stocks = stockRepository.findByWarehouseCompanyId(companyId);
        }

        stocks.sort(Comparator
                .comparing((Stock s) -> s.getWarehouse().getName())
                .thenComparing(s -> s.getProduct().getName()));

        final Warehouse filterForLambda = warehouseFilter;
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Inventário de Stock",
                    "INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            ));
            doc.add(buildMetaBlock(filterForLambda));
            doc.add(buildStockTable(stocks));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(buildSummary(stocks));
            doc.add(PdfDocumentBuilder.spacer(32f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildMetaBlock(Warehouse warehouseFilter) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Âmbito", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(
                warehouseFilter == null
                        ? "Todos os armazéns da empresa"
                        : "Armazém: " + warehouseFilter.getName(),
                PdfTheme.bodyFont()));
        if (warehouseFilter != null && warehouseFilter.getLocation() != null) {
            left.addElement(new Paragraph(warehouseFilter.getLocation(), PdfTheme.smallFont()));
        }
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph stamp = new Paragraph("Gerado em: " + LocalDateTime.now().format(DATE_FMT), PdfTheme.bodyFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildStockTable(List<Stock> stocks) {
        // SKU | Nome | Armazém | Stock Mínimo | Qtd | Preço Compra | Valor | Estado
        PdfPTable table = new PdfPTable(new float[]{9f, 10f, 12f, 24f, 14f, 7f, 9f, 10f, 10f, 10f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        header(table, "SKU", Element.ALIGN_LEFT);
        header(table, "Ref.", Element.ALIGN_LEFT);
        header(table, "Cod. Barras", Element.ALIGN_LEFT);
        header(table, "Nome do Artigo", Element.ALIGN_LEFT);
        header(table, "Armazém", Element.ALIGN_LEFT);
        header(table, "Min.", Element.ALIGN_RIGHT);
        header(table, "Qtd.", Element.ALIGN_RIGHT);
        header(table, "P. Compra", Element.ALIGN_RIGHT);
        header(table, "Valor", Element.ALIGN_RIGHT);
        header(table, "Estado", Element.ALIGN_CENTER);

        for (Stock s : stocks) {
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            BigDecimal min = s.getProduct().getMinStock() == null ? BigDecimal.ZERO : s.getProduct().getMinStock();
            BigDecimal price = s.getProduct().getPurchasePrice() == null ? BigDecimal.ZERO : s.getProduct().getPurchasePrice();
            BigDecimal value = qty.multiply(price);

            String status;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) status = "ESGOTADO";
            else if (min.compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(min) < 0) status = "BAIXO";
            else status = "OK";

            body(table, s.getProduct().getSku(), Element.ALIGN_LEFT);
            body(table, s.getProduct().getReference(), Element.ALIGN_LEFT);
            body(table, s.getProduct().getBarcode(), Element.ALIGN_LEFT);
            body(table, s.getProduct().getName(), Element.ALIGN_LEFT);
            body(table, s.getWarehouse().getName(), Element.ALIGN_LEFT);
            body(table, String.format("%,.0f", min), Element.ALIGN_RIGHT);
            body(table, String.format("%,.3f", qty), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(price), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(value), Element.ALIGN_RIGHT);
            body(table, status, Element.ALIGN_CENTER);
        }
        return table;
    }

    private PdfPTable buildSummary(List<Stock> stocks) {
        long total = stocks.size();
        long lowStock = stocks.stream().filter(s -> {
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            BigDecimal min = s.getProduct().getMinStock() == null ? BigDecimal.ZERO : s.getProduct().getMinStock();
            return qty.compareTo(BigDecimal.ZERO) > 0
                    && min.compareTo(BigDecimal.ZERO) > 0
                    && qty.compareTo(min) < 0;
        }).count();
        long outOfStock = stocks.stream().filter(s -> {
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            return qty.compareTo(BigDecimal.ZERO) <= 0;
        }).count();
        BigDecimal totalValue = stocks.stream()
                .map(s -> {
                    BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
                    BigDecimal price = s.getProduct().getPurchasePrice() == null ? BigDecimal.ZERO : s.getProduct().getPurchasePrice();
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable wrapper = new PdfPTable(new float[]{55f, 45f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        addSummaryLine(inner, "Artigos no inventário", String.valueOf(total), false);
        addSummaryLine(inner, "Artigos com stock baixo", String.valueOf(lowStock), false);
        addSummaryLine(inner, "Artigos esgotados", String.valueOf(outOfStock), false);
        addSummaryLine(inner, "VALOR TOTAL DO STOCK", MoneyFormat.format(totalValue), true);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Responsável pelo Inventário"));
        table.addCell(signatureCell("Conferido por"));
        return table;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);
        cell.addElement(new Paragraph("____________________________", PdfTheme.bodyFont()));
        cell.addElement(new Paragraph(label, PdfTheme.smallFont()));
        return cell;
    }

    private void addSummaryLine(PdfPTable t, String label, String value, boolean emphasised) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3f);
        v.setPadding(3f);
        if (emphasised) {
            l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        t.addCell(l);
        t.addCell(v);
    }

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }
}
