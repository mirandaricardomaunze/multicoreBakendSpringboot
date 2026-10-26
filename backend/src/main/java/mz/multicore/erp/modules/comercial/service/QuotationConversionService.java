package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversão documental fiel de propostas comerciais em encomendas de cliente e facturas directas.
 */
@Service
public class QuotationConversionService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuotationRepository quotationRepository;
    private final AuditLogService auditLogService;
    private final ComercialService comercialService;
    private final QuotationQueryService queryService;
    private final QuotationLifecycleService lifecycleService;

    public QuotationConversionService(QuotationRepository quotationRepository,
                                      AuditLogService auditLogService,
                                      ComercialService comercialService,
                                      QuotationQueryService queryService,
                                      QuotationLifecycleService lifecycleService) {
        this.quotationRepository = quotationRepository;
        this.auditLogService = auditLogService;
        this.comercialService = comercialService;
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
    }

    @Transactional
    public OrderDTO convert(Long id) {
        Quotation quotation = queryService.load(id);
        LocalDate today = LocalDate.now();

        if (quotation.getStatus() == QuotationStatus.CONVERTED) {
            throw new BusinessRuleException("Esta cotação já foi convertida na encomenda "
                    + quotation.getOrderNumber() + ".");
        }
        if (!quotation.getStatus().isOpen()) {
            throw new BusinessRuleException("Apenas cotações em aberto podem ser convertidas. "
                    + "Estado actual: " + quotation.getStatus().getLabel() + ".");
        }
        if (quotation.isExpired(today)) {
            throw new BusinessRuleException("A cotação " + quotation.getQuotationNumber()
                    + " caducou a " + quotation.getValidUntil().format(DATE_FMT)
                    + ". Estenda a validade antes de converter — o preço proposto deixou de estar garantido.");
        }

        List<OrderLine> orderLines = new ArrayList<>();
        for (QuotationLine quoted : quotation.getLines()) {
            OrderLine line = new OrderLine();
            line.setProduct(quoted.getProduct());
            line.setQuantity(quoted.getQuantity());
            line.setUnitPrice(quoted.getUnitPrice());
            line.setTaxRate(quoted.getTaxRate());
            line.setDiscountPercentage(quoted.getDiscountPercentage());
            line.setLineTotal(quoted.getLineTotal());
            orderLines.add(line);
        }

        OrderDTO order = comercialService.placeOrder(
                quotation.getCompany(),
                quotation.getClient(),
                quotation.getWarehouse(),
                quotation.getWalkInName(),
                orderLines,
                OrderKind.FORMAL_ORDER,
                quotation.agreedTerms());

        if (quotation.getDecidedAt() == null) {
            lifecycleService.stampDecision(quotation);
        }
        quotation.setStatus(QuotationStatus.CONVERTED);
        quotation.setOrderId(order.id());
        quotation.setOrderNumber(order.orderNumber());
        quotationRepository.save(quotation);

        auditLogService.logCurrent("QUOTATION_CONVERT",
                "Cotação " + quotation.getQuotationNumber() + " convertida na encomenda "
                        + order.orderNumber() + ". Total: " + order.totalAmount() + " MT.");
        return order;
    }

    @Transactional
    public InvoiceDTO convertToInvoice(Long id) {
        Quotation quotation = queryService.load(id);
        LocalDate today = LocalDate.now();

        if (quotation.getStatus() == QuotationStatus.CONVERTED) {
            String target = quotation.getInvoiceNumber() != null ? "na factura " + quotation.getInvoiceNumber()
                    : (quotation.getOrderNumber() != null ? "na encomenda " + quotation.getOrderNumber() : "");
            throw new BusinessRuleException("Esta cotação já foi convertida " + target + ".");
        }
        if (!quotation.getStatus().isOpen()) {
            throw new BusinessRuleException("Apenas cotações em aberto podem ser convertidas. "
                    + "Estado actual: " + quotation.getStatus().getLabel() + ".");
        }
        if (quotation.isExpired(today)) {
            throw new BusinessRuleException("A cotação " + quotation.getQuotationNumber()
                    + " caducou a " + quotation.getValidUntil().format(DATE_FMT)
                    + ". Estenda a validade antes de converter — o preço proposto deixou de estar garantido.");
        }

        InvoiceDTO invoice = comercialService.createInvoiceFromQuotation(quotation);

        if (quotation.getDecidedAt() == null) {
            lifecycleService.stampDecision(quotation);
        }
        quotation.setStatus(QuotationStatus.CONVERTED);
        quotation.setInvoiceId(invoice.id());
        quotation.setInvoiceNumber(invoice.invoiceNumber());
        quotationRepository.save(quotation);

        auditLogService.logCurrent("QUOTATION_CONVERT_INVOICE",
                "Cotação " + quotation.getQuotationNumber() + " convertida directamente na factura "
                        + invoice.invoiceNumber() + ". Total: " + invoice.totalAmount() + " MT.");
        return invoice;
    }
}
