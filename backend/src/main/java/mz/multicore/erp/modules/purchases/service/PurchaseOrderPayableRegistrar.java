package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.PurchaseOrderLine;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cria a dívida ao fornecedor pela mercadoria aceite numa recepção de encomenda.
 * O stock já entrou na recepção, por isso aqui não há movimento de stock.
 */
@Component
public class PurchaseOrderPayableRegistrar {

    private final PurchaseRepository purchaseRepository;
    private final DocumentNumberService documentNumberService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public PurchaseOrderPayableRegistrar(PurchaseRepository purchaseRepository,
                                         DocumentNumberService documentNumberService,
                                         ApplicationEventPublisher eventPublisher,
                                         AuditLogService auditLogService) {
        this.purchaseRepository = purchaseRepository;
        this.documentNumberService = documentNumberService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    public void register(PurchaseOrder order, Map<PurchaseOrderLine, BigDecimal> accepted) {
        if (accepted.isEmpty()) return;

        Purchase purchase = new Purchase();
        purchase.setPurchaseOrder(order);
        purchase.setSupplier(order.getSupplier());
        purchase.setWarehouse(order.getWarehouse());
        purchase.setCompany(order.getCompany());
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setCreatedBy(CurrentUserContext.getUsername());

        BigDecimal total = BigDecimal.ZERO, totalTax = BigDecimal.ZERO;
        for (Map.Entry<PurchaseOrderLine, BigDecimal> item : accepted.entrySet()) {
            LineCalculator.LineAmounts amounts = addLine(purchase, item.getKey(), item.getValue());
            total = total.add(amounts.total());
            totalTax = totalTax.add(amounts.tax());
        }
        purchase.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        purchase.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        purchase.setPurchaseNumber(documentNumberService.next(DocumentSeries.PURCHASE));
        purchase = purchaseRepository.save(purchase);

        eventPublisher.publishEvent(new PurchaseRegisteredEvent(
                order.getCompany().getId(), purchase.getId(), purchase.getPurchaseNumber(),
                purchase.getPurchaseDate().toLocalDate(),
                purchase.getTotalAmount().subtract(purchase.getTaxAmount()),
                purchase.getTaxAmount(), purchase.getTotalAmount(), BigDecimal.ZERO, false));
        auditLogService.logCurrent("PURCHASE_ORDER_PAYABLE",
                "Compra " + purchase.getPurchaseNumber() + " criada pela recepção da encomenda "
                        + order.getOrderNumber() + " (" + purchase.getTotalAmount().toPlainString() + " MT)");
    }

    private LineCalculator.LineAmounts addLine(Purchase purchase, PurchaseOrderLine source, BigDecimal quantity) {
        LineCalculator.LineAmounts amounts = LineCalculator.compute(
                source.getUnitPrice(), quantity, BigDecimal.ZERO, source.getTaxRate());
        PurchaseLine line = new PurchaseLine();
        line.setProduct(source.getProduct());
        line.setQuantity(quantity);
        line.setUnitPrice(source.getUnitPrice());
        line.setTaxRate(source.getTaxRate());
        line.setLineTotal(amounts.total());
        line.setBatchNumber(source.getBatchNumber());
        line.setExpirationDate(source.getExpirationDate());
        line.setSerialNumber(source.getSerialNumber());
        purchase.addLine(line);
        return amounts;
    }
}
