package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Receipt (RC) in A4: who paid, which invoice, how much and how. An offset receipt (advance applied to the final
 * invoice) says plainly that no money came in. A cancelled receipt prints with the cancellation stamp and reason.
 */
@Service
public class ReceiptDocumentPrintService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReceiptRepository receiptRepository;

    public ReceiptDocumentPrintService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long receiptId) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Receipt r = receiptRepository.findById(receiptId)
                .filter(x -> x.getCompany() != null && companyId.equals(x.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        boolean cancelled = "CANCELLED".equals(r.getStatus());
        boolean offset = AdvancePayments.isOffsetPayment(r.getPaymentMethod());
        var invoice = r.getInvoice();
        var client = invoice.getClient();
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(r.getCompany(), "Recibo", r.getReceiptNumber(), cancelled ? "ANULADO" : null));
            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            pair(info, "Cliente", client == null ? invoice.getCustomerName() : client.getName());
            pair(info, "NUIT", client == null ? null : client.getTaxId());
            pair(info, "Fatura", invoice.getInvoiceNumber());
            pair(info, "Data", r.getReceiptDate() == null ? null : DATE_TIME.format(r.getReceiptDate()));
            pair(info, "Forma de pagamento", offset ? "Compensação de adiantamento" : method(r.getPaymentMethod()));
            pair(info, "Conta", r.getTreasuryAccount() == null ? null : r.getTreasuryAccount().getName());
            doc.add(info);
            doc.add(PdfDocumentBuilder.spacer(14f));
            doc.add(new Paragraph("Recebemos a quantia de " + DisplayNumbers.money(r.getAmountPaid()) + " " + PdfTheme.CURRENCY_CODE
                    + " por conta da fatura " + invoice.getInvoiceNumber() + ".", PdfTheme.boldFont()));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(new Paragraph("Total da fatura: " + DisplayNumbers.money(invoice.getTotalAmount()) + " " + PdfTheme.CURRENCY_CODE
                    + "   ·   Em dívida após este recibo: " + DisplayNumbers.money(invoice.outstandingAmount()) + " " + PdfTheme.CURRENCY_CODE,
                    PdfTheme.bodyFont()));
            if (offset) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                doc.add(new Paragraph("Este recibo desconta um adiantamento já pago e regularizado por nota de crédito. "
                        + "Não houve nova entrada de dinheiro.", PdfTheme.smallFont()));
            }
            if (cancelled && r.getCancellationReason() != null) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                doc.add(new Paragraph("Recibo anulado. Motivo: " + r.getCancellationReason(), PdfTheme.voidStampFont()));
            }
            doc.add(PdfDocumentBuilder.spacer(36f));
            doc.add(SignatureBlockRenderer.build("Recebi (a empresa)", "O Cliente"));
        });
    }

    static String method(String code) {
        if (code == null) return "—";
        return switch (code.toUpperCase()) {
            case "CASH" -> "Numerário";
            case "CARD" -> "Cartão";
            case "BANK_TRANSFER" -> "Transferência";
            case "MPESA", "M-PESA" -> "M-Pesa";
            case "EMOLA", "E-MOLA" -> "E-Mola";
            default -> code;
        };
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
}
