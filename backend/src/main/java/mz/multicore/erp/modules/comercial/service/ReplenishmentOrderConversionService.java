package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ConvertOrderToTransferRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.service.InternalReplenishmentService.ReplenishmentStatus;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReplenishmentOrderConversionService {

    private final OrderRepository orderRepository;
    private final StockTransferService stockTransferService;
    private final ComercialService comercialService;

    public ReplenishmentOrderConversionService(OrderRepository orderRepository,
                                               StockTransferService stockTransferService,
                                               ComercialService comercialService) {
        this.orderRepository = orderRepository;
        this.stockTransferService = stockTransferService;
        this.comercialService = comercialService;
    }

    public StockTransferDTO convertToTransfer(Long orderId, ConvertOrderToTransferRequest request) {
        Order order = requireOrder(orderId);

        if (!OrderKind.orDefault(order.getKind()).usesWarehouseTransfer()) {
            throw new BusinessRuleException("Não é possível transferir: a encomenda "
                    + order.getOrderNumber() + " é uma venda a cliente, não uma reposição interna. "
                    + "A mercadoria de uma venda sai por factura ou por guia de remessa.");
        }
        if (order.getStockTransferId() != null) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber()
                    + " já gerou a transferência " + order.getTransferNumber() + ".");
        }
        if (!isReadyToTransfer(order.getStatus())) {
            throw new BusinessRuleException(explainNotReady(order));
        }
        if (order.getDestinationWarehouse() == null) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber()
                    + " não diz para que armazém vai a mercadoria.");
        }

        List<CreateStockTransferLineRequest> lines = order.getLines().stream()
                .map(line -> new CreateStockTransferLineRequest(line.getProduct().getId(), line.getQuantity()))
                .toList();

        String driver = request == null ? null : (request.driverName() != null ? request.driverName() : request.responsible());
        String plate = request == null ? null : (request.vehiclePlate() != null ? request.vehiclePlate() : request.vehicle());

        StockTransferDTO transfer = stockTransferService.create(new CreateStockTransferRequest(
                order.getCompany().getId(),
                order.getWarehouse().getId(),
                order.getDestinationWarehouse().getId(),
                request == null ? null : request.responsible(),
                request == null ? null : request.vehicle(),
                notesFor(order, request),
                lines,
                driver,
                plate));

        StockTransferDTO linked = stockTransferService.linkToOrder(
                transfer.id(), order.getId(), order.getOrderNumber());

        order.setStockTransferId(transfer.id());
        order.setTransferNumber(transfer.transferNumber());
        order.setStatus(ReplenishmentStatus.TRANSFER_PENDING);
        orderRepository.save(order);

        return linked;
    }

    public OrderDTO recordOrderFromTransfer(Long transferId) {
        StockTransferDTO transfer = stockTransferService.findById(transferId);

        if (!"APPROVED".equalsIgnoreCase(transfer.status())) {
            throw new BusinessRuleException("Só se regista o pedido de uma transferência já aprovada. "
                    + "A transferência " + transfer.transferNumber() + " ainda não moveu mercadoria nenhuma.");
        }
        if (transfer.orderId() != null) {
            throw new BusinessRuleException("A transferência " + transfer.transferNumber()
                    + " já tem a encomenda " + transfer.orderNumber() + " registada.");
        }

        Order order = comercialService.createReplenishmentRecord(transfer);
        stockTransferService.linkToOrder(transfer.id(), order.getId(), order.getOrderNumber());
        return comercialService.toDTO(order);
    }

    public void onTransferResolved(StockTransferResolvedEvent event) {
        if (event.orderId() == null) return;
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            if (event.outcome().releasesOrder()) {
                order.setStatus(ReplenishmentStatus.PENDING);
                order.setStockTransferId(null);
                order.setTransferNumber(null);
            } else {
                order.setStatus(ReplenishmentStatus.TRANSFERRED);
            }
            orderRepository.save(order);
        });
    }

    private Order requireOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        order.getLines().size();
        return order;
    }

    private boolean isReadyToTransfer(String status) {
        return ReplenishmentStatus.PENDING.equalsIgnoreCase(status)
                || ReplenishmentStatus.SEPARATED.equalsIgnoreCase(status);
    }

    private String explainNotReady(Order order) {
        String header = "Não é possível transferir a encomenda " + order.getOrderNumber() + ": ";
        return switch (order.getStatus() == null ? "" : order.getStatus().toUpperCase()) {
            case "AWAITING_SEPARATION" -> header + "os artigos ainda não foram recolhidos. "
                    + "Imprima a lista de separação e conclua a separação primeiro.";
            case "IN_SEPARATION" -> header + "a separação está a decorrer. "
                    + "Marque-a como separada quando os artigos estiverem conferidos.";
            case ReplenishmentStatus.TRANSFER_PENDING -> header + "já está a aguardar aprovação da transferência.";
            case ReplenishmentStatus.TRANSFERRED -> header + "a mercadoria já foi transferida.";
            case "CANCELLED" -> header + "a encomenda foi cancelada.";
            default -> header + "o estado actual não permite transferir.";
        };
    }

    private String notesFor(Order order, ConvertOrderToTransferRequest request) {
        String typed = request == null ? null : request.notes();
        String origin = "Reposição da encomenda " + order.getOrderNumber() + ".";
        return typed == null || typed.isBlank() ? origin : origin + " " + typed.trim();
    }
}
