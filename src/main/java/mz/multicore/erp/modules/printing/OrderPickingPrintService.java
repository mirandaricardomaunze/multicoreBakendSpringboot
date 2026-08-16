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
        addCentered(document, order.getCompany().getName(), PdfTheme.subtitleFont());
        addCentered(document, reprint ? "REIMPRESSAO - GUIA DE SEPARACAO" : "GUIA DE SEPARACAO", PdfTheme.boldFont());
        addCentered(document, order.getOrderNumber(), PdfTheme.bodyFont());
        if (order.getCreatedAt() != null) {
            addCentered(document, order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), PdfTheme.smallFont());
        }
        document.add(new Paragraph("Cliente: " + order.getClient().getName(), PdfTheme.bodyFont()));
        document.add(new Paragraph("Armazem: " + order.getWarehouse().getName(), PdfTheme.bodyFont()));
        document.add(PdfDocumentBuilder.spacer(5f));
        PdfPTable table = new PdfPTable(new float[]{55f, 20f, 25f});
        table.setWidthPercentage(100);
        addCell(table, "Artigo", Element.ALIGN_LEFT);
        addCell(table, "Qtd.", Element.ALIGN_RIGHT);
        addCell(table, "Peso", Element.ALIGN_RIGHT);
        java.math.BigDecimal totalWeight = java.math.BigDecimal.ZERO;
        for (OrderLine line : order.getLines()) {
            String reference = line.getProduct().getReference();
            addCell(table, line.getProduct().getName() + (reference == null ? "" : "\nRef: " + reference), Element.ALIGN_LEFT);
            addCell(table, line.getQuantity().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT);
            java.math.BigDecimal weight = line.getProduct().getGrossUnitWeightKg() == null
                    ? java.math.BigDecimal.ZERO
                    : line.getQuantity().multiply(line.getProduct().getGrossUnitWeightKg());
            totalWeight = totalWeight.add(weight);
            addCell(table, weight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg", Element.ALIGN_RIGHT);
        }
        document.add(table);
        Paragraph load = new Paragraph("PESO BRUTO TOTAL: "
                + totalWeight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg", PdfTheme.boldFont());
        load.setAlignment(Element.ALIGN_RIGHT);
        document.add(load);
        document.add(PdfDocumentBuilder.spacer(8f));
        document.add(new Paragraph("Separado por: __________________", PdfTheme.smallFont()));
        document.add(new Paragraph("Conferido por: _________________", PdfTheme.smallFont()));
    }

    private void addCentered(Document document, String text, com.lowagie.text.Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
    }

    private void addCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, PdfTheme.bodyFont()));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4f);
        table.addCell(cell);
    }
}
