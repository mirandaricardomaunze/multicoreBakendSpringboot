package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.crm.model.SupportTicket;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF da Folha de Obra de assistência técnica — o papel que o técnico deixa assinado no cliente.
 *
 * <p>Era o único documento do sistema sem impressão: havia serviço de PDF para factura, recibo,
 * guia, cotação, encomenda, transferência, recibo de vencimento e etiquetas, mas o técnico que
 * fecha uma intervenção em casa do cliente não tinha nada para lhe deixar na mão.
 *
 * <p><b>Não é documento fiscal.</b> Os valores saem sem IVA e o rodapé di-lo — o que o cliente
 * paga sai na factura emitida a partir desta folha ("Faturar Folha de Obra").
 */
@Service
public class WorkSheetPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Uma folha de obra não é factura: sem código de barras, sem validade e sem coluna de IVA. */
    private static final DocumentColumnsDTO WORKSHEET_COLUMNS =
            new DocumentColumnsDTO(false, false, true, false, true, true, false, true, null);

    private final WorkSheetRepository workSheetRepository;

    public WorkSheetPrintService(WorkSheetRepository workSheetRepository) {
        this.workSheetRepository = workSheetRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long workSheetId) {
        WorkSheet ws = workSheetRepository
                .findByIdAndSupportTicketCompanyId(workSheetId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Folha de obra não encontrada."));

        SupportTicket ticket = ws.getSupportTicket();

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(ticket.getCompany(), "Folha de Obra", number(ws)));

            if (ws.isVoided()) {
                doc.add(voidStamp(ws));
            }

            doc.add(ClientBlockRenderer.build(ticket.getClient(), ws.getCreatedAt(), null));
            doc.add(requestBlock(ticket));
            doc.add(interventionBlock(ws));
            doc.add(LineItemsTableRenderer.build(toRows(ws), WORKSHEET_COLUMNS));
            doc.add(summaryBlock(ws));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(fiscalNote());
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(SignatureBlockRenderer.build(
                    "O Técnico (" + safe(ws.getTechnicianName()) + ")",
                    "Cliente — serviço recebido em conformidade (nome, assinatura e data)"));
        });
    }

    /** Sem série própria: a folha identifica-se pelo id, com prefixo legível no papel. */
    private String number(WorkSheet ws) {
        return "FO-" + String.format("%06d", ws.getId());
    }

    private Paragraph voidStamp(WorkSheet ws) {
        Paragraph stamp = new Paragraph("FOLHA ANULADA — SEM VALOR", PdfTheme.voidStampFont());
        stamp.setAlignment(Element.ALIGN_CENTER);
        stamp.setSpacingAfter(4f);
        if (ws.getVoidReason() != null) {
            stamp.add(new Phrase("\nMotivo: " + ws.getVoidReason(), PdfTheme.smallFont()));
        }
        return stamp;
    }

    /** O pedido que originou a deslocação: sem ele a folha não se explica a quem a lê depois. */
    private PdfPTable requestBlock(SupportTicket ticket) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Pedido de assistência", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("#" + ticket.getId() + " — " + safe(ticket.getSubject()), PdfTheme.bodyFont()));
        left.addElement(new Paragraph(safe(ticket.getDescription()), PdfTheme.bodyFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        addRight(right, "Prioridade: " + ticket.getPriority().label());
        addRight(right, "Estado: " + ticket.getStatus().label());
        if (ticket.getCreatedAt() != null) {
            addRight(right, "Aberto em: " + ticket.getCreatedAt().format(DATE_FMT));
        }
        table.addCell(right);
        return table;
    }

    private PdfPTable interventionBlock(WorkSheet ws) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.addElement(new Paragraph("Trabalho executado", PdfTheme.subtitleFont()));
        cell.addElement(new Paragraph(safe(ws.getDescription()), PdfTheme.bodyFont()));
        if (ws.getPartsUsed() != null && !ws.getPartsUsed().isBlank()) {
            cell.addElement(new Paragraph("Peças substituídas: " + ws.getPartsUsed(), PdfTheme.bodyFont()));
        }
        table.addCell(cell);
        return table;
    }

    private List<LineItemsTableRenderer.Row> toRows(WorkSheet ws) {
        List<LineItemsTableRenderer.Row> rows = new ArrayList<>();
        if (ws.getHoursWorked() != null && ws.getHoursWorked().compareTo(BigDecimal.ZERO) > 0) {
            rows.add(new LineItemsTableRenderer.Row(
                    null, null, "Mão de obra técnica (horas)", null,
                    ws.getHoursWorked(), ws.getHourlyRate(),
                    BigDecimal.ZERO, BigDecimal.ZERO, labour(ws)));
        }
        if (ws.getPartsCost() != null && ws.getPartsCost().compareTo(BigDecimal.ZERO) > 0) {
            rows.add(new LineItemsTableRenderer.Row(
                    null, null, partsDescription(ws), null,
                    BigDecimal.ONE, ws.getPartsCost(),
                    BigDecimal.ZERO, BigDecimal.ZERO, ws.getPartsCost()));
        }
        return rows;
    }

    private String partsDescription(WorkSheet ws) {
        return ws.getPartsUsed() == null || ws.getPartsUsed().isBlank()
                ? "Materiais e peças"
                : "Materiais e peças — " + ws.getPartsUsed();
    }

    /**
     * Resumo próprio em vez do {@link TotalsBlockRenderer}: aquele bloco mostra "Subtotal / IVA /
     * TOTAL" e esta folha não liquida IVA nenhum. Imprimir "IVA 0,00" num papel que o cliente
     * assina seria dizer-lhe que não paga imposto — o imposto vem na factura.
     */
    private PdfPTable summaryBlock(WorkSheet ws) {
        PdfPTable wrapper = new PdfPTable(new float[]{60f, 40f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        line(inner, "Mão de obra", MoneyFormat.format(labour(ws)), false);
        line(inner, "Peças e materiais", MoneyFormat.format(ws.getPartsCost()), false);
        line(inner, "TOTAL (sem IVA)", MoneyFormat.format(ws.getTotalValue()), true);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private Paragraph fiscalNote() {
        String note = "Documento de registo de intervenção técnica. Não serve de factura: os valores "
                + "acima não incluem IVA e a liquidação é feita na factura emitida a partir desta folha.";
        Paragraph p = new Paragraph(note, PdfTheme.smallFont());
        p.setAlignment(Element.ALIGN_LEFT);
        return p;
    }

    private BigDecimal labour(WorkSheet ws) {
        BigDecimal hours = ws.getHoursWorked() == null ? BigDecimal.ZERO : ws.getHoursWorked();
        BigDecimal rate = ws.getHourlyRate() == null ? BigDecimal.ZERO : ws.getHourlyRate();
        return hours.multiply(rate);
    }

    private void line(PdfPTable t, String label, String value, boolean emphasised) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3f);
        v.setPadding(3f);
        if (emphasised) {
            l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        t.addCell(l);
        t.addCell(v);
    }

    private void addRight(PdfPCell cell, String text) {
        Paragraph p = new Paragraph(text, PdfTheme.bodyFont());
        p.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(p);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
