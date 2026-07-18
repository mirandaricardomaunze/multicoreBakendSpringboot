package com.phcpro.modules.comercial.service;

import com.phcpro.modules.approvals.service.ApprovalCallback;
import com.phcpro.modules.comercial.model.Order;
import com.phcpro.modules.comercial.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liga a Engine de Aprovações ao ciclo da encomenda. Uma encomenda nasce em
 * {@code PENDING_APPROVAL}; ao ser aprovada fica {@code PENDING} (faturável via
 * {@code ComercialService.billOrder}); ao ser rejeitada fica {@code CANCELLED}.
 * O stock só é movido na faturação — a aprovação não toca em inventário.
 */
@Component
public class OrderApprovalCallback implements ApprovalCallback {

    private final OrderRepository orderRepository;

    public OrderApprovalCallback(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean supports(String documentType) {
        return "ORDER".equalsIgnoreCase(documentType);
    }

    @Override
    @Transactional
    public void onApproved(Long documentId) {
        orderRepository.findById(documentId).ifPresent(order -> {
            // Só promove encomendas ainda à espera de aprovação — nunca reabre uma já faturada.
            if ("PENDING_APPROVAL".equalsIgnoreCase(order.getStatus())) {
                order.setStatus("PENDING");
                orderRepository.save(order);
            }
        });
    }

    @Override
    @Transactional
    public void onRejected(Long documentId, String reason) {
        orderRepository.findById(documentId).ifPresent(order -> {
            if ("PENDING_APPROVAL".equalsIgnoreCase(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
            }
        });
    }
}
