package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.UpdatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.PurchaseOrderLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Gestor do ciclo de vida comercial da encomenda a fornecedor (criação, alteração e cancelamento).
 */
@Service
public class PurchaseOrderLifecycleService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductLookup productLookup;
    private final WarehouseLookup warehouseLookup;
    private final CompanyLookup companyLookup;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public PurchaseOrderLifecycleService(
            PurchaseOrderRepository orderRepository,
            SupplierRepository supplierRepository,
            ProductLookup productLookup,
            WarehouseLookup warehouseLookup,
            CompanyLookup companyLookup,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.productLookup = productLookup;
        this.warehouseLookup = warehouseLookup;
        this.companyLookup = companyLookup;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PurchaseOrder createOrder(CreatePurchaseOrderRequest request) {
        CurrentUserContext.requireCompany(request.companyId());

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));
        if (supplier.getCompany() == null || !request.companyId().equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        if (!supplier.isActive()) {
            throw new BusinessRuleException("Fornecedor inactivo não pode receber encomendas.");
        }
        Warehouse warehouse = warehouseLookup.require(request.warehouseId());
        if (warehouse.getCompany() == null || !request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Company company = companyLookup.require(request.companyId());

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(supplier);
        order.setWarehouse(warehouse);
        order.setCompany(company);
        order.setExpectedDate(request.expectedDate());
        order.setNotes(request.notes());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(PurchaseOrder.ORDERED);
        order.setCreatedBy(CurrentUserContext.getUsername());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreatePurchaseOrderLineRequest lineReq : request.lines()) {
            Product product = productLookup.findForCompany(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            BigDecimal taxRate = lineReq.taxRate() != null ? lineReq.taxRate() : product.effectiveTaxRate();
            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    lineReq.unitPrice(), lineReq.quantity(), BigDecimal.ZERO, taxRate);

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(lineReq.unitPrice());
            line.setTaxRate(taxRate);
            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
            line.setExpirationDate(lineReq.expirationDate());
            line.setSerialNumber(lineReq.serialNumber());
            order.addLine(line);

            total = total.add(amounts.total());
            totalTax = totalTax.add(amounts.tax());
        }

        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        order.setOrderNumber(documentNumberService.next(DocumentSeries.PURCHASE_ORDER));

        order = orderRepository.save(order);
        auditLogService.logCurrent("PURCHASE_ORDER_CREATE",
                "Encomenda " + order.getOrderNumber() + " - Fornecedor " + supplier.getName());
        return order;
    }

    @Transactional
    public PurchaseOrder updateOrder(PurchaseOrder order, UpdatePurchaseOrderRequest request) {
        if (!PurchaseOrder.ORDERED.equals(order.getStatus())
                || order.getLines().stream().anyMatch(line -> (line.getReceivedQuantity() != null && line.getReceivedQuantity().signum() > 0))) {
            throw new BusinessRuleException(
                    "A encomenda só pode ser alterada antes de iniciar qualquer recepção de mercadoria.");
        }
        if (request.version() == null || request.version() != order.getVersion()) {
            throw new BusinessRuleException(
                    "A encomenda foi alterada por outro utilizador. Recarregue o documento antes de guardar.");
        }

        Long companyId = order.getCompany().getId();
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));
        if (supplier.getCompany() == null || !companyId.equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        if (!supplier.isActive()) {
            throw new BusinessRuleException("Fornecedor inactivo não pode receber encomendas.");
        }
        Warehouse warehouse = warehouseLookup.require(request.warehouseId());
        if (warehouse.getCompany() == null || !companyId.equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }

        order.setSupplier(supplier);
        order.setWarehouse(warehouse);
        order.setExpectedDate(request.expectedDate());
        order.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        order.getLines().clear();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (CreatePurchaseOrderLineRequest lineReq : request.lines()) {
            Product product = productLookup.findForCompany(lineReq.productId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));
            BigDecimal taxRate = lineReq.taxRate() != null ? lineReq.taxRate() : product.effectiveTaxRate();
            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    lineReq.unitPrice(), lineReq.quantity(), BigDecimal.ZERO, taxRate);
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setReceivedQuantity(BigDecimal.ZERO);
            line.setUnitPrice(lineReq.unitPrice());
            line.setTaxRate(taxRate);
            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
            line.setExpirationDate(lineReq.expirationDate());
            line.setSerialNumber(lineReq.serialNumber());
            order.addLine(line);
            total = total.add(amounts.total());
            totalTax = totalTax.add(amounts.tax());
        }
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));

        PurchaseOrder saved = orderRepository.saveAndFlush(order);
        auditLogService.logCurrent("PURCHASE_ORDER_UPDATE",
                "Encomenda " + saved.getOrderNumber() + " actualizada antes da recepção. Total: "
                        + saved.getTotalAmount() + " MT.");
        return saved;
    }

    @Transactional
    public PurchaseOrder cancelOrder(PurchaseOrder order, String reason) {
        PermissionGuard.requireManagerOrAdmin("cancelar encomenda a fornecedor");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Indique o motivo do cancelamento.");
        }
        if (!PurchaseOrder.ORDERED.equals(order.getStatus())
                && !PurchaseOrder.PARTIALLY_RECEIVED.equals(order.getStatus())) {
            throw new BusinessRuleException(
                    "Só encomendas por receber (ORDERED ou PARTIALLY_RECEIVED) podem ser canceladas.");
        }
        order.setStatus(PurchaseOrder.CANCELLED);
        order.setCancellationReason(reason);
        order = orderRepository.save(order);
        auditLogService.logCurrent("PURCHASE_ORDER_CANCEL",
                "Encomenda " + order.getOrderNumber() + " cancelada: " + reason);
        return order;
    }
}
