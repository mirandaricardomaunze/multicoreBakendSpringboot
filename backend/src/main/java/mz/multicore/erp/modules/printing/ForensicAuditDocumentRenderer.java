package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.company.model.Company;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ForensicAuditDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] renderDocument(Company company, ForensicAuditSummaryDTO summary, LocalDate startDate, LocalDate endDate) {
        return PdfDocumentBuilder.buildA4(doc -> {
            String docNumber = "AUD-FOR/" + LocalDate.now().getYear();
            doc.add(CompanyHeaderRenderer.build(company, "Dossiê de Auditoria Forense & Controlo Interno", docNumber));
            doc.add(buildMetaBlock(summary, startDate, endDate));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildKpiCards(summary));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildComplianceBanner(summary));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(buildAnomaliesTable(summary));
            doc.add(PdfDocumentBuilder.spacer(14f));
            doc.add(buildSignaturesBlock());
        });
    }

    private PdfPTable buildMetaBlock(ForensicAuditSummaryDTO summary, LocalDate start, LocalDate end) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);

        String period = (start != null && end != null)
                ? "Período: " + start.format(DAY_FMT) + " a " + end.format(DAY_FMT)
                : "Período: Histórico Integral";

        PdfPCell c1 = new PdfPCell(new Paragraph(period + " | Emissão: " + LocalDate.now().format(DAY_FMT), PdfTheme.bodyFont()));
        c1.setBorder(0);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Paragraph("Moeda: " + PdfTheme.CURRENCY_CODE + " | Conformidade: " + summary.complianceScore(), PdfTheme.bodyFont()));
        c2.setBorder(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);

        return table;
    }

    private PdfPTable buildKpiCards(ForensicAuditSummaryDTO summary) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}

        table.addCell(metricCard("Risco Financeiro Total", MoneyFormat.format(summary.totalFinancialRisk()), new Color(254, 242, 242), PdfTheme.DANGER));
        table.addCell(metricCard("Ocorrências Críticas", String.valueOf(summary.criticalCount()), new Color(254, 226, 226), PdfTheme.DANGER));
        table.addCell(metricCard("Ocorrências Suspeitas", String.valueOf(summary.suspiciousCount()), new Color(254, 243, 199), new Color(180, 83, 9)));
        table.addCell(metricCard("Total de Registos", String.valueOf(summary.totalAnomalies()), new Color(245, 245, 247), PdfTheme.TEXT));

        return table;
    }

    private PdfPCell metricCard(String title, String val, Color bgColor, Color textColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);

        Paragraph pTitle = new Paragraph(title, PdfTheme.smallFont());
        pTitle.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pTitle);

        Paragraph pVal = new Paragraph(val, PdfTheme.boldFont());
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        return cell;
    }

    private PdfPTable buildComplianceBanner(ForensicAuditSummaryDTO summary) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);

        Color bannerBg;
        String prefix;
        if (summary.criticalCount() > 0) {
            bannerBg = new Color(254, 226, 226);
            prefix = "CLASSIFICAÇÃO DE RISCO: EXPOSIÇÃO CRÍTICA A FRAUDE INTERNA — ";
        } else if (summary.suspiciousCount() > 0) {
            bannerBg = new Color(254, 243, 199);
            prefix = "CLASSIFICAÇÃO DE RISCO: NÍVEL MODERADO / ATENÇÃO REQUERIDA — ";
        } else {
            bannerBg = new Color(220, 252, 231);
            prefix = "CLASSIFICAÇÃO DE RISCO: EXCELENTE CONFORMIDADE OPERACIONAL — ";
        }

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bannerBg);
        cell.setPadding(8f);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);

        Paragraph p = new Paragraph(prefix + summary.complianceScore(), PdfTheme.boldFont());
        cell.addElement(p);

        Paragraph desc = new Paragraph(
                "Este dossiê consolida os desvios fiscais, cancelamentos de faturas, quebras anormais de armazém e concessões de desconto.",
                PdfTheme.bodyFont());
        cell.addElement(desc);

        table.addCell(cell);
        return table;
    }

    private PdfPTable buildAnomaliesTable(ForensicAuditSummaryDTO summary) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100f);
        try { table.setWidths(new float[]{14f, 10f, 18f, 12f, 11f, 13f, 22f}); } catch (Exception ignored) {}

        addHeaderCell(table, "Data/Hora", Element.ALIGN_CENTER);
        addHeaderCell(table, "Severidade", Element.ALIGN_CENTER);
        addHeaderCell(table, "Categoria", Element.ALIGN_LEFT);
        addHeaderCell(table, "Referência", Element.ALIGN_LEFT);
        addHeaderCell(table, "Operador", Element.ALIGN_LEFT);
        addHeaderCell(table, "Impacto (MT)", Element.ALIGN_RIGHT);
        addHeaderCell(table, "Detalhes / Justificação", Element.ALIGN_LEFT);

        boolean alternate = false;
        for (ForensicAnomalyDTO a : summary.anomalies()) {
            Color rowBg = alternate ? PdfTheme.TOTAL_ROW_BG : Color.WHITE;
            alternate = !alternate;

            addCell(table, a.timestamp().format(DATE_FMT), rowBg, Element.ALIGN_CENTER, false);

            PdfPCell cSev = new PdfPCell(new Paragraph(a.severity().name(), PdfTheme.boldFont()));
            cSev.setBackgroundColor(rowBg);
            cSev.setBorderColor(PdfTheme.BORDER);
            cSev.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cSev);

            addCell(table, a.categoryLabel(), rowBg, Element.ALIGN_LEFT, false);
            addCell(table, a.documentOrReference(), rowBg, Element.ALIGN_LEFT, true);
            addCell(table, a.operator(), rowBg, Element.ALIGN_LEFT, false);
            addCell(table, MoneyFormat.format(a.financialImpact()), rowBg, Element.ALIGN_RIGHT, true);
            addCell(table, a.details(), rowBg, Element.ALIGN_LEFT, false);
        }

        return table;
    }

    private void addHeaderCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Color bg, int align, boolean bold) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, bold ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(0);
        c1.setPaddingTop(25f);
        c1.addElement(new Paragraph("____________________________________________", PdfTheme.smallFont()));
        c1.addElement(new Paragraph("Auditoria Interna & Controlo de Risco", PdfTheme.bodyFont()));
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setBorder(0);
        c2.setPaddingTop(25f);
        c2.addElement(new Paragraph("____________________________________________", PdfTheme.smallFont()));
        c2.addElement(new Paragraph("Direção Executiva & Conselho de Gerência", PdfTheme.bodyFont()));
        table.addCell(c2);

        return table;
    }
}
