package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.Payslip;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Compõe as vias e os blocos visuais do recibo de salário. */
@Component
public class PayslipDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT = Locale.forLanguageTag("pt-PT");

    private final PayslipCompensationTableRenderer compensationRenderer;

    public PayslipDocumentRenderer(PayslipCompensationTableRenderer compensationRenderer) {
        this.compensationRenderer = compensationRenderer;
    }

    byte[] render(Payslip payslip, Company company, List<String> copies) {
        return PdfDocumentBuilder.buildA4(document -> {
            for (int index = 0; index < copies.size(); index++) {
                if (index > 0) {
                    document.newPage();
                }
                renderCopy(document, payslip, company, copies.get(index));
            }
        });
    }

    private void renderCopy(Document document, Payslip payslip, Company company, String copyLabel) {
        document.add(CompanyHeaderRenderer.build(company, "Recibo de Salário", payslip.getPayslipNumber()));
        document.add(buildCopyBadge(copyLabel));
        document.add(buildEmployeeBlock(payslip));
        document.add(compensationRenderer.buildEarnings(payslip));
        document.add(compensationRenderer.buildDeductions(payslip));
        document.add(PdfDocumentBuilder.spacer(6f));
        document.add(buildNetPayBlock(payslip));
        addNotes(document, payslip);
        addTaxConfiguration(document, payslip);
        document.add(PdfDocumentBuilder.spacer(30f));
        document.add(buildSignatureBlock());
    }

    private PdfPTable buildCopyBadge(String copyText) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        table.setSpacingAfter(8f);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setBorderColor(new Color(203, 213, 225));
        cell.setBorderWidth(1f);
        cell.setPadding(5f);
        Font font = new Font(Font.HELVETICA, 8.5f, Font.BOLD, new Color(30, 41, 59));
        Paragraph paragraph = new Paragraph(copyText, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(paragraph);
        table.addCell(cell);
        return table;
    }

    private PdfPTable buildEmployeeBlock(Payslip payslip) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{60f, 40f});
        } catch (Exception ignored) {
            // As larguras por omissão continuam válidas se o renderer PDF as rejeitar.
        }
        table.setSpacingAfter(10f);

        PdfPCell employee = borderlessCell();
        employee.addElement(new Paragraph("Colaborador", PdfTheme.subtitleFont()));
        employee.addElement(new Paragraph(payslip.getEmployee().getName(), PdfTheme.bodyFont()));
        addEmployeeDetail(employee, "Departamento: ", payslip.getEmployee().getDepartment(), PdfTheme.bodyFont());
        addEmployeeDetail(employee, "Cargo: ", payslip.getEmployee().getRole(), PdfTheme.bodyFont());
        addEmployeeDetail(employee, "", payslip.getEmployee().getEmail(), PdfTheme.smallFont());
        table.addCell(employee);
        table.addCell(buildPeriodCell(payslip));
        return table;
    }

    private PdfPCell buildPeriodCell(Payslip payslip) {
        PdfPCell period = borderlessCell();
        addRight(period, "Período", PdfTheme.subtitleFont());
        String month = Month.of(payslip.getMonth()).getDisplayName(TextStyle.FULL, PT);
        addRight(period, capitalize(month) + " de " + payslip.getYear(), PdfTheme.bodyFont());
        addRight(period, "Estado: " + payslip.getStatus(), PdfTheme.boldFont());
        if (payslip.getPaymentDate() != null) {
            addRight(period, "Pago em: " + payslip.getPaymentDate().format(DATE_FMT), PdfTheme.bodyFont());
        }
        return period;
    }

    private PdfPTable buildNetPayBlock(Payslip payslip) {
        PdfPTable wrapper = new PdfPTable(new float[]{50f, 50f});
        wrapper.setWidthPercentage(100);
        wrapper.addCell(borderlessCell());
        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        PdfPCell label = totalCell("LÍQUIDO A RECEBER", Element.ALIGN_LEFT);
        PdfPCell value = totalCell(MoneyFormat.format(payslip.getNetPay()), Element.ALIGN_RIGHT);
        inner.addCell(label);
        inner.addCell(value);
        PdfPCell innerWrapper = new PdfPCell(inner);
        innerWrapper.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrapper);
        return wrapper;
    }

    private void addNotes(Document document, Payslip payslip) {
        if (payslip.getNotes() != null && !payslip.getNotes().isBlank()) {
            document.add(PdfDocumentBuilder.spacer(10f));
            document.add(new Paragraph("Observações: " + payslip.getNotes(), PdfTheme.bodyFont()));
        }
    }

    private void addTaxConfiguration(Document document, Payslip payslip) {
        if (payslip.getTaxConfigName() == null) {
            return;
        }
        document.add(PdfDocumentBuilder.spacer(8f));
        document.add(new Paragraph("Configuração fiscal: " + payslip.getTaxConfigName(), PdfTheme.smallFont()));
        if (payslip.getTaxLegalBasis() != null) {
            document.add(new Paragraph(
                    "Base legal/configuração: " + payslip.getTaxLegalBasis(), PdfTheme.smallFont()));
        }
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{50f, 50f});
        } catch (Exception ignored) {
            // As larguras por omissão continuam válidas se o renderer PDF as rejeitar.
        }
        table.addCell(signatureCell("Entidade Patronal"));
        table.addCell(signatureCell("Colaborador"));
        return table;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = borderlessCell();
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);
        cell.addElement(new Paragraph("____________________________", PdfTheme.bodyFont()));
        cell.addElement(new Paragraph(label, PdfTheme.smallFont()));
        return cell;
    }

    private PdfPCell totalCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.subtitleFont()));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        return cell;
    }

    private PdfPCell borderlessCell() {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }

    private void addEmployeeDetail(PdfPCell cell, String prefix, String value, Font font) {
        if (value != null) {
            cell.addElement(new Paragraph(prefix + value, font));
        }
    }

    private void addRight(PdfPCell cell, String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(paragraph);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
