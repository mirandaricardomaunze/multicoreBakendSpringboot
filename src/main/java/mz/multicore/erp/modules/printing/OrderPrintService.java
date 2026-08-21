package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for a sales order (Encomenda). */
@Service
public class OrderPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderRepository orderRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public OrderPrintService(OrderRepository orderRepository, LineRowMapper lineRowMapper,
                             DocumentConfigService documentConfigService) {
        this.orderRepository = orderRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));

        CurrentUserContext.requireCompany(order.getCompany().getId());
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    order.getCompany(),
                    "Encomenda",
                    order.getOrderNumber()
            ));
            doc.add(ClientBlockRenderer.build(order.getClient(), order.getWalkInName(),
                    order.getCreatedAt(), order.getWarehouse()));
            // Origem: quem abre a encomenda tem de conseguir chegar à proposta que a justificou.
            if (order.getQuotationNumber() != null && !order.getQuotationNumber().isBlank()) {
                Paragraph origin = new Paragraph(
                        "Origem: Cotação " + order.getQuotationNumber(), PdfTheme.boldFont());
                origin.setSpacingAfter(8f);
                doc.add(origin);
            }
            doc.add(LineItemsTableRenderer.build(toRows(order.getLines()),
                    documentConfigService.getColumns(order.getCompany().getId(), mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL)));
            java.math.BigDecimal grossWeight = order.getLines().stream()
                    .map(line -> line.getProduct().getGrossUnitWeightKg() == null ? java.math.BigDecimal.ZERO
                            : line.getQuantity().multiply(line.getProduct().getGrossUnitWeightKg()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            Paragraph weight = new Paragraph("Peso bruto total: "
                    + grossWeight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg", PdfTheme.boldFont());
            weight.setAlignment(Element.ALIGN_RIGHT);
            doc.add(weight);
            doc.add(TotalsBlockRenderer.build(
                    order.getTotalBeforeTax(),
                    order.getTaxAmount(),
                    order.getTotalAmount()
            ));
            doc.add(PdfDocumentBuilder.spacer(10f));
            // Condições acordadas + data prometida. Mesmo bloco da cotação — é o mesmo acordo.
            PdfPTable terms = CommercialTermsRenderer.build(order.getPaymentTerms(),
                    order.getDeliveryTerms(), order.getExpectedDeliveryDate(), null);
            if (terms != null) {
                doc.add(terms);
            }
            doc.add(PdfDocumentBuilder.spacer(18f));
            doc.add(SignatureBlockRenderer.build("Pela empresa", "Confirmação do cliente"));
            doc.add(PdfDocumentBuilder.spacer(8f));
            // Estado em PT-MZ: este documento vai para o cliente e imprimia "PENDING_APPROVAL".
            doc.add(new Paragraph("Estado: " + OrderStatusLabel.of(order.getStatus()), PdfTheme.boldFont()));
        });
    }

    private List<LineItemsTableRenderer.Row> toRows(List<OrderLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(),
                l.getBatchNumber(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}
