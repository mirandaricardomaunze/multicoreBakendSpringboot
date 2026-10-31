package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.dto.OrderPickingDetailsDTO;
import mz.multicore.erp.modules.comercial.dto.OrderPickingLineDTO;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.OrderEventRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.printing.OrderPickingPrintService;
import mz.multicore.erp.modules.printing.OrderPickingSheetPrintService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de impressão de guias de separação (POS térmico e folha A4) e autorização de reimpressão.
 */
@Service
public class OrderPickingCoordinator {

    private final OrderRepository orderRepository;
    private final OrderEventRepository eventRepository;
    private final OrderPickingPrintService printService;
    private final OrderPickingSheetPrintService sheetPrintService;
    private final AppUserService appUserService;

    public OrderPickingCoordinator(OrderRepository orderRepository,
                                   OrderEventRepository eventRepository,
                                   OrderPickingPrintService printService,
                                   OrderPickingSheetPrintService sheetPrintService,
                                   AppUserService appUserService) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.printService = printService;
        this.sheetPrintService = sheetPrintService;
        this.appUserService = appUserService;
    }

    @Transactional
    public byte[] printForPicking(Long id, String terminal) {
        PermissionGuard.requireManagerOrAdmin("imprimir guia de separacao");
        Order order = requireSeparationCircuit(id, "imprimir a lista de separação");
        if (!OrderFulfillmentStatus.AWAITING_SEPARATION.name().equals(order.getStatus())) {
            throw new BusinessRuleException(explainWrongState(order, OrderFulfillmentStatus.AWAITING_SEPARATION, OrderFulfillmentStatus.IN_SEPARATION));
        }
        order.setStatus(OrderFulfillmentStatus.IN_SEPARATION.name());
        byte[] document = printService.render(id, false);
        registerPrint(order, terminal, "PICKING_PRINTED", "Primeira impressao; separacao iniciada.");
        return document;
    }

    @Transactional
    public byte[] reprint(Long id, ReprintAuthorizationRequest request) {
        PermissionGuard.requireManagerOrAdmin("reimprimir guia de separacao");
        Order order = requireSeparationCircuit(id, "reimprimir a lista de separação");
        if (!OrderFulfillmentStatus.IN_SEPARATION.name().equals(order.getStatus())) {
            throw new BusinessRuleException("A guia so pode ser reimpressa durante a separacao.");
        }
        if (CurrentUserContext.getUsername().equalsIgnoreCase(request.approverUsername())) {
            throw new BusinessRuleException("A reimpressao exige autorizacao de outro utilizador.");
        }
        AppUser approver = appUserService.authenticate(request.approverUsername(), request.approverPassword());
        AppUserCompanyAccess access = approver.findCompanyAccess(CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("O autorizador nao pertence a empresa activa."));
        if (!"MANAGER".equalsIgnoreCase(access.getRole()) && !"ADMIN".equalsIgnoreCase(access.getRole())) {
            throw new BusinessRuleException("O autorizador deve ser gerente ou administrador.");
        }
        byte[] document = printService.render(id, true);
        registerPrint(order, request.terminalName(), "PICKING_REPRINTED",
                "Autorizado por " + approver.getUsername() + ". Motivo: " + request.reason().trim());
        return document;
    }

    @Transactional
    public byte[] printForPickingA4(Long id, String terminal) {
        PermissionGuard.requireManagerOrAdmin("imprimir guia de separacao A4");
        Order order = requireOrder(id);
        if (OrderFulfillmentStatus.AWAITING_SEPARATION.name().equals(order.getStatus())
                || "PENDING".equalsIgnoreCase(order.getStatus())) {
            order.setStatus(OrderFulfillmentStatus.IN_SEPARATION.name());
        }
        byte[] document = sheetPrintService.renderA4(id, false);
        registerPrint(order, terminal, "PICKING_A4_PRINTED", "Guia de separação A4 impressa; separação iniciada.");
        return document;
    }

    @Transactional(readOnly = true)
    public OrderPickingDetailsDTO getPickingDetails(Long id) {
        Order order = requireOrder(id);
        BigDecimal totalBoxes = BigDecimal.ZERO;
        List<OrderPickingLineDTO> lines = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            Product p = line.getProduct();
            String ref = p != null && p.getReference() != null ? p.getReference() : "—";
            String barcode = p != null && p.getBarcode() != null ? p.getBarcode() : "—";
            String name = p != null ? p.getName() : "Artigo";
            int pkgsPerBox = p != null ? p.getPackagesPerBox() : 1;
            int unitsPerPkg = p != null ? p.getUnitsPerPackage() : 1;
            BigDecimal boxes = BigDecimal.ZERO;
            if (pkgsPerBox > 1 || unitsPerPkg > 1) {
                long totalUnits = (long) pkgsPerBox * (unitsPerPkg > 0 ? unitsPerPkg : 1);
                if (totalUnits > 0 && line.getQuantity() != null) {
                    boxes = line.getQuantity().divide(BigDecimal.valueOf(totalUnits), 2, RoundingMode.HALF_UP);
                }
            }
            totalBoxes = totalBoxes.add(boxes);
            lines.add(new OrderPickingLineDTO(
                    line.getId(), p != null ? p.getId() : null, ref, barcode, name,
                    line.getQuantity(), line.getSeparatedQuantity() != null ? line.getSeparatedQuantity() : BigDecimal.ZERO,
                    pkgsPerBox, unitsPerPkg, boxes, line.getBatchNumber(), null,
                    order.getWarehouse() != null ? order.getWarehouse().getName() : "Armazém"
            ));
        }
        return new OrderPickingDetailsDTO(
                order.getId(), order.getOrderNumber(),
                order.getClient() != null ? order.getClient().getName() : "Consumidor Final",
                order.getWarehouse() != null ? order.getWarehouse().getName() : "Central",
                order.getStatus(), lines, lines.size(), totalBoxes.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public Order requireOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new BusinessRuleException("Pedido nao encontrado."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        order.getLines().size();
        return order;
    }

    public Order requireSeparationCircuit(Long id, String action) {
        Order order = requireOrder(id);
        if (!OrderKind.orDefault(order.getKind()).usesSeparationFlow()) {
            throw new BusinessRuleException("Não é possível " + action + ": a encomenda "
                    + order.getOrderNumber() + " é do tipo \"" + OrderKind.FORMAL_ORDER.label()
                    + "\" e não passa pelo armazém — aprova-se e fatura-se directamente.");
        }
        return order;
    }

    private void registerPrint(Order order, String terminal, String type, String details) {
        order.setPrintCount(order.getPrintCount() + 1);
        order.setPrintedAt(LocalDateTime.now());
        order.setLastPrintedBy(CurrentUserContext.getUsername());
        orderRepository.save(order);
        recordEvent(order, type, order.getStatus(), order.getStatus(), terminal, details);
    }

    public void recordEvent(Order order, String type, String previous, String next, String terminal, String details) {
        OrderEvent event = new OrderEvent();
        event.setOrder(order); event.setEventType(type); event.setPreviousStatus(previous); event.setNewStatus(next);
        event.setActorRole(CurrentUserContext.getRole()); event.setTerminalName(terminal); event.setDetails(details);
        event.setCreatedBy(CurrentUserContext.getUsername());
        eventRepository.save(event);
    }

    private String explainWrongState(Order order, OrderFulfillmentStatus expected, OrderFulfillmentStatus target) {
        String current = order.getStatus();
        String header = "Não é possível marcar como " + OrderStatusLabel.of(target.name()) + ": ";
        if (!OrderFulfillmentStatus.isFulfillmentStatus(current)) {
            return header + "esta encomenda (" + order.getOrderNumber() + ") não foi criada para "
                    + "separação — está em \"" + current + "\" e fatura-se directamente.";
        }
        if (OrderFulfillmentStatus.AWAITING_SEPARATION.name().equals(current)) {
            return header + "a encomenda " + order.getOrderNumber() + " ainda aguarda separação. "
                    + "Imprima primeiro a lista de separação — é isso que dá início ao trabalho no armazém.";
        }
        return header + "a encomenda " + order.getOrderNumber() + " está em \"" + OrderStatusLabel.of(current)
                + "\" e o passo esperado era \"" + OrderStatusLabel.of(expected.name()) + "\".";
    }
}
