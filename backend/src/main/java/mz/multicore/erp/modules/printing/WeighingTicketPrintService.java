package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.weighbridge.model.WeighingDirection;
import mz.multicore.erp.modules.weighbridge.model.WeighingStatus;
import mz.multicore.erp.modules.weighbridge.model.WeighingTicket;
import mz.multicore.erp.modules.weighbridge.service.WeighbridgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Weighing ticket (TP) handed to the driver: both weighings, the net weight and who weighed. Internal document;
 * a typed weight is printed as such.
 */
@Service
public class WeighingTicketPrintService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WeighbridgeService weighbridgeService;

    public WeighingTicketPrintService(WeighbridgeService weighbridgeService) {
        this.weighbridgeService = weighbridgeService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long ticketId) {
        WeighingTicket t = weighbridgeService.forPrint(companyId, ticketId);
        String stamp = t.getStatus() == WeighingStatus.OPEN ? "EM CURSO" : null;
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(t.getCompany(), "Talão de Pesagem", t.getNumber(), stamp));
            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            pair(info, "Matrícula", t.getPlate());
            pair(info, "Motorista", t.getDriver());
            pair(info, "Sentido", t.getDirection() == WeighingDirection.IN ? "Entrada de material" : "Saída de material");
            pair(info, "Cliente / fornecedor", t.getCounterparty());
            pair(info, "Artigo", t.getProduct() == null ? null : t.getProduct().getName());
            pair(info, "Documento", t.getReference());
            doc.add(info);
            doc.add(PdfDocumentBuilder.spacer(10f));
            PdfPTable weights = new PdfPTable(3);
            weights.setWidthPercentage(100);
            for (String header : new String[]{"Pesagem", "Data e hora", "Peso (kg)"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, PdfTheme.tableHeaderFont()));
                cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
                cell.setBorderColor(PdfTheme.BORDER);
                cell.setPadding(5f);
                weights.addCell(cell);
            }
            row(weights, "1.ª pesagem (entrada)", t.getFirstAt() == null ? "" : DATE_TIME.format(t.getFirstAt()), DisplayNumbers.qty(t.getFirstWeight()));
            row(weights, "2.ª pesagem (saída)", t.getSecondAt() == null ? "—" : DATE_TIME.format(t.getSecondAt()),
                    t.getSecondWeight() == null ? "—" : DisplayNumbers.qty(t.getSecondWeight()));
            row(weights, "PESO LÍQUIDO", "", t.getNetWeight() == null ? "—" : DisplayNumbers.qty(t.getNetWeight()));
            doc.add(weights);
            doc.add(PdfDocumentBuilder.spacer(8f));
            if (t.isManual()) doc.add(new Paragraph("Atenção: pelo menos um dos pesos foi escrito à mão (não lido da balança).", PdfTheme.voidStampFont()));
            doc.add(new Paragraph("Documento interno de pesagem. Não substitui a fatura nem a guia de remessa.", PdfTheme.smallFont()));
            doc.add(PdfDocumentBuilder.spacer(30f));
            doc.add(SignatureBlockRenderer.build("O Operador da Báscula", "O Motorista (nome e assinatura)"));
        });
    }

    private static void pair(PdfPTable table, String label, String value) {
        PdfPCell key = new PdfPCell(new Phrase(label, PdfTheme.smallFont()));
        key.setBorderColor(PdfTheme.BORDER);
        key.setPadding(4f);
        table.addCell(key);
        PdfPCell val = new PdfPCell(new Phrase(value == null || value.isBlank() ? "—" : value, PdfTheme.boldFont()));
        val.setBorderColor(PdfTheme.BORDER);
        val.setPadding(4f);
        table.addCell(val);
    }

    private static void row(PdfPTable table, String a, String b, String c) {
        for (String text : new String[]{a, b, c}) {
            PdfPCell cell = new PdfPCell(new Phrase(text, "PESO LÍQUIDO".equals(a) ? PdfTheme.boldFont() : PdfTheme.tableBodyFont()));
            cell.setBorderColor(PdfTheme.BORDER);
            cell.setPadding(5f);
            if (text.equals(c)) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }
    }
}
