package mz.multicore.erp.modules.printing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.pos.dto.CashDenominationDTO;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Constrói secções complementares do Fecho de Caixa Z (turnos, notas/moedas e observações). */
@Component
public class PosZReportSectionBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PdfPTable buildShiftHandoversTable(List<ShiftReconciliationDTO> shifts) {
        PdfPTable table = new PdfPTable(new float[]{20f, 30f, 17f, 17f, 16f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(4f);

        header(table, "Hora", Element.ALIGN_LEFT);
        header(table, "Passagem de Turno", Element.ALIGN_LEFT);
        header(table, "Esperado", Element.ALIGN_RIGHT);
        header(table, "Contado", Element.ALIGN_RIGHT);
        header(table, "Diferença", Element.ALIGN_RIGHT);

        for (ShiftReconciliationDTO s : shifts) {
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

    public PdfPTable buildCashBreakdownTable(String json) {
        try {
            List<CashDenominationDTO> items = objectMapper.readValue(json, new TypeReference<List<CashDenominationDTO>>() {});
            if (items == null || items.isEmpty() || items.stream().noneMatch(d -> d.count() > 0)) {
                return null;
            }
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

    public PdfPTable buildNotesBlock(String notes) {
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

    static void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
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
        }
        return String.format("Moeda de %,.2f MT", d);
    }

    static String money(BigDecimal v) {
        return v == null ? "—" : String.format("%,.2f MT", v);
    }

    static String safe(String s) {
        return s == null ? "—" : s;
    }
}
