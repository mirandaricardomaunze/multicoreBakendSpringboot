package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Fatura mercadoria já expedida por guia. O stock saiu na aprovação da guia, por isso esta fatura
 * nunca move stock: nem ao ser emitida, nem ao ser anulada.
 */
@Service
public class DeliveryGuideBillingService {

    private final DeliveryGuideRepository guideRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceivablesService receivablesService;
    private final ComercialClientAndCreditService clientAndCreditService;
    private final ComercialInvoiceLifecycleService invoiceLifecycleService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public DeliveryGuideBillingService(DeliveryGuideRepository guideRepository,
                                       OrderRepository orderRepository,
                                       InvoiceRepository invoiceRepository,
                                       ReceivablesService receivablesService,
                                       ComercialClientAndCreditService clientAndCreditService,
                                       ComercialInvoiceLifecycleService invoiceLifecycleService,
                                       DocumentNumberService documentNumberService,
                                       AuditLogService auditLogService) {
        this.guideRepository = guideRepository;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.receivablesService = receivablesService;
        this.clientAndCreditService = clientAndCreditService;
        this.invoiceLifecycleService = invoiceLifecycleService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public InvoiceDTO bill(Long guideId) {
        PermissionGuard.requireManagerOrAdmin("faturar guia de remessa");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        DeliveryGuide guide = guideRepository.findForUpdate(guideId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada."));
        if (guide.getStatus() != DeliveryGuideStatus.APPROVED) {
            throw new BusinessRuleException("Só uma guia aprovada pode ser faturada. Estado actual: " + guide.getStatus().getLabel());
        }
        if (guide.getInvoiceId() != null) {
            throw new BusinessRuleException("A guia " + guide.getGuideNumber() + " já foi faturada na fatura " + guide.getInvoiceNumber() + ".");
        }
        Order order = guide.getOrderId() == null ? null : orderRepository.findForUpdate(guide.getOrderId(), companyId).orElse(null);

        Invoice invoice = toInvoice(guide, order);
        receivablesService.assertCreditAvailable(invoice.getClient(), invoice.getTotalAmount());
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));
        invoice = invoiceRepository.save(invoice);
        invoiceLifecycleService.publishSale(invoice, BigDecimal.ZERO, false);

        guide.setInvoiceId(invoice.getId());
        guide.setInvoiceNumber(invoice.getInvoiceNumber());
        guideRepository.save(guide);
        if (order != null) {
            order.setStatus(OrderStatus.BILLED.name());
            order.setInvoiceId(invoice.getId());
            orderRepository.save(order);
        }
        auditLogService.logCurrent("DELIVERY_GUIDE_BILL",
                "Guia de remessa " + guide.getGuideNumber() + " faturada em " + invoice.getInvoiceNumber() + ".");
        return invoiceLifecycleService.toDTO(invoice);
    }

    /**
     * Anulada a fatura, a encomenda volta a poder ser faturada. Com guia, a mercadoria continua
     * entregue e fica por faturar; sem guia, o stock foi reposto e a encomenda volta a pendente.
     */
    @EventListener
    @Transactional
    public void onInvoiceCancelled(InvoiceCancelledEvent event) {
        DeliveryGuide guide = guideRepository.findByCompanyIdAndInvoiceId(event.companyId(), event.invoiceId()).orElse(null);
        if (guide != null) {
            guide.setInvoiceId(null);
            guide.setInvoiceNumber(null);
            guideRepository.save(guide);
        }
        orderRepository.findFirstByCompanyIdAndInvoiceId(event.companyId(), event.invoiceId()).ifPresent(order -> {
            order.setStatus((guide == null ? OrderStatus.PENDING : OrderStatus.GUIDED).name());
            order.setInvoiceId(null);
            orderRepository.save(order);
        });
    }

    private Invoice toInvoice(DeliveryGuide guide, Order order) {
        Invoice invoice = new Invoice();
        invoice.setClient(guide.getClient());
        invoice.setCompany(guide.getCompany());
        invoice.setWarehouse(guide.getWarehouse());
        invoice.setDeliveryGuideId(guide.getId());
        if (guide.getWalkInName() != null && !guide.getWalkInName().isBlank()) {
            invoice.setCustomerName(guide.getWalkInName().trim());
        }
        LocalDate today = LocalDate.now();
        String terms = order == null ? null : order.getPaymentTerms();
        invoice.assignDueDate(today, clientAndCreditService.resolveDueDateFromTerms(terms, today, guide.getClient()));
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setSalesChannel(order == null ? SalesChannel.MANUAL : SalesChannel.ORDER);
        invoice.setCreatedBy(CurrentUserContext.getUsername());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (DeliveryGuideLine guideLine : guide.getLines()) {
            LineCalculator.LineAmounts amounts = LineCalculator.compute(guideLine.getUnitPrice(), guideLine.getQuantity(),
                    guideLine.getDiscountPercentage(), guideLine.getTaxRate());
            InvoiceLine line = new InvoiceLine();
            line.setProduct(guideLine.getProduct());
            line.setQuantity(guideLine.getQuantity());
            line.setUnitPrice(guideLine.getUnitPrice());
            line.setTaxRate(guideLine.getTaxRate());
            line.setDiscountPercentage(guideLine.getDiscountPercentage());
            line.setLineTotal(amounts.total());
            line.setBatchNumber(guideLine.getBatchNumber());
            line.setSerialNumber(guideLine.getSerialNumber());
            line.setUnitCost(guideLine.getProduct().getPurchasePrice());
            invoice.addLine(line);
            subtotal = subtotal.add(amounts.net());
            tax = tax.add(amounts.tax());
        }
        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(tax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(subtotal.add(tax).setScale(2, RoundingMode.HALF_UP));
        return invoice;
    }
}
