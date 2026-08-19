package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
public class OrderPickingPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderRepository orderRepository;

    public OrderPickingPrintService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long orderId, boolean reprint) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Pedido nao encontrado."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        return PdfDocumentBuilder.buildReceipt(doc -> render(doc, order, reprint));
    }

    private void render(Document document, Order order, boolean reprint) {
        // Mesmo cabeçalho do recibo do POS: logótipo, nome, NUIT, morada, telefone, email. Este
        // talão sai da mesma impressora e vai para a mesma loja — não faz sentido ser outra coisa.
        ThermalReceiptRenderer.companyHeader(document, order.getCompany());

        ThermalReceiptRenderer.centered(document,
                reprint ? "REIMPRESSAO - GUIA DE SEPARACAO" : "GUIA DE SEPARACAO", PdfTheme.boldFont());
        ThermalReceiptRenderer.centered(document, order.getOrderNumber(), PdfTheme.bodyFont());
        if (order.getCreatedAt() != null) {
            ThermalReceiptRenderer.centered(document,
                    order.getCreatedAt().format(DATE_FMT), PdfTheme.smallFont());
        }
        ThermalReceiptRenderer.centered(document, "Cliente: " + order.getClient().getName(), PdfTheme.smallFont());
        ThermalReceiptRenderer.centered(document, "Armazem: " + order.getWarehouse().getName(), PdfTheme.smallFont());
        document.add(PdfDocumentBuilder.spacer(4f));

        PdfPTable table = new PdfPTable(new float[]{55f, 20f, 25f});
        table.setWidthPercentage(100);
        addCell(table, "Artigo", Element.ALIGN_LEFT, true);
        addCell(table, "Qtd.", Element.ALIGN_RIGHT, true);
        addCell(table, "Peso", Element.ALIGN_RIGHT, true);
        java.math.BigDecimal totalWeight = java.math.BigDecimal.ZERO;
        for (OrderLine line : order.getLines()) {
            String reference = line.getProduct().getReference();
            addCell(table, line.getProduct().getName() + (reference == null ? "" : "\nRef: " + reference),
                    Element.ALIGN_LEFT, false);
            addCell(table, line.getQuantity().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT, false);
            java.math.BigDecimal weight = line.getProduct().getGrossUnitWeightKg() == null
                    ? java.math.BigDecimal.ZERO
                    : line.getQuantity().multiply(line.getProduct().getGrossUnitWeightKg());
            totalWeight = totalWeight.add(weight);
            addCell(table, weight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg", Element.ALIGN_RIGHT, false);
        }
        document.add(table);

        ThermalReceiptRenderer.dottedLine(document);
        ThermalReceiptRenderer.right(document, "PESO BRUTO TOTAL: "
                + totalWeight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg", PdfTheme.boldFont());
        document.add(PdfDocumentBuilder.spacer(8f));
        document.add(new Paragraph("Separado por: __________________", PdfTheme.smallFont()));
        document.add(new Paragraph("Conferido por: _________________", PdfTheme.smallFont()));
    }

    private void addCell(PdfPTable table, String text, int alignment, boolean header) {
        ThermalReceiptRenderer.cell(table, text,
                header ? PdfTheme.tableHeaderFont() : PdfTheme.bodyFont(), alignment, header);
    }
}
