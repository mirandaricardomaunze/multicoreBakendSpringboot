package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Comandos de escrita de encomendas e cotações para os outros módulos (encomendas personalizadas, POS):
 * gravam por aqui, nunca pelo Repository do comercial (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Leitura: {@link OrderLookup}.
 */
@Service
@Transactional
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final QuotationRepository quotationRepository;

    public OrderCommandService(OrderRepository orderRepository, QuotationRepository quotationRepository) {
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Marca como convertida a cotação aberta da empresa que deu origem à venda POS {@code invoice}.
     * Devolve a cotação convertida, ou vazio se não existir ou já não estiver aberta (nada é gravado).
     */
    public Optional<Quotation> markQuotationConvertedByPos(Long quotationId, Long companyId, Invoice invoice) {
        return quotationRepository.findByIdAndCompanyId(quotationId, companyId)
                .filter(quotation -> quotation.getStatus().isOpen())
                .map(quotation -> {
                    quotation.setStatus(QuotationStatus.CONVERTED);
                    quotation.setInvoiceId(invoice.getId());
                    quotation.setInvoiceNumber(invoice.getInvoiceNumber());
                    if (quotation.getDecidedAt() == null) {
                        quotation.setDecidedAt(LocalDateTime.now());
                        quotation.setDecidedBy(invoice.getCreatedBy());
                    }
                    quotationRepository.save(quotation);
                    return quotation;
                });
    }
}
