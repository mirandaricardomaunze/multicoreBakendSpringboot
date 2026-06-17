package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.hr.model.Payslip;
import com.phcpro.modules.hr.service.HRService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.Month;
import java.util.Locale;

/** Generates the PDF for an employee salary receipt (Recibo de Salário). */
@Service
public class PayslipPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT = new Locale("pt", "PT");

    private final HRService hrService;

    public PayslipPrintService(HRService hrService) {
        this.hrService = hrService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long payslipId) {
        Payslip p = hrService.loadPayslipForPrint(payslipId);
        Company company = p.getEmployee().getCompany();

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Recibo de Salário",
                    p.getPayslipNumber()
            ));
            doc.add(buildEmployeeBlock(p));
            doc.add(buildEarningsTable(p));
            doc.add(buildDeductionsTable(p));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(buildNetPayBlock(p));
            if (p.getNotes() != null && !p.getNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(10f));
                doc.add(new Paragraph("Observações: " + p.getNotes(), PdfTheme.bodyFont()));
            }
            if (p.getTaxConfigName() != null) {
                doc.add(PdfDocumentBuilder.spacer(8f));
                doc.add(new Paragraph("Configuração fiscal: " + p.getTaxConfigName(), PdfTheme.smallFont()));
                if (p.getTaxLegalBasis() != null) {
                    doc.add(new Paragraph("Base legal/configuração: " + p.getTaxLegalBasis(), PdfTheme.smallFont()));
                }
            }
            doc.add(PdfDocumentBuilder.spacer(30f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildEmployeeBlock(Payslip p) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Colaborador", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(p.getEmployee().getName(), PdfTheme.bodyFont()));
        if (p.getEmployee().getDepartment() != null) {
            left.addElement(new Paragraph("Departamento: " + p.getEmployee().getDepartment(), PdfTheme.bodyFont()));
        }
        if (p.getEmployee().getRole() != null) {
            left.addElement(new Paragraph("Cargo: " + p.getEmployee().getRole(), PdfTheme.bodyFont()));
        }
        if (p.getEmployee().getEmail() != null) {
            left.addElement(new Paragraph(p.getEmployee().getEmail(), PdfTheme.smallFont()));
        }
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph period = new Paragraph("Período", PdfTheme.subtitleFont());
        period.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(period);
        String monthName = Month.of(p.getMonth()).getDisplayName(TextStyle.FULL, PT);
        Paragraph periodVal = new Paragraph(capitalize(monthName) + " de " + p.getYear(), PdfTheme.bodyFont());
        periodVal.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(periodVal);
        Paragraph status = new Paragraph("Estado: " + p.getStatus(), PdfTheme.boldFont());
        status.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(status);
        if (p.getPaymentDate() != null) {
            Paragraph paid = new Paragraph("Pago em: " + p.getPaymentDate().format(DATE_FMT), PdfTheme.bodyFont());
            paid.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(paid);
        }
        table.addCell(right);

        return table;
    }

    private PdfPTable buildEarningsTable(Payslip p) {
        PdfPTable table = new PdfPTable(new float[]{70f, 30f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(4f);

        header(table, "Vencimentos", Element.ALIGN_LEFT);
        header(table, "Valor (MT)", Element.ALIGN_RIGHT);

        line(table, "Salário Base",            p.getBaseSalary());
        line(table, "Subsídios / Abonos",      p.getAllowances());
        line(table, "Horas Extras",            p.getOvertime());

        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
        totalLine(table, "Vencimentos Brutos", gross);
        return table;
    }

    private PdfPTable buildDeductionsTable(Payslip p) {
        PdfPTable table = new PdfPTable(new float[]{70f, 30f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(4f);

        header(table, "Descontos", Element.ALIGN_LEFT);
        header(table, "Valor (MT)", Element.ALIGN_RIGHT);

        line(table, "IRPS",                p.getIrpsDeduction());
        line(table, "INSS",                p.getInssDeduction());
        line(table, "Outros Descontos",    p.getOtherDeductions());

        BigDecimal total = p.getIrpsDeduction().add(p.getInssDeduction()).add(p.getOtherDeductions());
        totalLine(table, "Total de Descontos", total);
        line(table, "INSS Patronal (informativo)", p.getEmployerInss());
        return table;
    }

    private PdfPTable buildNetPayBlock(Payslip p) {
        PdfPTable wrapper = new PdfPTable(new float[]{50f, 50f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        PdfPCell label = new PdfPCell(new Phrase("LÍQUIDO A RECEBER", PdfTheme.subtitleFont()));
        PdfPCell value = new PdfPCell(new Phrase(MoneyFormat.format(p.getNetPay()), PdfTheme.subtitleFont()));
        label.setBorder(PdfPCell.NO_BORDER);
        value.setBorder(PdfPCell.NO_BORDER);
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        label.setPadding(6f);
        value.setPadding(6f);
        label.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        value.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        inner.addCell(label);
        inner.addCell(value);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        table.addCell(signatureCell("Entidade Patronal"));
        table.addCell(signatureCell("Colaborador"));
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

    private void line(PdfPTable table, String label, BigDecimal value) {
        PdfPCell l = new PdfPCell(new Phrase(label, PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(MoneyFormat.formatPlain(value), PdfTheme.bodyFont()));
        l.setBorderColor(PdfTheme.BORDER);
        v.setBorderColor(PdfTheme.BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(4f);
        v.setPadding(4f);
        table.addCell(l);
        table.addCell(v);
    }

    private void totalLine(PdfPTable table, String label, BigDecimal value) {
        PdfPCell l = new PdfPCell(new Phrase(label, PdfTheme.boldFont()));
        PdfPCell v = new PdfPCell(new Phrase(MoneyFormat.formatPlain(value), PdfTheme.boldFont()));
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
