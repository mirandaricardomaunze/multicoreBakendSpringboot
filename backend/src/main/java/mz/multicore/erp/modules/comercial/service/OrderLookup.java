package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de encomendas e dos documentos que as rodeiam (cotações e guias de entrega) para os
 * outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2, granularidade: agregado mais próximo). As entidades
 * devolvidas servem só para relações JPA e renderização dentro do backend e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class OrderLookup {

    static final String ORDER_NOT_FOUND = "Encomenda não encontrada.";

    private final OrderRepository orderRepository;
    private final QuotationRepository quotationRepository;
    private final DeliveryGuideRepository deliveryGuideRepository;

    public OrderLookup(OrderRepository orderRepository, QuotationRepository quotationRepository,
                       DeliveryGuideRepository deliveryGuideRepository) {
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
        this.deliveryGuideRepository = deliveryGuideRepository;
    }

    // ---- Encomendas ----

    /** Encomenda pelo id; {@code BusinessRuleException("Encomenda não encontrada.")} se não existir. */
    public Order requireOrder(Long id) {
        return findOrder(id).orElseThrow(() -> new BusinessRuleException(ORDER_NOT_FOUND));
    }

    public Optional<Order> findOrder(Long id) {
        return id == null ? Optional.empty() : orderRepository.findById(id);
    }

    /** Encomenda pelo id, só se pertencer à empresa indicada. */
    public Optional<Order> findOrderForCompany(Long id, Long companyId) {
        return findOrder(id).filter(o -> o.getCompany() != null && o.getCompany().getId() != null
                && o.getCompany().getId().equals(companyId));
    }

    public List<Order> listOrdersForCompany(Long companyId) {
        return orderRepository.findByCompanyId(companyId);
    }

    /** Primeira encomenda da empresa ligada à factura indicada. */
    public Optional<Order> findOrderByInvoice(Long companyId, Long invoiceId) {
        return orderRepository.findFirstByCompanyIdAndInvoiceId(companyId, invoiceId);
    }

    // ---- Cotações ----

    /** Cotações da empresa, da mais recente para a mais antiga. */
    public List<Quotation> listQuotationsForCompany(Long companyId) {
        return quotationRepository.findByCompanyIdOrderByQuotationDateDesc(companyId);
    }

    public Optional<Quotation> findQuotationForCompany(Long id, Long companyId) {
        return quotationRepository.findByIdAndCompanyId(id, companyId);
    }

    /** Cotação da empresa com as linhas já carregadas (impressão, preços do POS). */
    public Optional<Quotation> findQuotationWithLines(Long id, Long companyId) {
        return quotationRepository.findByIdWithLinesAndCompanyId(id, companyId);
    }

    // ---- Guias de entrega ----

    public List<DeliveryGuide> listDeliveryGuidesForCompany(Long companyId) {
        return deliveryGuideRepository.findByCompanyId(companyId);
    }

    /** Guia de entrega da empresa com as linhas já carregadas (impressão, manifestos de frota). */
    public Optional<DeliveryGuide> findDeliveryGuideWithLines(Long id, Long companyId) {
        return deliveryGuideRepository.findByIdWithLinesAndCompanyId(id, companyId);
    }
}
