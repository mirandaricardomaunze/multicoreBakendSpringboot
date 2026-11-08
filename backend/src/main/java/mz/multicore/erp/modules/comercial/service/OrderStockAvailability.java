package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Antes de a mercadoria de uma encomenda sair, confirma que não sai stock prometido a outro pedido.
 *
 * <p>Disponível = físico (com bloqueio da linha de stock) − reservas activas no armazém + a reserva
 * da própria encomenda, que é ela que a vai consumir. Sem isto, uma encomenda A4 (que não reserva)
 * levava o stock já separado para um pedido de separação, e o armazém só o descobria ao conferir.
 */
final class OrderStockAvailability {

    /** Uma saída de stock: o artigo e a quantidade. */
    record Outgoing(Product product, BigDecimal quantity) {}

    private OrderStockAvailability() {
    }

    /** @param order encomenda de origem, ou {@code null} quando a saída não vem de nenhuma (guia avulsa) */
    static void require(Order order, Warehouse warehouse, List<Outgoing> outgoing,
                        InventoryService inventoryService, OrderLineRepository orderLineRepository) {
        Map<Long, BigDecimal> ownReservation = new HashMap<>();
        if (order != null && order.isReservationActive() && order.getWarehouse() != null
                && order.getWarehouse().getId().equals(warehouse.getId())) {
            for (OrderLine line : order.getLines()) {
                ownReservation.merge(line.getProduct().getId(), zeroIfNull(line.getReservedQuantity()), BigDecimal::add);
            }
        }
        Map<Long, BigDecimal> needed = new LinkedHashMap<>();
        Map<Long, Product> products = new HashMap<>();
        for (Outgoing item : outgoing) {
            // Services and other untracked products never hold stock, matching registerMovement, which skips them.
            if (!item.product().isStockTracked()) continue;
            needed.merge(item.product().getId(), item.quantity(), BigDecimal::add);
            products.put(item.product().getId(), item.product());
        }
        for (Map.Entry<Long, BigDecimal> entry : needed.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal physical = zeroIfNull(inventoryService.lockAndGetPhysicalQuantity(productId, warehouse.getId()));
            BigDecimal reserved = zeroIfNull(orderLineRepository.sumActiveReservations(productId, warehouse.getId()));
            BigDecimal available = physical.subtract(reserved).add(ownReservation.getOrDefault(productId, BigDecimal.ZERO));
            if (available.compareTo(entry.getValue()) < 0) {
                throw new BusinessRuleException("Stock disponível insuficiente para " + products.get(productId).getName()
                        + " no armazém " + warehouse.getName() + ": precisa de " + DisplayNumbers.qty(entry.getValue())
                        + ", livre " + DisplayNumbers.qty(available.max(BigDecimal.ZERO))
                        + ". O restante está reservado para pedidos de separação.");
            }
        }
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
