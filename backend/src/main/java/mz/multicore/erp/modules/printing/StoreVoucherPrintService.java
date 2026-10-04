package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.pos.model.StoreVoucher;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Serviço de renderização em PDF do Vale de Compras / Store Credit (formato térmico 80mm).
 */
@Service
public class StoreVoucherPrintService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StoreVoucherRepository storeVoucherRepository;

    public StoreVoucherPrintService(StoreVoucherRepository storeVoucherRepository) {
        this.storeVoucherRepository = storeVoucherRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long voucherId) {
        StoreVoucher voucher = storeVoucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado."));
        return renderVoucher(voucher);
    }

    @Transactional(readOnly = true)
    public byte[] renderByCode(String code) {
        StoreVoucher voucher = storeVoucherRepository.findByCode(code)
                .orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado para o código: " + code));
        return renderVoucher(voucher);
    }

    private byte[] renderVoucher(StoreVoucher voucher) {
        CurrentUserContext.requireCompany(voucher.getCompany().getId());

        return PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, voucher.getCompany());

            addCentered(doc, "VALE DE COMPRAS", PdfTheme.boldFont());
            addCentered(doc, "CRÉDITO DE LOJA (STORE CREDIT)", PdfTheme.smallFont());
            doc.add(PdfDocumentBuilder.spacer(4f));

            // Caixa do Código do Vale em Destaque
            PdfPTable codeBox = new PdfPTable(1);
            codeBox.setWidthPercentage(100f);
            PdfPCell cell = new PdfPCell(new Paragraph(voucher.getCode(), PdfTheme.boldFont()));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6f);
            cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
            cell.setBorder(Rectangle.BOX);
            cell.setBorderColor(PdfTheme.BORDER);
            codeBox.addCell(cell);
            doc.add(codeBox);

            doc.add(PdfDocumentBuilder.spacer(6f));

            // Valor do Vale em Grande Destaque
            addCentered(doc, "VALOR DO CRÉDITO", PdfTheme.smallFont());
            addCentered(doc, String.format("%,.2f MT", voucher.getRemainingAmount()), PdfTheme.titleFont());

            ThermalReceiptRenderer.dottedLine(doc);

            // Detalhes do Vale
            String client = voucher.getClientName() != null && !voucher.getClientName().isBlank()
                    ? voucher.getClientName() : (voucher.getClient() != null ? voucher.getClient().getName() : "Ao Portador");
            addLeftRight(doc, "Beneficiário:", client);
            addLeftRight(doc, "Emissão:", voucher.getIssuedAt() != null ? voucher.getIssuedAt().format(DATE_TIME_FMT) : "—");
            addLeftRight(doc, "Validade:", voucher.getExpiresAt() != null ? voucher.getExpiresAt().format(DATE_FMT) : "—");
            if (voucher.getCreditNote() != null) {
                addLeftRight(doc, "Nota de Crédito:", voucher.getCreditNote().getNoteNumber());
            }
            addLeftRight(doc, "Operador:", voucher.getCreatedBy() != null ? voucher.getCreatedBy() : "—");
            addLeftRight(doc, "Estado:", voucher.getStatus() != null ? voucher.getStatus().name() : "ACTIVE");

            ThermalReceiptRenderer.dottedLine(doc);

            // Termos e Condições
            doc.add(PdfDocumentBuilder.spacer(4f));
            addCentered(doc, "TERMOS DE UTILIZAÇÃO", PdfTheme.smallFont());
            String terms = "Apresente este talão no acto do pagamento em qualquer compra nesta loja.\n"
                    + "O valor será deduzido do total a pagar. Saldo remanescente poderá ser utilizado em compras futuras.\n"
                    + "Não convertível em numerário. Válido até à data indicada.";
            for (String line : terms.split("\\n")) {
                addCentered(doc, line, PdfTheme.smallFont());
            }

            doc.add(PdfDocumentBuilder.spacer(6f));
            addCentered(doc, "Obrigado pela sua preferência!", PdfTheme.smallFont());
        });
    }

    private static void addCentered(Document doc, String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private static void addLeftRight(Document doc, String label, String value) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        try {
            table.setWidths(new float[]{40f, 60f});
        } catch (Exception ignored) {}

        PdfPCell cLabel = new PdfPCell(new Paragraph(label, PdfTheme.smallFont()));
        cLabel.setBorder(Rectangle.NO_BORDER);
        cLabel.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell cVal = new PdfPCell(new Paragraph(value, PdfTheme.bodyFont()));
        cVal.setBorder(Rectangle.NO_BORDER);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(cLabel);
        table.addCell(cVal);
        doc.add(table);
    }
}
