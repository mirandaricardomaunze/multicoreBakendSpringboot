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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Folha de contagem de inventário — <b>contagem cega</b>: lista os artigos (referência/SKU, nome) com
 * uma coluna <b>Contagem</b> em branco para o funcionário escrever à mão, <b>sem</b> mostrar a
 * quantidade do sistema. Depois as contagens são introduzidas e reconciliadas na aplicação (ajustes).
 *
 * <p>Uma responsabilidade: dados de stock → PDF de contagem. Reusa CompanyHeaderRenderer +
 * PdfDocumentBuilder para manter o layout consistente. Ver {@code docs/INVENTARIO_FISICO_SPEC.md}.
 */
@Service
public class InventoryCountSheetPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyService companyService;

    public InventoryCountSheetPrintService(
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            CompanyService companyService
    ) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyService = companyService;
    }

    /**
     * @param companyId   obrigatório
     * @param warehouseId opcional — quando indicado, restringe a folha a um armazém.
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
                    "Folha de Contagem de Inventário",
                    "CNT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            ));
            doc.add(buildMetaBlock(filterForLambda, stocks.size()));
            doc.add(buildCountTable(stocks, filterForLambda == null));
            doc.add(PdfDocumentBuilder.spacer(28f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildMetaBlock(Warehouse warehouseFilter, int total) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Âmbito (contagem cega)", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(
                warehouseFilter == null ? "Todos os armazéns da empresa"
                        : "Armazém: " + warehouseFilter.getName(),
                PdfTheme.bodyFont()));
        left.addElement(new Paragraph(total + " artigo(s) a contar", PdfTheme.smallFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph stamp = new Paragraph("Gerado em: " + LocalDateTime.now().format(DATE_FMT), PdfTheme.bodyFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildCountTable(List<Stock> stocks, boolean showWarehouse) {
        // Referência | Cód. Barras | Nome | [Armazém] | Contagem (em branco para escrever)
        PdfPTable table = showWarehouse
                ? new PdfPTable(new float[]{14f, 15f, 33f, 16f, 22f})
                : new PdfPTable(new float[]{16f, 18f, 42f, 24f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Cód. Barras", Element.ALIGN_LEFT);
        header(table, "Nome do Artigo", Element.ALIGN_LEFT);
        if (showWarehouse) header(table, "Armazém", Element.ALIGN_LEFT);
        header(table, "Contagem", Element.ALIGN_CENTER);

        for (Stock s : stocks) {
            String reference = s.getProduct().getReference();
            if (reference == null || reference.isBlank()) reference = s.getProduct().getSku();

            body(table, reference, Element.ALIGN_LEFT);
            body(table, s.getProduct().getBarcode(), Element.ALIGN_LEFT);
            body(table, s.getProduct().getName(), Element.ALIGN_LEFT);
            if (showWarehouse) body(table, s.getWarehouse().getName(), Element.ALIGN_LEFT);
            countBox(table); // célula em branco, mais alta, para escrever
        }
        return table;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Contado por"));
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
        cell.setMinimumHeight(18f);
        table.addCell(cell);
    }

    /** Célula em branco, mais alta, para o funcionário escrever a contagem à mão. */
    private void countBox(PdfPTable table) {
        PdfPCell cell = new PdfPCell(new Phrase(" ", PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setMinimumHeight(18f);
        table.addCell(cell);
    }
}
