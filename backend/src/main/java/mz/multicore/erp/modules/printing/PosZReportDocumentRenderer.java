package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Renderizador A4 do documento de Fecho de Caixa (Z). */
@Component
public class PosZReportDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final PosZReportSectionBuilder sectionBuilder;

    @Autowired
    public PosZReportDocumentRenderer(PosZReportSectionBuilder sectionBuilder) {
        this.sectionBuilder = sectionBuilder;
    }

    public PosZReportDocumentRenderer() {
        this(new PosZReportSectionBuilder());
    }

    public byte[] render(Company company, PosZReportDTO z) {
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Fecho de Caixa (Z)", "Z-" + z.sessionId()));
            doc.add(buildMetaBlock(z));
            doc.add(buildReconciliationTable(z));

            if (z.shiftReconciliations() != null && !z.shiftReconciliations().isEmpty()) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                doc.add(sectionBuilder.buildShiftHandoversTable(z.shiftReconciliations()));
            }

            if (z.cashBreakdownJson() != null && !z.cashBreakdownJson().isBlank()) {
                PdfPTable breakdown = sectionBuilder.buildCashBreakdownTable(z.cashBreakdownJson());
                if (breakdown != null) {
                    doc.add(PdfDocumentBuilder.spacer(6f));
                    doc.add(breakdown);
                }
            }

            if (z.closingNotes() != null && !z.closingNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                doc.add(sectionBuilder.buildNotesBlock(z.closingNotes()));
            }

            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(buildPaymentBreakdownTable(z));
            doc.add(PdfDocumentBuilder.spacer(20f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildMetaBlock(PosZReportDTO z) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Sessão de caixa", PdfTheme.subtitleFont()));
        String op = PosZReportSectionBuilder.safe(z.operator());
        if (z.currentOperator() != null && !z.currentOperator().isBlank() && !z.currentOperator().equalsIgnoreCase(z.operator())) {
            op = op + " -> " + PosZReportSectionBuilder.safe(z.currentOperator());
        }
        left.addElement(new Paragraph("Operador: " + op, PdfTheme.bodyFont()));
        left.addElement(new Paragraph(z.saleCount() + " venda(s) em numerário", PdfTheme.smallFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.addElement(rightPara("Aberta: " + (z.openDate() == null ? "—" : z.openDate().format(DATE_FMT))));
        right.addElement(rightPara("Fechada: " + (z.closeDate() == null ? "— (aberta)" : z.closeDate().format(DATE_FMT))));
        table.addCell(right);
        return table;
    }

    private PdfPTable buildReconciliationTable(PosZReportDTO z) {
        PdfPTable table = new PdfPTable(new float[]{65f, 35f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        PosZReportSectionBuilder.header(table, "Reconciliação da gaveta", Element.ALIGN_LEFT);
        PosZReportSectionBuilder.header(table, "Valor", Element.ALIGN_RIGHT);

        row(table, "Saldo de abertura", z.openingBalance(), false);
        row(table, "(+) Vendas em numerário", z.cashSales(), false);
        row(table, "(+) Suprimentos", z.suprimentos(), false);
        row(table, "(−) Sangrias", z.sangrias(), false);
        row(table, "(−) Devoluções", z.refunds(), false);
        if (z.counterReceipts().signum() != 0) row(table, "(+) Recebimentos ao balcão", z.counterReceipts(), false);
        row(table, "= Esperado na gaveta", z.expectedCash(), true);
        row(table, "Contado (saldo físico)", z.countedCash(), false);
        row(table, "= Diferença", z.difference(), true);
        return table;
    }

    private PdfPTable buildPaymentBreakdownTable(PosZReportDTO z) {
        PdfPTable table = new PdfPTable(new float[]{65f, 35f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        PosZReportSectionBuilder.header(table, "Vendas por Meio de Pagamento", Element.ALIGN_LEFT);
        PosZReportSectionBuilder.header(table, "Montante Total", Element.ALIGN_RIGHT);

        row(table, "Numerário (Gaveta)", z.cashSales(), false);
        row(table, "Cartão de Débito/Crédito", z.cardSales(), false);
        row(table, "M-Pesa / E-Mola", z.mpesaSales(), false);
        row(table, "Transferência Bancária", z.chequeSales(), false);
        row(table, "Vendas a Crédito (Fiado)", z.creditSales(), false);
        row(table, "= Total Geral de Vendas", z.totalSales(), true);
        return table;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Operador"));
        table.addCell(signatureCell("Conferido por"));
        return table;
    }

    private void row(PdfPTable table, String label, BigDecimal value, boolean emphasis) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasis ? PdfTheme.tableHeaderFont() : PdfTheme.bodyFont()));
        l.setBorderColor(PdfTheme.BORDER);
        l.setHorizontalAlignment(Element.ALIGN_LEFT);
        l.setPadding(5f);
        if (emphasis) l.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        table.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(PosZReportSectionBuilder.money(value),
                emphasis ? PdfTheme.tableHeaderFont() : PdfTheme.bodyFont()));
        v.setBorderColor(PdfTheme.BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(5f);
        if (emphasis) v.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        table.addCell(v);
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

    private Paragraph rightPara(String text) {
        Paragraph p = new Paragraph(text, PdfTheme.bodyFont());
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }
}
