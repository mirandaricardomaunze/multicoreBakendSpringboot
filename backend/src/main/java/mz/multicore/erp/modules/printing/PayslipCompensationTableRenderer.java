package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.hr.dto.PayslipDeductionLineDTO;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.service.PayrollDeductionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Renderiza vencimentos e descontos sem conhecer o restante documento. */
@Component
public class PayslipCompensationTableRenderer {

    private final PayrollDeductionService deductionService;

    public PayslipCompensationTableRenderer(PayrollDeductionService deductionService) {
        this.deductionService = deductionService;
    }

    PdfPTable buildEarnings(Payslip payslip) {
        PdfPTable table = table(6f);
        header(table, "Vencimentos", Element.ALIGN_LEFT);
        header(table, "Valor (MT)", Element.ALIGN_RIGHT);
        line(table, "Salário Base", payslip.getBaseSalary(), false);
        line(table, "Subsídios / Abonos", payslip.getAllowances(), false);
        line(table, "Horas Extras", payslip.getOvertime(), false);
        if (payslip.getSalesBonus() != null && payslip.getSalesBonus().signum() > 0) {
            line(table, "Prémio de Desempenho", payslip.getSalesBonus(), false);
        }
        BigDecimal gross = payslip.getBaseSalary()
                .add(payslip.getAllowances())
                .add(payslip.getOvertime())
                .add(payslip.getSalesBonus() == null ? BigDecimal.ZERO : payslip.getSalesBonus());
        line(table, "Vencimentos Brutos", gross, true);
        return table;
    }

    PdfPTable buildDeductions(Payslip payslip) {
        PdfPTable table = table(8f);
        header(table, "Descontos", Element.ALIGN_LEFT);
        header(table, "Valor (MT)", Element.ALIGN_RIGHT);
        line(table, "IRPS", payslip.getIrpsDeduction(), false);
        line(table, "INSS", payslip.getInssDeduction(), false);
        line(table, "Faltas Injustificadas", payslip.getAbsenceDeduction(), false);
        addOtherDeductions(table, payslip);

        BigDecimal total = payslip.getIrpsDeduction()
                .add(payslip.getInssDeduction())
                .add(payslip.getAbsenceDeduction())
                .add(payslip.getOtherDeductions());
        line(table, "Total de Descontos", total, true);
        line(table, "INSS Patronal (informativo)", payslip.getEmployerInss(), false);
        return table;
    }

    private void addOtherDeductions(PdfPTable table, Payslip payslip) {
        BigDecimal itemised = BigDecimal.ZERO;
        for (PayslipDeductionLineDTO deduction : deductionService.linesOf(payslip.getId())) {
            line(table, deduction.kindLabel() + " — " + deduction.description(), deduction.amount(), false);
            itemised = itemised.add(deduction.amount());
        }
        BigDecimal remainder = payslip.getOtherDeductions().subtract(itemised);
        if (remainder.signum() != 0 || itemised.signum() == 0) {
            line(table, "Outros Descontos", remainder, false);
        }
    }

    private PdfPTable table(float spacingBefore) {
        PdfPTable table = new PdfPTable(new float[]{70f, 30f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(spacingBefore);
        table.setSpacingAfter(4f);
        return table;
    }

    private void header(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void line(PdfPTable table, String label, BigDecimal value, boolean total) {
        PdfPCell left = new PdfPCell(new Phrase(label, total ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell right = new PdfPCell(new Phrase(
                MoneyFormat.formatPlain(value), total ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        left.setBorderColor(PdfTheme.BORDER);
        right.setBorderColor(PdfTheme.BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        left.setPadding(total ? 5f : 4f);
        right.setPadding(total ? 5f : 4f);
        if (total) {
            left.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            right.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        table.addCell(left);
        table.addCell(right);
    }
}
