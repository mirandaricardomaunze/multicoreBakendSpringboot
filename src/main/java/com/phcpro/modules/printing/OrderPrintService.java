package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.model.Order;
import com.phcpro.modules.comercial.model.OrderLine;
import com.phcpro.modules.comercial.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for a sales order (Encomenda). */
@Service
public class OrderPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderRepository orderRepository;

    public OrderPrintService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    order.getCompany(),
                    "Encomenda",
                    order.getOrderNumber()
            ));
            doc.add(buildClientBlock(order));
            doc.add(LineItemsTableRenderer.build(toRows(order.getLines())));
            doc.add(TotalsBlockRenderer.build(
                    order.getTotalBeforeTax(),
                    order.getTaxAmount(),
                    order.getTotalAmount()
            ));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(new Paragraph("Estado: " + order.getStatus(), PdfTheme.boldFont()));
        });
    }

    private PdfPTable buildClientBlock(Order order) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell client = new PdfPCell();
        client.setBorder(PdfPCell.NO_BORDER);
        client.addElement(new Paragraph("Cliente", PdfTheme.subtitleFont()));
        if (order.getClient() != null) {
            client.addElement(new Paragraph(order.getClient().getName(), PdfTheme.bodyFont()));
            if (order.getClient().getTaxId() != null) {
                client.addElement(new Paragraph("NUIT: " + order.getClient().getTaxId(), PdfTheme.bodyFont()));
            }
            if (order.getClient().getAddress() != null) {
                client.addElement(new Paragraph(order.getClient().getAddress(), PdfTheme.bodyFont()));
            }
        }
        table.addCell(client);

        PdfPCell meta = new PdfPCell();
        meta.setBorder(PdfPCell.NO_BORDER);
        if (order.getCreatedAt() != null) {
            Paragraph date = new Paragraph("Data: " + order.getCreatedAt().format(DATE_FMT), PdfTheme.bodyFont());
            date.setAlignment(Element.ALIGN_RIGHT);
            meta.addElement(date);
        }
        if (order.getWarehouse() != null) {
            Paragraph wh = new Paragraph("Armazém: " + order.getWarehouse().getName(), PdfTheme.bodyFont());
            wh.setAlignment(Element.ALIGN_RIGHT);
            meta.addElement(wh);
        }
        table.addCell(meta);
        return table;
    }

    private List<LineItemsTableRenderer.Row> toRows(List<OrderLine> lines) {
        return lines.stream().map(l -> new LineItemsTableRenderer.Row(
                l.getProduct().getSku(),
                l.getProduct().getName(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}
