package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideLineDTO;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Guia de Remessa ao cliente, gerada a partir de uma encomenda aprovada. Máquina de estados no
 * molde da Guia de Transferência: nasce {@code PENDING_APPROVAL} e o stock (SALE) só sai na
 * APROVAÇÃO. Rejeitar/cancelar nunca movem stock e devolvem a encomenda a faturável.
 *
 * <p>Caminhos separados (decisão do utilizador): uma encomenda vira guia OU fatura, nunca as duas.
 * Por isso gerar a guia tira a encomenda do estado {@code PENDING} — deixando de ser faturável por
 * {@code ComercialService.billOrder} (cujo guard exige {@code PENDING}). Ver
 * docs/GUIA_REMESSA_ENCOMENDA_SPEC.md.</p>
 */
@Service
public class DeliveryGuideService {

    private final DeliveryGuideRepository guideRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public DeliveryGuideService(DeliveryGuideRepository guideRepository,
                                OrderRepository orderRepository,
                                InventoryService inventoryService,
                                DocumentNumberService documentNumberService,
                                AuditLogService auditLogService) {
        this.guideRepository = guideRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    /** Cria a guia a partir de uma encomenda aprovada ({@code PENDING}). Não move stock. */
    @Transactional
    public DeliveryGuideDTO createFromOrder(CreateDeliveryGuideRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());

        // A guia de remessa entrega a um cliente. Uma reposição interna não tem cliente — a
        // mercadoria muda de armazém e continua a ser da empresa. O documento dela é a guia de
        // transferência. Ver docs/REPOSICAO_INTERNA_SPEC.md §3 (R2).
        if (mz.multicore.erp.modules.comercial.model.OrderKind
                .orDefault(order.getKind()).usesWarehouseTransfer()) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber()
                    + " é uma reposição interna e não tem cliente a quem entregar. "
                    + "Converta-a em transferência entre armazéns.");
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException(
                    "Só encomendas aprovadas e ainda não expedidas/faturadas podem gerar guia. "
                            + "Estado actual da encomenda: " + order.getStatus());
        }

        DeliveryGuide guide = new DeliveryGuide();
        guide.setGuideNumber(documentNumberService.next(DocumentSeries.DELIVERY_GUIDE));
        guide.setGuideDate(LocalDateTime.now());
        guide.setCompany(order.getCompany());
        guide.setOrderId(order.getId());
        guide.setOrderNumber(order.getOrderNumber());
        guide.setClient(order.getClient());
        guide.setWalkInName(order.getWalkInName());
        guide.setWarehouse(order.getWarehouse());
        guide.setTotalAmount(order.getTotalAmount());
        guide.setStatus(DeliveryGuideStatus.PENDING_APPROVAL);
        guide.setResponsible(blankToNull(request.responsible()));
        guide.setVehicle(blankToNull(request.vehicle()));
        guide.setNotes(blankToNull(request.notes()));
        guide.setCreatedBy(CurrentUserContext.getUsername());

        for (OrderLine ol : order.getLines()) {
            DeliveryGuideLine line = new DeliveryGuideLine();
            line.setProduct(ol.getProduct());
            line.setQuantity(ol.getQuantity());
            line.setUnitPrice(ol.getUnitPrice());
            line.setTaxRate(ol.getTaxRate());
            line.setDiscountPercentage(ol.getDiscountPercentage());
            line.setLineTotal(ol.getLineTotal());
            line.setBatchNumber(ol.getBatchNumber());
            line.setSerialNumber(ol.getSerialNumber());
            guide.addLine(line);
        }

        DeliveryGuide saved = guideRepository.save(guide);

        // Trava a encomenda: sai de PENDING, deixando de ser faturável (caminhos separados).
        order.setStatus("GUIDE_PENDING");
        order.setDeliveryGuideId(saved.getId());
        orderRepository.save(order);

        auditLogService.logCurrent("DELIVERY_GUIDE_CREATE",
                "Guia de remessa " + saved.getGuideNumber() + " criada a partir da encomenda "
                        + order.getOrderNumber() + ".");
        return toDTO(saved);
    }

    /** Aprova a guia e dá saída ao stock (SALE, FEFO) — só MANAGER/ADMIN. */
    @Transactional
    public DeliveryGuideDTO approve(Long id) {
        DeliveryGuide guide = load(id);
        PermissionGuard.requireManagerOrAdmin("aprovar ou rejeitar guias de remessa");
        if (guide.getStatus() != DeliveryGuideStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser aprovadas. Estado actual: " + guide.getStatus().getLabel());
        }

        for (DeliveryGuideLine line : guide.getLines()) {
            String desc = String.format("Saída Guia %s (Encomenda %s) - Cliente %s",
                    guide.getGuideNumber(), guide.getOrderNumber(), clientLabel(guide));
            inventoryService.registerMovement(
                    line.getProduct(),
                    guide.getWarehouse(),
                    line.getQuantity().negate(),
                    "SALE",
                    line.getBatchNumber(),
                    line.getSerialNumber(),
                    desc
            );
        }

        guide.setStatus(DeliveryGuideStatus.APPROVED);
        guide.setApprovedBy(CurrentUserContext.getUsername());
        guide.setApprovedAt(LocalDateTime.now());
        DeliveryGuide saved = guideRepository.save(guide);

        markOrder(saved.getOrderId(), "GUIDED", saved.getId());

        auditLogService.logCurrent("DELIVERY_GUIDE_APPROVE",
                "Guia de remessa " + saved.getGuideNumber() + " aprovada — stock expedido.");
        return toDTO(saved);
    }

    /** Rejeita a guia pendente (sem stock) e devolve a encomenda a faturável. Só MANAGER/ADMIN. */
    @Transactional
    public DeliveryGuideDTO reject(Long id, String rejectionReason) {
        DeliveryGuide guide = load(id);
        PermissionGuard.requireManagerOrAdmin("aprovar ou rejeitar guias de remessa");
        if (guide.getStatus() != DeliveryGuideStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser rejeitadas. Estado actual: " + guide.getStatus().getLabel());
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o motivo da rejeição.");
        }
        guide.setStatus(DeliveryGuideStatus.REJECTED);
        guide.setRejectionReason(rejectionReason);
        guide.setApprovedBy(CurrentUserContext.getUsername());
        guide.setApprovedAt(LocalDateTime.now());
        DeliveryGuide saved = guideRepository.save(guide);

        releaseOrder(saved.getOrderId());

        auditLogService.logCurrent("DELIVERY_GUIDE_REJECT",
                "Guia de remessa " + saved.getGuideNumber() + " rejeitada. Motivo: " + rejectionReason);
        return toDTO(saved);
    }

    /** Cancela uma guia ainda pendente (sem stock) e devolve a encomenda a faturável. */
    @Transactional
    public DeliveryGuideDTO cancel(Long id) {
        DeliveryGuide guide = load(id);
        if (guide.getStatus() == DeliveryGuideStatus.APPROVED) {
            throw new BusinessRuleException("Guias já aprovadas não podem ser canceladas — o stock já saiu.");
        }
        if (guide.getStatus() != DeliveryGuideStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser canceladas. Estado actual: " + guide.getStatus().getLabel());
        }
        guide.setStatus(DeliveryGuideStatus.CANCELLED);
        DeliveryGuide saved = guideRepository.save(guide);

        releaseOrder(saved.getOrderId());

        auditLogService.logCurrent("DELIVERY_GUIDE_CANCEL",
                "Guia de remessa " + saved.getGuideNumber() + " cancelada.");
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryGuideDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return guideRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryGuideDTO findById(Long id) {
        return toDTO(load(id));
    }

    @Transactional(readOnly = true)
    public DeliveryGuide loadForPrint(Long id) {
        return load(id);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private DeliveryGuide load(Long id) {
        return guideRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada."));
    }

    /** Devolve a encomenda ao estado faturável (PENDING) quando a guia é rejeitada/cancelada. */
    private void releaseOrder(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("PENDING");
            order.setDeliveryGuideId(null);
            orderRepository.save(order);
        });
    }

    private void markOrder(Long orderId, String status, Long guideId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            order.setDeliveryGuideId(guideId);
            orderRepository.save(order);
        });
    }

    private String clientLabel(DeliveryGuide guide) {
        if (guide.getWalkInName() != null && !guide.getWalkInName().isBlank()) {
            return guide.getWalkInName();
        }
        return guide.getClient() != null ? guide.getClient().getName() : "";
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private DeliveryGuideDTO toDTO(DeliveryGuide g) {
        var logistics = mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.calculate(g.getLines().stream()
                .map(line -> new mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.Input(
                        line.getQuantity(), line.getProduct().getGrossUnitWeightKg())).toList());
        List<DeliveryGuideLineDTO> lineDTOs = java.util.stream.IntStream.range(0, g.getLines().size())
                .mapToObj(index -> {
                    DeliveryGuideLine l = g.getLines().get(index);
                    var share = logistics.get(index);
                    return new DeliveryGuideLineDTO(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getLineTotal(),
                        l.getBatchNumber(),
                        Math.max(1, l.getProduct().getUnitsPerBox()),
                        l.getProduct().getNetUnitWeightKg(), l.getProduct().getGrossUnitWeightKg(),
                        share.lineWeightKg(), share.quantityPercentage(), share.weightPercentage());
                }).toList();
        return new DeliveryGuideDTO(
                g.getId(),
                g.getGuideNumber(),
                g.getGuideDate(),
                g.getCompany() != null ? g.getCompany().getId() : null,
                g.getOrderId(),
                g.getOrderNumber(),
                g.getClient() != null ? g.getClient().getId() : null,
                clientLabel(g),
                g.getWarehouse() != null ? g.getWarehouse().getId() : null,
                g.getWarehouse() != null ? g.getWarehouse().getName() : null,
                g.getStatus() != null ? g.getStatus().name() : null,
                g.getTotalAmount() != null ? g.getTotalAmount() : BigDecimal.ZERO,
                g.getResponsible(),
                g.getVehicle(),
                g.getNotes(),
                g.getApprovedBy(),
                g.getApprovedAt(),
                g.getRejectionReason(),
                lineDTOs
        );
    }
}
