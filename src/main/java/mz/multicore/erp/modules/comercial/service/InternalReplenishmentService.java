package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ConvertOrderToTransferRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reposição interna: a encomenda de uma loja ao armazém que a abastece, e a transferência que a
 * cumpre. Ver {@code docs/REPOSICAO_INTERNA_SPEC.md}.
 *
 * <p>Os dois documentos existiam sem se conhecerem. A transferência criava-se do zero — origem,
 * destino, e produto a produto — pelo que uma reposição de quarenta artigos era digitada quarenta
 * vezes a partir de um papel, e não ficava registo de quem pediu o quê.
 *
 * <p><b>O que este serviço não faz é tão importante como o que faz:</b> não move stock. A
 * mercadoria muda de armazém uma só vez, na aprovação da transferência. Aqui só se liga um
 * documento ao outro.
 */
@Service
public class InternalReplenishmentService {

    private final OrderRepository orderRepository;
    private final StockTransferService stockTransferService;
    private final ComercialService comercialService;

    public InternalReplenishmentService(OrderRepository orderRepository,
                                        StockTransferService stockTransferService,
                                        ComercialService comercialService) {
        this.orderRepository = orderRepository;
        this.stockTransferService = stockTransferService;
        this.comercialService = comercialService;
    }

    /**
     * Converte a encomenda de reposição na transferência que a vai cumprir (R6..R8).
     *
     * <p>Aceita {@code PENDING} e {@code SEPARATED}: separada é o fim natural do circuito do
     * armazém — o talão foi impresso, os artigos recolhidos e conferidos. É esse o momento em que
     * a carrinha carrega.
     */
    @Transactional
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

        // Linhas verbatim: artigo e quantidade. Uma transferência não tem preços — tem mercadoria.
        List<CreateStockTransferLineRequest> lines = order.getLines().stream()
                .map(line -> new CreateStockTransferLineRequest(line.getProduct().getId(), line.getQuantity()))
                .toList();

        StockTransferDTO transfer = stockTransferService.create(new CreateStockTransferRequest(
                order.getCompany().getId(),
                order.getWarehouse().getId(),
                order.getDestinationWarehouse().getId(),
                request == null ? null : request.responsible(),
                request == null ? null : request.vehicle(),
                notesFor(order, request),
                lines));

        StockTransferDTO linked = stockTransferService.linkToOrder(
                transfer.id(), order.getId(), order.getOrderNumber());

        order.setStockTransferId(transfer.id());
        order.setTransferNumber(transfer.transferNumber());
        order.setStatus(ReplenishmentStatus.TRANSFER_PENDING);
        orderRepository.save(order);

        return linked;
    }

    /**
     * Regista a encomenda em falta a partir de uma transferência já feita (R10..R12).
     *
     * <p>É <b>registo, não compromisso</b>: a mercadoria já mudou de armazém. A encomenda nasce já
     * cumprida e não move stock nenhum — mover seria contar a mesma saída duas vezes.
     */
    @Transactional
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

    /**
     * O que a transferência decide, a encomenda segue (R9).
     *
     * <p>Rejeitar ou cancelar não move stock, pelo que a encomenda volta a estar por cumprir e pode
     * ser corrigida e convertida de novo. Aprovar move a mercadoria e fecha o pedido.
     */
    @EventListener
    @Transactional
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

    /** Diz o passo que falta, não só que o estado está errado. */
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

    /** Estados da reposição interna, no vocabulário que a encomenda já usa. */
    static final class ReplenishmentStatus {
        static final String PENDING = "PENDING";
        static final String SEPARATED = "SEPARATED";
        static final String TRANSFER_PENDING = "TRANSFER_PENDING";
        static final String TRANSFERRED = "TRANSFERRED";

        private ReplenishmentStatus() {}
    }
}
