package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.dto.PayrollFiscalSummaryDTO;
import mz.multicore.erp.modules.hr.service.PayrollTaxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Mapa fiscal salarial mensal (INSS + IRPS por colaborador) para entrega às autoridades. */
@Service
public class PayrollFiscalMapPrintService {

    private static final Locale PT = new Locale("pt", "PT");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PayrollTaxService payrollTaxService;
    private final CompanyService companyService;

    public PayrollFiscalMapPrintService(PayrollTaxService payrollTaxService, CompanyService companyService) {
        this.payrollTaxService = payrollTaxService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int year, int month) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        PayrollFiscalSummaryDTO summary = payrollTaxService.fiscalSummary(year, month);
        String monthLabel = capitalize(Month.of(month).getDisplayName(TextStyle.FULL, PT)) + " de " + year;

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Mapa Fiscal Salarial (INSS / IRPS)",
                    "MFS-" + year + "-" + String.format("%02d", month)
            ));
            doc.add(buildPeriodBlock(monthLabel));
            doc.add(buildLinesTable(summary));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildTotals(summary));
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
        left.addElement(new Paragraph("Período de Referência", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(monthLabel, PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Contribuições para a Segurança Social (INSS) e IRPS retido na fonte", PdfTheme.smallFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph stamp = new Paragraph("Gerado em: " + LocalDateTime.now().format(STAMP_FMT), PdfTheme.bodyFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildLinesTable(PayrollFiscalSummaryDTO s) {
        PdfPTable table = new PdfPTable(new float[]{10f, 24f, 13f, 13f, 14f, 13f, 13f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);

        header(table, "Nº", Element.ALIGN_LEFT);
        header(table, "Colaborador", Element.ALIGN_LEFT);
        header(table, "NUIT", Element.ALIGN_LEFT);
        header(table, "Nº INSS", Element.ALIGN_LEFT);
        header(table, "Tributável (MT)", Element.ALIGN_RIGHT);
        header(table, "IRPS (MT)", Element.ALIGN_RIGHT);
        header(table, "INSS Trab. (MT)", Element.ALIGN_RIGHT);

        for (PayrollFiscalSummaryDTO.PayrollFiscalLineDTO l : s.lines()) {
            body(table, l.employeeNumber(), Element.ALIGN_LEFT);
            body(table, l.employeeName(), Element.ALIGN_LEFT);
            body(table, l.taxId(), Element.ALIGN_LEFT);
            body(table, l.inssNumber(), Element.ALIGN_LEFT);
            body(table, MoneyFormat.formatPlain(l.taxableIncome()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(l.irps()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(l.employeeInss()), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private PdfPTable buildTotals(PayrollFiscalSummaryDTO s) {
        PdfPTable table = new PdfPTable(new float[]{60f, 40f});
        table.setWidthPercentage(100);

        totalRow(table, "Total IRPS retido", MoneyFormat.format(s.irpsWithheld()));
        totalRow(table, "Total INSS Trabalhador", MoneyFormat.format(s.employeeInss()));
        totalRow(table, "Total INSS Patronal", MoneyFormat.format(s.employerInss()));
        totalRow(table, "TOTAL INSS A ENTREGAR (Trab. + Patronal)", MoneyFormat.format(s.totalInss()));
        return table;
    }

    private void totalRow(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, PdfTheme.boldFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, PdfTheme.boldFont()));
        l.setBorderColor(PdfTheme.BORDER);
        v.setBorderColor(PdfTheme.BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(5f);
        v.setPadding(5f);
        l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        table.addCell(l);
        table.addCell(v);
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Entidade Empregadora"));
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
