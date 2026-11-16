package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Saída de stock de uma venda que não vem de encomenda: fatura directa, cotação convertida,
 * fatura aprovada após desconto especial, e POS.
 *
 * <p>Antes de registar a saída, confirma que não leva stock reservado para pedidos de separação
 * (a mesma regra da faturação de encomendas, em {@link OrderStockAvailability}). Sem isto o balcão
 * vendia o que o armazém já tinha separado para outro cliente.
 */
@Component
public class SaleStockIssuer {

    private final InventoryService inventoryService;
    private final OrderLineRepository orderLineRepository;

    public SaleStockIssuer(@Lazy InventoryService inventoryService, OrderLineRepository orderLineRepository) {
        this.inventoryService = inventoryService;
        this.orderLineRepository = orderLineRepository;
    }

    /** Verifica o stock livre de todas as linhas e só depois regista a saída de cada uma. */
    public void issue(Invoice invoice, Warehouse warehouse, String description) {
        OrderStockAvailability.require(null, warehouse, invoice.getLines().stream()
                .map(line -> new OrderStockAvailability.Outgoing(line.getProduct(), line.getQuantity())).toList(),
                inventoryService, orderLineRepository);
        for (InvoiceLine line : invoice.getLines()) {
            inventoryService.registerMovement(line.getProduct(), warehouse, line.getQuantity().negate(),
                    "SALE", line.getBatchNumber(), line.getSerialNumber(), description);
        }
    }
}
