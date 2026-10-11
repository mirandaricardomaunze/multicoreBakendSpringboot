package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Mapa Oficial de Amortizações e Reintegrações da Autoridade Tributária de Moçambique (Modelo 22 Anexo D - PGC-NIRF).
 * Emissão em folha A4 horizontal (Landscape) com todas as colunas de apuramento fiscal e contabilístico.
 */
@Service
public class AssetDepreciationMapPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AssetRepository assetRepository;
    private final CompanyService companyService;

    public AssetDepreciationMapPrintService(AssetRepository assetRepository, CompanyService companyService) {
        this.assetRepository = assetRepository;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int fiscalYear) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        List<Asset> assets = assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId);

        return PdfDocumentBuilder.build(PageSize.A4.rotate(), doc -> {
            doc.add(buildOfficialFiscalHeader(company, fiscalYear));
            doc.add(buildAssetsTable(assets, fiscalYear));
            doc.add(PdfDocumentBuilder.spacer(14f));
            doc.add(buildFiscalClosingBlock());
        });
    }

    private PdfPTable buildOfficialFiscalHeader(Company company, int fiscalYear) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{65f, 35f}); } catch (Exception ignored) {}
        table.setSpacingAfter(12f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("REPÚBLICA DE MOÇAMBIQUE — AUTORIDADE TRIBUTÁRIA", PdfTheme.tableHeaderFont()));
        left.addElement(new Paragraph("MODELO 22 — ANEXO D: MAPA OFICIAL DE AMORTIZAÇÕES E REINTEGRAÇÕES", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(company.getName() + " — NUIT: " + (company.getTaxId() != null ? company.getTaxId() : "N/D"), PdfTheme.bodyFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph yearP = new Paragraph("EXERCÍCIO FISCAL: " + fiscalYear, PdfTheme.titleFont());
        yearP.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(yearP);
        Paragraph stamp = new Paragraph("Emitido em: " + LocalDateTime.now().format(STAMP_FMT), PdfTheme.smallFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);

        return table;
    }

    private PdfPTable buildAssetsTable(List<Asset> assets, int fiscalYear) {
        // 10 colunas oficiais
        PdfPTable table = new PdfPTable(new float[]{8f, 20f, 12f, 9f, 11f, 6f, 11f, 11f, 11f, 11f});
        table.setWidthPercentage(100);

        header(table, "Código", Element.ALIGN_LEFT);
        header(table, "Designação do Elemento do Activo", Element.ALIGN_LEFT);
        header(table, "Categoria", Element.ALIGN_LEFT);
        header(table, "Data Aquis.", Element.ALIGN_CENTER);
        header(table, "Custo Aquis. (MT)", Element.ALIGN_RIGHT);
        header(table, "Taxa%", Element.ALIGN_RIGHT);
        header(table, "Amort. Anteriores", Element.ALIGN_RIGHT);
        header(table, "Quota Exercício", Element.ALIGN_RIGHT);
        header(table, "Amort. Acumuladas", Element.ALIGN_RIGHT);
        header(table, "V. Líquido (MT)", Element.ALIGN_RIGHT);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalPrev = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal totalAccum = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        boolean alt = false;
        for (Asset a : assets) {
            BigDecimal cost = a.getAcquisitionCost() != null ? a.getAcquisitionCost() : BigDecimal.ZERO;
            BigDecimal rate = a.getAnnualDepreciationRate() != null ? a.getAnnualDepreciationRate() : BigDecimal.ZERO;
            BigDecimal accum = a.getAccumulatedDepreciation() != null ? a.getAccumulatedDepreciation() : BigDecimal.ZERO;
            BigDecimal net = a.getNetBookValue() != null ? a.getNetBookValue() : cost.subtract(accum).max(BigDecimal.ZERO);

            // Quota calculada com base na taxa anual sobre o custo histórico
            BigDecimal ratePercent = rate.compareTo(BigDecimal.ONE) <= 0 ? rate : rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal currentYearQuota = cost.multiply(ratePercent).setScale(2, RoundingMode.HALF_UP);
            if (currentYearQuota.compareTo(cost.subtract(accum)) > 0 && cost.subtract(accum).signum() > 0) {
                currentYearQuota = cost.subtract(accum);
            }
            if (accum.compareTo(currentYearQuota) < 0) {
                currentYearQuota = accum;
            }
            BigDecimal prevAccum = accum.subtract(currentYearQuota).max(BigDecimal.ZERO);

            totalCost = totalCost.add(cost);
            totalPrev = totalPrev.add(prevAccum);
            totalCurrent = totalCurrent.add(currentYearQuota);
            totalAccum = totalAccum.add(accum);
            totalNet = totalNet.add(net);

            cell(table, a.getAssetCode(), Element.ALIGN_LEFT, alt);
            cell(table, a.getName(), Element.ALIGN_LEFT, alt);
            cell(table, a.getCategory() != null ? a.getCategory().name() : "-", Element.ALIGN_LEFT, alt);
            cell(table, a.getAcquisitionDate() != null ? a.getAcquisitionDate().format(DATE_FMT) : "-", Element.ALIGN_CENTER, alt);
            cell(table, MoneyFormat.format(cost), Element.ALIGN_RIGHT, alt);
            cell(table, (ratePercent.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)) + "%", Element.ALIGN_RIGHT, alt);
            cell(table, MoneyFormat.format(prevAccum), Element.ALIGN_RIGHT, alt);
            cell(table, MoneyFormat.format(currentYearQuota), Element.ALIGN_RIGHT, alt);
            cell(table, MoneyFormat.format(accum), Element.ALIGN_RIGHT, alt);
            cell(table, MoneyFormat.format(net), Element.ALIGN_RIGHT, alt);

            alt = !alt;
        }

        // Linha de Totais
        totalCell(table, "TOTAIS GERAIS (MZN)", 4, Element.ALIGN_LEFT);
        totalCell(table, MoneyFormat.format(totalCost), 1, Element.ALIGN_RIGHT);
        totalCell(table, "-", 1, Element.ALIGN_CENTER);
        totalCell(table, MoneyFormat.format(totalPrev), 1, Element.ALIGN_RIGHT);
        totalCell(table, MoneyFormat.format(totalCurrent), 1, Element.ALIGN_RIGHT);
        totalCell(table, MoneyFormat.format(totalAccum), 1, Element.ALIGN_RIGHT);
        totalCell(table, MoneyFormat.format(totalNet), 1, Element.ALIGN_RIGHT);

        return table;
    }

    private PdfPTable buildFiscalClosingBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("O Técnico de Contas Certificado (OCAM)", PdfTheme.bodyFont()));
        left.addElement(PdfDocumentBuilder.spacer(20f));
        left.addElement(new Paragraph("____________________________________________", PdfTheme.bodyFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph p1 = new Paragraph("A Gerência / Conselho de Administração", PdfTheme.bodyFont());
        p1.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(p1);
        right.addElement(PdfDocumentBuilder.spacer(20f));
        Paragraph p2 = new Paragraph("____________________________________________", PdfTheme.bodyFont());
        p2.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(p2);
        table.addCell(right);

        return table;
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
