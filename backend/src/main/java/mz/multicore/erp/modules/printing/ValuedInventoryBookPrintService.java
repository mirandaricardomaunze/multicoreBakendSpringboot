package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Livro de Inventário Valorizado de Fim de Exercício (Código Comercial de Moçambique & PGC-NIRF).
 * Exigido para encerramento de contas e apuramento do Custo das Mercadorias Vendidas e Matérias Consumidas (CMVMC).
 */
@Service
public class ValuedInventoryBookPrintService {

    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StockRepository stockRepository;
    private final CompanyService companyService;

    public ValuedInventoryBookPrintService(StockRepository stockRepository, CompanyService companyService) {
        this.stockRepository = stockRepository;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int fiscalYear) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        List<Stock> stocks = stockRepository.findByWarehouseCompanyId(companyId).stream()
                .filter(s -> s.getQuantity() != null && s.getQuantity().signum() > 0)
                .sorted(Comparator.comparing(s -> s.getProduct() != null ? s.getProduct().getName() : ""))
                .toList();

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(buildOfficialHeader(company, fiscalYear));
            doc.add(buildStocksTable(stocks));
            doc.add(PdfDocumentBuilder.spacer(12f));
            doc.add(buildLegalClosingTerms(fiscalYear));
        });
    }

    private PdfPTable buildOfficialHeader(Company company, int fiscalYear) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{65f, 35f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("REPÚBLICA DE MOÇAMBIQUE — CÓDIGO COMERCIAL", PdfTheme.tableHeaderFont()));
        left.addElement(new Paragraph("LIVRO DE INVENTÁRIO VALORIZADO DE FIM DE EXERCÍCIO", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(company.getName() + " — NUIT: " + (company.getTaxId() != null ? company.getTaxId() : "N/D"), PdfTheme.bodyFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph yearP = new Paragraph("EXERCÍCIO: " + fiscalYear, PdfTheme.titleFont());
        yearP.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(yearP);
        Paragraph stamp = new Paragraph("Emitido em: " + LocalDateTime.now().format(STAMP_FMT), PdfTheme.smallFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);

        return table;
    }

    private PdfPTable buildStocksTable(List<Stock> stocks) {
        // Colunas: SKU, Designação do Artigo, Armazém, Unid., Qtd Existente, Custo Unitário (CUMP), Valor Total (MZN)
        PdfPTable table = new PdfPTable(new float[]{14f, 32f, 16f, 8f, 12f, 18f, 20f});
        table.setWidthPercentage(100);

        header(table, "Código/SKU", Element.ALIGN_LEFT);
        header(table, "Designação das Mercadorias / Produtos", Element.ALIGN_LEFT);
        header(table, "Armazém", Element.ALIGN_LEFT);
        header(table, "Un.", Element.ALIGN_CENTER);
        header(table, "Quantidade", Element.ALIGN_RIGHT);
        header(table, "CUMP (MT)", Element.ALIGN_RIGHT);
        header(table, "Valor Total (MT)", Element.ALIGN_RIGHT);

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal grandTotalValue = BigDecimal.ZERO;

        boolean alt = false;
        for (Stock s : stocks) {
            Product p = s.getProduct();
            String sku = p != null && p.getSku() != null ? p.getSku() : "-";
            String name = p != null && p.getName() != null ? p.getName() : "Artigo sem nome";
            String warehouse = s.getWarehouse() != null ? s.getWarehouse().getName() : "-";
            String unit = p != null && p.getSaleType() != null ? p.getSaleType().name() : "UN";
            BigDecimal qty = s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO;
            BigDecimal cost = p != null && p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = qty.multiply(cost).setScale(2, RoundingMode.HALF_UP);

            totalQuantity = totalQuantity.add(qty);
            grandTotalValue = grandTotalValue.add(lineTotal);

            cell(table, sku, Element.ALIGN_LEFT, alt);
            cell(table, name, Element.ALIGN_LEFT, alt);
            cell(table, warehouse, Element.ALIGN_LEFT, alt);
            cell(table, unit, Element.ALIGN_CENTER, alt);
            cell(table, MoneyFormat.formatQty(qty), Element.ALIGN_RIGHT, alt);
            cell(table, MoneyFormat.format(cost), Element.ALIGN_RIGHT, alt);
            cell(table, MoneyFormat.format(lineTotal), Element.ALIGN_RIGHT, alt);

            alt = !alt;
        }

        // Linha de Totais
        totalCell(table, "VALOR GLOBAL DAS EXISTÊNCIAS (PGC-NIRF)", 4, Element.ALIGN_LEFT);
        totalCell(table, MoneyFormat.formatQty(totalQuantity), 1, Element.ALIGN_RIGHT);
        totalCell(table, "-", 1, Element.ALIGN_CENTER);
        totalCell(table, MoneyFormat.format(grandTotalValue), 1, Element.ALIGN_RIGHT);

        return table;
    }

    private PdfPTable buildLegalClosingTerms(int fiscalYear) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell legalCell = new PdfPCell();
        legalCell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        legalCell.setBorderColor(PdfTheme.BORDER);
        legalCell.setPadding(8f);

        Paragraph title = new Paragraph("TERMO DE ENCERRAMENTO DE INVENTÁRIO — EXERCÍCIO " + fiscalYear, PdfTheme.tableHeaderFont());
        Paragraph text = new Paragraph(
                "O presente Livro de Inventário encerra as existências físicas devidamente conferidas e valorizadas ao Custo " +
                "Médio Ponderado (CUMP) nos termos dos artigos 44º e seguintes do Código Comercial de Moçambique e em " +
                "conformidade com as normas do Plano Geral de Contabilidade com base nas NIRF (PGC-NIRF).",
                PdfTheme.smallFont());
        legalCell.addElement(title);
        legalCell.addElement(text);
        table.addCell(legalCell);

        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        try { sigTable.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        sigTable.setSpacingBefore(18f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("O Responsável pelo Armazém / Logística", PdfTheme.bodyFont()));
        left.addElement(PdfDocumentBuilder.spacer(18f));
        left.addElement(new Paragraph("____________________________________________", PdfTheme.bodyFont()));
        sigTable.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph rTitle = new Paragraph("O Contabilista Certificado / Auditor", PdfTheme.bodyFont());
        rTitle.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(rTitle);
        right.addElement(PdfDocumentBuilder.spacer(18f));
        Paragraph rLine = new Paragraph("____________________________________________", PdfTheme.bodyFont());
        rLine.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(rLine);
        sigTable.addCell(right);

        PdfPTable container = new PdfPTable(1);
        container.setWidthPercentage(100);
        PdfPCell c1 = new PdfPCell(table);
        c1.setBorder(PdfPCell.NO_BORDER);
        PdfPCell c2 = new PdfPCell(sigTable);
        c2.setBorder(PdfPCell.NO_BORDER);
        container.addCell(c1);
        container.addCell(c2);

        return container;
    }

    private void header(PdfPTable table, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        c.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        c.setBorderColor(PdfTheme.BORDER);
        table.addCell(c);
    }

    private void cell(PdfPTable table, String text, int align, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", PdfTheme.bodyFont()));
        c.setHorizontalAlignment(align);
        c.setPadding(4f);
        c.setBorderColor(PdfTheme.BORDER);
        if (alt) c.setBackgroundColor(PdfTheme.ROW_ALT);
        table.addCell(c);
    }

    private void totalCell(PdfPTable table, String text, int colSpan, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, PdfTheme.boldFont()));
        c.setColspan(colSpan);
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        c.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        c.setBorderColor(PdfTheme.BORDER);
        table.addCell(c);
    }
}
