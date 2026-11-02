package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.model.*;
import mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderReceiptRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Executor transacional de recepções de encomendas a fornecedor com idempotência SHA-256,
 * separação analítica de divergências (danificados vs em falta), entrada em stock e dívida ao fornecedor.
 */
@Service
public class PurchaseOrderReceiptExecutor {

    private final PurchaseOrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final GoodsReceiptDiscrepancyRepository discrepancyRepository;
    private final PurchaseOrderReceiptRepository receiptRepository;
    private final PurchaseOrderPayableRegistrar payableRegistrar;

    public PurchaseOrderReceiptExecutor(
            PurchaseOrderRepository orderRepository,
            InventoryService inventoryService,
            AuditLogService auditLogService,
            GoodsReceiptDiscrepancyRepository discrepancyRepository,
            PurchaseOrderReceiptRepository receiptRepository,
            PurchaseOrderPayableRegistrar payableRegistrar
    ) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.discrepancyRepository = discrepancyRepository;
        this.receiptRepository = receiptRepository;
        this.payableRegistrar = payableRegistrar;
    }

    @Transactional
    public PurchaseOrder receiveOrder(PurchaseOrder order) {
        PermissionGuard.requireManagerOrAdmin("receber encomenda a fornecedor");
        requireReceivable(order);

        Map<PurchaseOrderLine, BigDecimal> accepted = new LinkedHashMap<>();
        for (PurchaseOrderLine line : order.getLines()) {
            BigDecimal outstanding = outstanding(line);
            if (outstanding.signum() > 0) {
                receiveLine(order, line, outstanding, accepted);
            }
        }

        recomputeStatus(order);
        order = orderRepository.save(order);
        payableRegistrar.register(order, accepted);
        auditLogService.logCurrent("PURCHASE_ORDER_RECEIVE",
                "Encomenda " + order.getOrderNumber() + " recebida (" + order.getLines().size() + " linhas)");
        return order;
    }

    @Transactional
    public PurchaseOrder receivePartial(Long id, ReceivePurchaseOrderRequest request) {
        PermissionGuard.requireManagerOrAdmin("receber encomenda a fornecedor");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        PurchaseOrder order = orderRepository.lockByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        String key = request.idempotencyKey();
        String fingerprint = key == null ? null : receiptFingerprint(request);
        if (key != null) {
            Optional<PurchaseOrderReceipt> previous =
                    receiptRepository.findByPurchaseOrderIdAndIdempotencyKey(id, key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException(
                            "A chave de idempotência desta recepção já foi usada com dados diferentes.");
                }
                return order;
            }
        }
        requireReceivable(order);

        Map<PurchaseOrderLine, BigDecimal> accepted = new LinkedHashMap<>();
        for (ReceivePurchaseOrderRequest.ReceiveLine item : request.lines()) {
            PurchaseOrderLine line = order.getLines().stream()
                    .filter(l -> l.getId() != null && l.getId().equals(item.lineId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Linha não pertence a esta encomenda: " + item.lineId()));
            BigDecimal qty = item.quantity();
            if (qty == null || qty.signum() <= 0) {
                throw new BusinessRuleException("Quantidade a receber tem de ser positiva.");
            }
            BigDecimal outstanding = outstanding(line);
            BigDecimal accounted = item.totalAccountedFor();
            if (accounted.compareTo(outstanding) > 0) {
                throw new BusinessRuleException(String.format(
                        "Linha %s: %s recebidas + %s danificadas + %s em falta excedem as %s por receber.",
                        line.getProduct().getName(), qty.toPlainString(),
                        item.safeDamaged().toPlainString(), item.safeMissing().toPlainString(),
                        outstanding.toPlainString()));
            }
            receiveLine(order, line, qty, accepted);
            recordDiscrepancies(order, line, item);
        }

        recomputeStatus(order);
        order = orderRepository.save(order);
        payableRegistrar.register(order, accepted);
        if (key != null) {
            PurchaseOrderReceipt receipt = new PurchaseOrderReceipt();
            receipt.setPurchaseOrder(order);
            receipt.setIdempotencyKey(key);
            receipt.setRequestFingerprint(fingerprint);
            receipt.setReceivedAt(LocalDateTime.now());
            receipt.setCreatedBy(CurrentUserContext.getUsername());
            receiptRepository.save(receipt);
        }
        auditLogService.logCurrent("PURCHASE_ORDER_RECEIVE_PARTIAL",
                "Encomenda " + order.getOrderNumber() + " — recepção parcial (estado " + order.getStatus() + ")");
        return order;
    }

    private void recordDiscrepancies(PurchaseOrder order, PurchaseOrderLine line,
                                     ReceivePurchaseOrderRequest.ReceiveLine item) {
        if (!item.hasDiscrepancy()) return;

        if (item.safeDamaged().signum() > 0) {
            discrepancyRepository.save(newDiscrepancy(order, line,
                    DiscrepancyType.DAMAGED,
                    item.safeDamaged(), item.notes()));
        }
        if (item.safeMissing().signum() > 0) {
            discrepancyRepository.save(newDiscrepancy(order, line,
                    DiscrepancyType.MISSING,
                    item.safeMissing(), item.notes()));
            line.setReceivedQuantity(nz(line.getReceivedQuantity()).add(item.safeMissing()));
        }

        auditLogService.logCurrent("PURCHASE_ORDER_DISCREPANCY", String.format(
                "Encomenda %s, artigo %s: %s danificada(s), %s em falta. %s",
                order.getOrderNumber(), line.getProduct().getName(),
                item.safeDamaged().toPlainString(), item.safeMissing().toPlainString(),
                item.notes() == null ? "" : item.notes()));
    }

    private GoodsReceiptDiscrepancy newDiscrepancy(
            PurchaseOrder order, PurchaseOrderLine line,
            DiscrepancyType type,
            BigDecimal quantity, String notes) {
        var discrepancy = new GoodsReceiptDiscrepancy();
        discrepancy.setCompany(order.getCompany());
        discrepancy.setPurchaseOrder(order);
        discrepancy.setPurchaseOrderLine(line);
        discrepancy.setProduct(line.getProduct());
        discrepancy.setSupplierId(order.getSupplier() == null ? null : order.getSupplier().getId());
        discrepancy.setSupplierName(order.getSupplier() == null ? null : order.getSupplier().getName());
        discrepancy.setType(type);
        discrepancy.setQuantity(quantity);
        discrepancy.setUnitPrice(line.getUnitPrice());
        discrepancy.setNotes(notes);
        discrepancy.setOccurredOn(LocalDate.now());
        discrepancy.setCreatedBy(CurrentUserContext.getUsername());
        return discrepancy;
    }

    private void receiveLine(PurchaseOrder order, PurchaseOrderLine line, BigDecimal qty,
                             Map<PurchaseOrderLine, BigDecimal> accepted) {
        String desc = String.format("Recepção Encomenda %s - Fornecedor %s",
                order.getOrderNumber(), order.getSupplier().getName());
        inventoryService.registerMovement(
                line.getProduct(),
                order.getWarehouse(),
                qty,
                "PURCHASE",
                line.getBatchNumber(),
                line.getSerialNumber(),
                desc,
                line.getExpirationDate());
        line.setReceivedQuantity(nz(line.getReceivedQuantity()).add(qty));
        accepted.merge(line, qty, BigDecimal::add);
    }

    private void recomputeStatus(PurchaseOrder order) {
        boolean allComplete = order.getLines().stream().allMatch(l -> outstanding(l).signum() <= 0);
        if (allComplete) {
            order.setStatus(PurchaseOrder.RECEIVED);
            order.setReceivedAt(LocalDateTime.now());
        } else {
            order.setStatus(PurchaseOrder.PARTIALLY_RECEIVED);
        }
    }

    public BigDecimal outstanding(PurchaseOrderLine line) {
        return nz(line.getQuantity()).subtract(nz(line.getReceivedQuantity()));
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void requireReceivable(PurchaseOrder order) {
        if (!PurchaseOrder.ORDERED.equals(order.getStatus())
                && !PurchaseOrder.PARTIALLY_RECEIVED.equals(order.getStatus())) {
            throw new BusinessRuleException(
                    "Só encomendas por receber (ORDERED ou PARTIALLY_RECEIVED) podem ser recebidas.");
        }
    }

    private static String receiptFingerprint(ReceivePurchaseOrderRequest request) {
        StringBuilder canonical = new StringBuilder(320);
        request.lines().stream()
                .sorted(java.util.Comparator.comparing(ReceivePurchaseOrderRequest.ReceiveLine::lineId))
                .forEach(line -> {
                    appendFingerprintPart(canonical, line.lineId());
                    appendFingerprintPart(canonical, canonicalDecimal(line.quantity()));
                    appendFingerprintPart(canonical, canonicalDecimal(line.safeDamaged()));
                    appendFingerprintPart(canonical, canonicalDecimal(line.safeMissing()));
                    appendFingerprintPart(canonical, line.notes());
                });
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }

    private static String canonicalDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static void appendFingerprintPart(StringBuilder target, Object value) {
        String text = value == null ? "<null>" : value.toString().trim();
        target.append(text.length()).append(':').append(text).append('|');
    }
}
