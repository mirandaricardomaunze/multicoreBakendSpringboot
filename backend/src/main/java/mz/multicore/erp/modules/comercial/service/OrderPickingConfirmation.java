package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.dto.OrderPickingConfirmationLineRequest;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras puras da conferência de separação: em que estado se pode conferir e como as quantidades
 * conferidas passam para as linhas. Sem IO — quem chama grava e regista o evento.
 */
final class OrderPickingConfirmation {

    private OrderPickingConfirmation() {
    }

    /** Só se confere o que aguarda ou está em separação. {@code PENDING} é um pedido criado sem reserva. */
    static void requireConfirmable(Order order) {
        String status = order.getStatus();
        if (!OrderStatus.is(status, OrderStatus.PENDING, OrderStatus.AWAITING_SEPARATION, OrderStatus.IN_SEPARATION)) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " está em \""
                    + OrderStatusLabel.of(status) + "\": só se confere o que aguarda ou está em separação.");
        }
    }

    /**
     * Sem linhas, tudo saiu completo. Com linhas, a conferência tem de dizer quanto saiu de <b>cada</b>
     * linha: uma linha esquecida não pode virar "separado zero" em silêncio, nem uma linha de outra
     * encomenda ser ignorada sem aviso.
     */
    static void apply(Order order, List<OrderPickingConfirmationLineRequest> confirmations) {
        if (confirmations == null || confirmations.isEmpty()) {
            order.getLines().forEach(line -> line.setSeparatedQuantity(line.getQuantity()));
            return;
        }
        Map<Long, OrderPickingConfirmationLineRequest> byLine = new HashMap<>();
        for (OrderPickingConfirmationLineRequest confirmation : confirmations) {
            if (byLine.put(confirmation.lineId(), confirmation) != null) {
                throw new BusinessRuleException("A linha " + confirmation.lineId() + " foi conferida mais de uma vez.");
            }
        }
        for (OrderLine line : order.getLines()) {
            OrderPickingConfirmationLineRequest confirmation = byLine.remove(line.getId());
            if (confirmation == null) {
                throw new BusinessRuleException("Falta conferir " + line.getProduct().getName()
                        + ". Indique a quantidade separada de todas as linhas, mesmo que seja zero.");
            }
            BigDecimal confirmed = confirmation.confirmedQuantity();
            if (confirmed == null || confirmed.signum() < 0 || confirmed.compareTo(line.getQuantity()) > 0) {
                throw new BusinessRuleException("A quantidade separada de " + line.getProduct().getName()
                        + " tem de estar entre zero e a quantidade encomendada.");
            }
            line.setSeparatedQuantity(confirmed);
            if (order.isReservationActive()) {
                // O que não saiu da prateleira deixa de estar prometido: fica livre para outras vendas.
                line.setReservedQuantity(confirmed);
            }
            if (confirmation.batchNumber() != null && !confirmation.batchNumber().isBlank()) {
                line.setBatchNumber(confirmation.batchNumber().trim());
            }
        }
        if (!byLine.isEmpty()) {
            throw new BusinessRuleException("A conferência inclui linhas que não pertencem à encomenda "
                    + order.getOrderNumber() + ".");
        }
    }
}
