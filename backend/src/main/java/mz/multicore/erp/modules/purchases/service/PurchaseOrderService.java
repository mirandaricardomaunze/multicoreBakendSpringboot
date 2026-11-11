package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.UpdatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderReceiptRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orquestrador principal do ciclo de vida das encomendas a fornecedor.
 * Delega o ciclo comercial em {@link PurchaseOrderLifecycleService} e as recepções físicas
 * em {@link PurchaseOrderReceiptExecutor}.
 */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLifecycleService lifecycleService;
    private final PurchaseOrderReceiptExecutor receiptExecutor;

    public PurchaseOrderService(
            PurchaseOrderRepository orderRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyLookup companyLookup,
            InventoryService inventoryService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            GoodsReceiptDiscrepancyRepository discrepancyRepository,
            PurchaseOrderReceiptRepository receiptRepository,
            PurchaseOrderPayableRegistrar payableRegistrar
    ) {
        this(
                orderRepository,
                new PurchaseOrderLifecycleService(orderRepository, supplierRepository, productRepository,
                        warehouseRepository, companyLookup, documentNumberService, auditLogService),
                new PurchaseOrderReceiptExecutor(orderRepository, inventoryService, auditLogService,
                        discrepancyRepository, receiptRepository, payableRegistrar)
        );
    }

    @Autowired
    public PurchaseOrderService(
            PurchaseOrderRepository orderRepository,
            PurchaseOrderLifecycleService lifecycleService,
            PurchaseOrderReceiptExecutor receiptExecutor
    ) {
        this.orderRepository = orderRepository;
        this.lifecycleService = lifecycleService;
        this.receiptExecutor = receiptExecutor;
    }

    @Transactional
    public PurchaseOrderDTO createOrder(CreatePurchaseOrderRequest request) {
        return toDTO(lifecycleService.createOrder(request));
    }

    @Transactional
    public PurchaseOrderDTO updateOrder(Long id, UpdatePurchaseOrderRequest request) {
        PurchaseOrder order = loadForActiveCompany(id);
        return toDTO(lifecycleService.updateOrder(order, request));
    }

    @Transactional
    public PurchaseOrderDTO receiveOrder(Long id) {
        PurchaseOrder order = loadForActiveCompany(id);
        return toDTO(receiptExecutor.receiveOrder(order));
    }

    @Transactional
    public PurchaseOrderDTO receivePartial(Long id, ReceivePurchaseOrderRequest request) {
        return toDTO(receiptExecutor.receivePartial(id, request));
    }

    @Transactional
    public PurchaseOrderDTO cancelOrder(Long id, String reason) {
        PurchaseOrder order = loadForActiveCompany(id);
        return toDTO(lifecycleService.cancelOrder(order, reason));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDTO> findOrdersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return orderRepository.findByCompanyIdOrderByOrderDateDesc(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDTO> searchOrders(Long companyId, String query) {
        CurrentUserContext.requireCompany(companyId);
        String term = query == null ? "" : query.trim().toLowerCase();
        return orderRepository.findByCompanyIdOrderByOrderDateDesc(companyId).stream()
                .filter(o -> term.isEmpty()
                        || (o.getOrderNumber() != null && o.getOrderNumber().toLowerCase().contains(term))
                        || (o.getSupplier() != null && o.getSupplier().getName() != null
                            && o.getSupplier().getName().toLowerCase().contains(term)))
                .map(this::toDTO)
                .toList();
    }

    public PurchaseOrder loadForActiveCompany(Long id) {
        PurchaseOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany() != null ? order.getCompany().getId() : null);
        return order;
    }

    public PurchaseOrderDTO toDTO(PurchaseOrder o) {
        List<PurchaseOrderLineDTO> lines = o.getLines().stream().map(l -> new PurchaseOrderLineDTO(
                l.getId(),
                l.getProduct().getId(),
                l.getProduct().getName(),
                l.getProduct().getSku(),
                l.getQuantity(),
                PurchaseOrderReceiptExecutor.nz(l.getReceivedQuantity()),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getLineTotal(),
                l.getBatchNumber(),
                l.getExpirationDate(),
                l.getSerialNumber()
        )).toList();

        Set<String> bus = o.getLines().stream()
                .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getName() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        String buName = bus.isEmpty() ? null : (bus.size() == 1 ? bus.iterator().next() : "Misto");
        Long buId = bus.size() == 1 ? o.getLines().stream()
                .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getId() : null)
                .filter(Objects::nonNull).findFirst().orElse(null) : null;

        return new PurchaseOrderDTO(
                o.getId(),
                o.getOrderNumber(),
                o.getSupplier().getId(),
                o.getSupplier().getName(),
                o.getWarehouse() != null ? o.getWarehouse().getId() : null,
                o.getCompany() != null ? o.getCompany().getId() : null,
                o.getExpectedDate(),
                o.getOrderDate(),
                o.getReceivedAt(),
                o.getTotalAmount(),
                o.getTaxAmount(),
                o.getStatus(),
                o.getNotes(),
                lines,
                o.getVersion(),
                buId,
                buName
        );
    }
}
