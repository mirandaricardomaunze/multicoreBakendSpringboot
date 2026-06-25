package com.phcpro.modules.comercial.service;

import com.phcpro.modules.comercial.model.Order;
import com.phcpro.modules.comercial.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do callback que liga a Engine de Aprovações à encomenda. Aprovar promove a
 * encomenda de PENDING_APPROVAL para PENDING (faturável); rejeitar cancela. Nunca mexe
 * numa encomenda que já saiu do estado de aprovação.
 */
class OrderApprovalCallbackTest {

    private OrderRepository orderRepository;
    private OrderApprovalCallback callback;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        callback = new OrderApprovalCallback(orderRepository);
    }

    @Test
    void supports_apenasORDER() {
        assertTrue(callback.supports("ORDER"));
        assertTrue(callback.supports("order"));
        assertFalse(callback.supports("INVOICE"));
    }

    @Test
    void onApproved_promovePendingApproval_paraPending() {
        Order order = order(1L, "PENDING_APPROVAL");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        callback.onApproved(1L);

        assertEquals("PENDING", order.getStatus());
    }

    @Test
    void onRejected_cancelaPendingApproval() {
        Order order = order(1L, "PENDING_APPROVAL");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        callback.onRejected(1L, "fora de stock");

        assertEquals("CANCELLED", order.getStatus());
    }

    @Test
    void onApproved_encomendaJaFaturada_naoEReaberta() {
        Order order = order(1L, "BILLED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        callback.onApproved(1L);

        assertEquals("BILLED", order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    private Order order(long id, String status) {
        Order o = new Order();
        o.setId(id);
        o.setOrderNumber("EC-2026/1");
        o.setStatus(status);
        return o;
    }
}
