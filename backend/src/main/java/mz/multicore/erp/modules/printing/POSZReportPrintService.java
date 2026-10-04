package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.service.POSService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.pos.dto.CashDenominationDTO;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Documento de <b>Fecho de Caixa (Z)</b>: reconciliação da gaveta de uma sessão (abertura + vendas em
 * numerário + suprimentos − sangrias − devoluções = esperado, vs contado, e a diferença). Uma
 * responsabilidade: {@link PosZReportDTO} → PDF. Reusa CompanyHeaderRenderer + PdfDocumentBuilder.
 * Ver {@code docs/FECHO_CAIXA_Z_SPEC.md}.
 */
@Service
public class POSZReportPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final POSService posService;
    private final CompanyService companyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public POSZReportPrintService(POSService posService, CompanyService companyService) {
        this.posService = posService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long sessionId) {
        PosZReportDTO z = posService.buildZReport(sessionId);
        Company company = companyService.getCompanyById(CurrentUserContext.getCurrentCompanyId());
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Fecho de Caixa (Z)", "Z-" + z.sessionId()));
            doc.add(buildMetaBlock(z));
            doc.add(buildReconciliationTable(z));

            if (z.shiftReconciliations() != null && !z.shiftReconciliations().isEmpty()) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                doc.add(buildShiftHandoversTable(z.shiftReconciliations()));
            }

            if (z.cashBreakdownJson() != null && !z.cashBreakdownJson().isBlank()) {
                PdfPTable breakdown = buildCashBreakdownTable(z.cashBreakdownJson());
                if (breakdown != null) {
                    doc.add(PdfDocumentBuilder.spacer(6f));
                    doc.add(breakdown);
                }
            }

            if (z.closingNotes() != null && !z.closingNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                doc.add(buildNotesBlock(z.closingNotes()));
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
        String op = safe(z.operator());
        if (z.currentOperator() != null && !z.currentOperator().isBlank() && !z.currentOperator().equalsIgnoreCase(z.operator())) {
            op = op + " -> " + safe(z.currentOperator());
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

        header(table, "Reconciliação da gaveta", Element.ALIGN_LEFT);
        header(table, "Valor", Element.ALIGN_RIGHT);

        row(table, "Saldo de abertura", z.openingBalance(), false);
        row(table, "(+) Vendas em numerário", z.cashSales(), false);
        row(table, "(+) Suprimentos", z.suprimentos(), false);
        row(table, "(−) Sangrias", z.sangrias(), false);
        row(table, "(−) Devoluções", z.refunds(), false);
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

        header(table, "Vendas por Meio de Pagamento", Element.ALIGN_LEFT);
        header(table, "Montante Total", Element.ALIGN_RIGHT);

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

    // ── helpers ──────────────────────────────────────────────────────────────

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void row(PdfPTable table, String label, BigDecimal value, boolean emphasis) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasis ? PdfTheme.tableHeaderFont() : PdfTheme.bodyFont()));
        l.setBorderColor(PdfTheme.BORDER);
        l.setHorizontalAlignment(Element.ALIGN_LEFT);
        l.setPadding(5f);
        if (emphasis) l.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        table.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(money(value), emphasis ? PdfTheme.tableHeaderFont() : PdfTheme.bodyFont()));
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

    private PdfPTable buildShiftHandoversTable(List<mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO> shifts) {
        PdfPTable table = new PdfPTable(new float[]{20f, 30f, 17f, 17f, 16f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(4f);

        header(table, "Hora", Element.ALIGN_LEFT);
        header(table, "Passagem de Turno", Element.ALIGN_LEFT);
        header(table, "Esperado", Element.ALIGN_RIGHT);
        header(table, "Contado", Element.ALIGN_RIGHT);
        header(table, "Diferença", Element.ALIGN_RIGHT);

        for (mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO s : shifts) {
            String time = s.reconciledAt() != null ? s.reconciledAt().format(DATE_FMT) : "—";
            String opTransition = safe(s.outgoingOperator()) + " -> " + safe(s.incomingOperator());
            table.addCell(bodyCell(time, Element.ALIGN_LEFT));
            table.addCell(bodyCell(opTransition, Element.ALIGN_LEFT));
            table.addCell(bodyCell(money(s.expectedCash()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(s.countedCash()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(s.difference()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private PdfPTable buildCashBreakdownTable(String json) {
        try {
            List<CashDenominationDTO> items = objectMapper.readValue(json, new TypeReference<List<CashDenominationDTO>>() {});
            if (items == null || items.isEmpty()) return null;

            boolean hasCount = items.stream().anyMatch(d -> d.count() > 0);
            if (!hasCount) return null;

            PdfPTable table = new PdfPTable(new float[]{45f, 25f, 30f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(4f);
            table.setSpacingAfter(4f);

            header(table, "Contagem Física da Gaveta (Notas & Moedas)", Element.ALIGN_LEFT);
            header(table, "Quantidade", Element.ALIGN_RIGHT);
            header(table, "Subtotal (MT)", Element.ALIGN_RIGHT);

            for (CashDenominationDTO d : items) {
                if (d.count() > 0) {
                    table.addCell(bodyCell(formatDenomination(d.denomination()), Element.ALIGN_LEFT));
                    table.addCell(bodyCell(String.valueOf(d.count()), Element.ALIGN_RIGHT));
                    table.addCell(bodyCell(money(d.subtotal()), Element.ALIGN_RIGHT));
                }
            }
            return table;
        } catch (Exception e) {
            return null;
        }
    }

    private PdfPTable buildNotesBlock(String notes) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(4f);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfTheme.ROW_ALT);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        cell.addElement(new Paragraph("Observações / Justificação do Fecho de Caixa:", PdfTheme.subtitleFont()));
        cell.addElement(new Paragraph(notes, PdfTheme.bodyFont()));
        table.addCell(cell);
        return table;
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        return cell;
    }

    private String formatDenomination(BigDecimal d) {
        if (d == null) return "—";
        if (d.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return String.format("Nota de %,.0f MT", d);
        } else if (d.compareTo(BigDecimal.ONE) >= 0) {
            return String.format("Moeda de %,.0f MT", d);
        } else {
            return String.format("Moeda de %,.2f MT", d);
        }
    }

    private static String money(BigDecimal v) {
        return v == null ? "—" : String.format("%,.2f MT", v);
    }

    private static String safe(String s) {
        return s == null ? "—" : s;
    }
}
