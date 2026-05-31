package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.service.CompanyService;
import com.phcpro.modules.fiscal.dto.IvaSummaryDTO;
import com.phcpro.modules.fiscal.service.FiscalSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Declaração mensal de IVA (Modelo simplificado, para entrega à AT). */
@Service
public class IvaDeclarationPrintService {

    private static final Locale PT = new Locale("pt", "PT");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FiscalSummaryService fiscalSummaryService;
    private final CompanyService companyService;

    public IvaDeclarationPrintService(FiscalSummaryService fiscalSummaryService, CompanyService companyService) {
        this.fiscalSummaryService = fiscalSummaryService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int year, int month) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        IvaSummaryDTO summary = fiscalSummaryService.computeMonth(companyId, year, month);
        String monthLabel = capitalize(Month.of(month).getDisplayName(TextStyle.FULL, PT)) + " de " + year;

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Declaração Mensal de IVA",
                    "IVA-" + year + "-" + String.format("%02d", month)
            ));
            doc.add(buildPeriodBlock(monthLabel));
            doc.add(buildOperationsTable(summary));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildResult(summary));

            if (!summary.sales().isEmpty()) {
                doc.add(PdfDocumentBuilder.spacer(14f));
                doc.add(buildSectionTitle("Operações de Saída (Vendas)"));
                doc.add(buildLinesTable(summary.sales()));
            }
            if (!summary.purchases().isEmpty()) {
                doc.add(PdfDocumentBuilder.spacer(12f));
                doc.add(buildSectionTitle("Operações de Entrada (Compras)"));
                doc.add(buildLinesTable(summary.purchases()));
            }

            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildPeriodBlock(String monthLabel) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Período de Apuramento", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(monthLabel, PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Imposto sobre o Valor Acrescentado — Lei do IVA / DL 8/2024", PdfTheme.smallFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph stamp = new Paragraph("Gerado em: " + LocalDateTime.now().format(STAMP_FMT), PdfTheme.bodyFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildOperationsTable(IvaSummaryDTO s) {
        PdfPTable table = new PdfPTable(new float[]{50f, 25f, 25f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);

        header(table, "Operação", Element.ALIGN_LEFT);
        header(table, "Base Tributável (MT)", Element.ALIGN_RIGHT);
        header(table, "IVA (MT)", Element.ALIGN_RIGHT);

        body(table, "Operações de Saída — Vendas (IVA Liquidado)", Element.ALIGN_LEFT);
        body(table, MoneyFormat.formatPlain(s.salesBase()), Element.ALIGN_RIGHT);
        body(table, MoneyFormat.formatPlain(s.outputTax()), Element.ALIGN_RIGHT);

        body(table, "Operações de Entrada — Compras (IVA Deduzível)", Element.ALIGN_LEFT);
        body(table, MoneyFormat.formatPlain(s.purchasesBase()), Element.ALIGN_RIGHT);
        body(table, MoneyFormat.formatPlain(s.inputTax()), Element.ALIGN_RIGHT);

        return table;
    }

    private PdfPTable buildResult(IvaSummaryDTO s) {
        BigDecimal net = s.netDue();
        boolean toPay = net.compareTo(BigDecimal.ZERO) >= 0;
        String label = toPay ? "IVA A ENTREGAR AO ESTADO" : "CRÉDITO DE IVA A REPORTAR";

        PdfPTable wrapper = new PdfPTable(new float[]{55f, 45f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});

        PdfPCell l = new PdfPCell(new Phrase(label, PdfTheme.subtitleFont()));
        PdfPCell v = new PdfPCell(new Phrase(MoneyFormat.format(net.abs()), PdfTheme.subtitleFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(7f);
        v.setPadding(7f);
        l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        inner.addCell(l);
        inner.addCell(v);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private Paragraph buildSectionTitle(String title) {
        Paragraph p = new Paragraph(title, PdfTheme.subtitleFont());
        p.setSpacingAfter(4f);
        return p;
    }

    private PdfPTable buildLinesTable(List<IvaSummaryDTO.IvaLineDTO> lines) {
        PdfPTable table = new PdfPTable(new float[]{20f, 35f, 15f, 15f, 15f});
        table.setWidthPercentage(100);
        header(table, "Documento", Element.ALIGN_LEFT);
        header(table, "Contraparte", Element.ALIGN_LEFT);
        header(table, "Base (MT)", Element.ALIGN_RIGHT);
        header(table, "IVA (MT)", Element.ALIGN_RIGHT);
        header(table, "Total (MT)", Element.ALIGN_RIGHT);

        for (IvaSummaryDTO.IvaLineDTO l : lines) {
            body(table, l.documentNumber(), Element.ALIGN_LEFT);
            body(table, l.partner(), Element.ALIGN_LEFT);
            body(table, MoneyFormat.formatPlain(l.base()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(l.tax()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(l.total()), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Sujeito Passivo / Representante Legal"));
        table.addCell(signatureCell("Contabilista Certificado"));
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
        table.addCell(cell);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
