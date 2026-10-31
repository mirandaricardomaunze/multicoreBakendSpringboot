package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static mz.multicore.erp.modules.comercial.service.DeliveryGuideQueryService.clientLabel;
import static mz.multicore.erp.modules.comercial.service.DeliveryGuideQueryService.toDTO;

@Service
public class DeliveryGuideLifecycleService {
    private final DeliveryGuideRepository guideRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final DeliveryGuideQueryService queryService;

    public DeliveryGuideLifecycleService(DeliveryGuideRepository guideRepository,
                                         OrderRepository orderRepository,
                                         InventoryService inventoryService,
                                         AuditLogService auditLogService,
                                         DeliveryGuideQueryService queryService) {
        this.guideRepository = guideRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
    }

    @Transactional
    public DeliveryGuideDTO approve(Long id) {
        DeliveryGuide guide = queryService.load(id);
        PermissionGuard.requireManagerOrAdmin("aprovar ou rejeitar guias de remessa");
        if (guide.getStatus() != DeliveryGuideStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser aprovadas. Estado actual: " + guide.getStatus().getLabel());
        }

        for (DeliveryGuideLine line : guide.getLines()) {
            String origin = guide.getOrderNumber() == null
                    ? "Guia avulsa" : "Encomenda " + guide.getOrderNumber();
            String desc = String.format("Saída Guia %s (%s) - Cliente %s",
                    guide.getGuideNumber(), origin, clientLabel(guide));
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

        if (saved.getOrderId() != null) markOrder(saved.getOrderId(), "GUIDED", saved.getId());

        auditLogService.logCurrent("DELIVERY_GUIDE_APPROVE",
                "Guia de remessa " + saved.getGuideNumber() + " aprovada — stock expedido.");
        return toDTO(saved);
    }

    @Transactional
    public DeliveryGuideDTO reject(Long id, String rejectionReason) {
        DeliveryGuide guide = queryService.load(id);
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

        if (saved.getOrderId() != null) releaseOrder(saved.getOrderId());

        auditLogService.logCurrent("DELIVERY_GUIDE_REJECT",
                "Guia de remessa " + saved.getGuideNumber() + " rejeitada. Motivo: " + rejectionReason);
        return toDTO(saved);
    }

    @Transactional
    public DeliveryGuideDTO cancel(Long id) {
        DeliveryGuide guide = queryService.load(id);
        if (guide.getStatus() == DeliveryGuideStatus.APPROVED) {
            throw new BusinessRuleException("Guias já aprovadas não podem ser canceladas — o stock já saiu.");
        }
        if (guide.getStatus() != DeliveryGuideStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser canceladas. Estado actual: " + guide.getStatus().getLabel());
        }
        guide.setStatus(DeliveryGuideStatus.CANCELLED);
        DeliveryGuide saved = guideRepository.save(guide);

        if (saved.getOrderId() != null) releaseOrder(saved.getOrderId());

        auditLogService.logCurrent("DELIVERY_GUIDE_CANCEL",
                "Guia de remessa " + saved.getGuideNumber() + " cancelada.");
        return toDTO(saved);
    }

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
}
