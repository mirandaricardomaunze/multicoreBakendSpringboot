package com.phcpro.modules.purchases.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import com.phcpro.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseOrderRequest;
import com.phcpro.modules.purchases.dto.PurchaseOrderDTO;
import com.phcpro.modules.purchases.dto.PurchaseOrderLineDTO;
import com.phcpro.modules.purchases.dto.ReceivePurchaseOrderRequest;
import com.phcpro.modules.purchases.model.PurchaseOrder;
import com.phcpro.modules.purchases.model.PurchaseOrderLine;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.repository.PurchaseOrderRepository;
import com.phcpro.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ciclo da encomenda a fornecedor: criar (ORDERED), receber (entra stock, RECEIVED) e cancelar.
 * Mirror da encomenda de cliente — não move stock até à recepção. Recepção/cancelamento exigem
 * MANAGER/ADMIN e são auditados.
 */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final InventoryService inventoryService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public PurchaseOrderService(
            PurchaseOrderRepository orderRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            InventoryService inventoryService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.inventoryService = inventoryService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PurchaseOrderDTO createOrder(CreatePurchaseOrderRequest request) {
        CurrentUserContext.requireCompany(request.companyId());

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));
        if (supplier.getCompany() == null || !request.companyId().equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        if (!supplier.isActive()) {
            throw new BusinessRuleException("Fornecedor inactivo não pode receber encomendas.");
        }
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (warehouse.getCompany() == null || !request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

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
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            // Mesma regra da compra: taxa acordada com o fornecedor; sem ela, a do artigo.
            BigDecimal taxRate = lineReq.taxRate() != null
                    ? lineReq.taxRate()
                    : product.effectiveTaxRate();
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
        return toDTO(order);
    }

    /** Recebe tudo o que falta de cada linha (de ORDERED ou PARTIALLY_RECEIVED) e fecha RECEIVED. */
    @Transactional
    public PurchaseOrderDTO receiveOrder(Long id) {
        PurchaseOrder order = loadForActiveCompany(id);
        PermissionGuard.requireManagerOrAdmin("receber encomenda a fornecedor");
        requireReceivable(order);

        for (PurchaseOrderLine line : order.getLines()) {
            BigDecimal outstanding = outstanding(line);
            if (outstanding.signum() > 0) {
                receiveLine(order, line, outstanding);
            }
        }

        recomputeStatus(order);
        order = orderRepository.save(order);
        auditLogService.logCurrent("PURCHASE_ORDER_RECEIVE",
                "Encomenda " + order.getOrderNumber() + " recebida (" + order.getLines().size() + " linhas)");
        return toDTO(order);
    }

    /** Recebe apenas as quantidades indicadas por linha; a encomenda fica PARTIALLY_RECEIVED ou RECEIVED. */
    @Transactional
    public PurchaseOrderDTO receivePartial(Long id, ReceivePurchaseOrderRequest request) {
        PurchaseOrder order = loadForActiveCompany(id);
        PermissionGuard.requireManagerOrAdmin("receber encomenda a fornecedor");
        requireReceivable(order);

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
            if (qty.compareTo(outstanding) > 0) {
                throw new BusinessRuleException(String.format(
                        "Linha %s: não pode receber %s (em falta: %s).",
                        line.getProduct().getName(), qty.toPlainString(), outstanding.toPlainString()));
            }
            receiveLine(order, line, qty);
        }

        recomputeStatus(order);
        order = orderRepository.save(order);
        auditLogService.logCurrent("PURCHASE_ORDER_RECEIVE_PARTIAL",
                "Encomenda " + order.getOrderNumber() + " — recepção parcial (estado " + order.getStatus() + ")");
        return toDTO(order);
    }

    private void requireReceivable(PurchaseOrder order) {
        if (!PurchaseOrder.ORDERED.equals(order.getStatus())
                && !PurchaseOrder.PARTIALLY_RECEIVED.equals(order.getStatus())) {
            throw new BusinessRuleException(
                    "Só encomendas por receber (ORDERED ou PARTIALLY_RECEIVED) podem ser recebidas.");
        }
    }

    /** Regista a entrada de stock de {@code qty} numa linha e soma à quantidade recebida. */
    private void receiveLine(PurchaseOrder order, PurchaseOrderLine line, BigDecimal qty) {
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
    }

    /** Deriva o estado da encomenda a partir do recebido de cada linha. */
    private void recomputeStatus(PurchaseOrder order) {
        boolean allComplete = order.getLines().stream().allMatch(l -> outstanding(l).signum() <= 0);
        if (allComplete) {
            order.setStatus(PurchaseOrder.RECEIVED);
            order.setReceivedAt(LocalDateTime.now());
        } else {
            order.setStatus(PurchaseOrder.PARTIALLY_RECEIVED);
        }
    }

    private BigDecimal outstanding(PurchaseOrderLine line) {
        return nz(line.getQuantity()).subtract(nz(line.getReceivedQuantity()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Transactional
    public PurchaseOrderDTO cancelOrder(Long id, String reason) {
        PurchaseOrder order = loadForActiveCompany(id);
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
        return toDTO(order);
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

    private PurchaseOrder loadForActiveCompany(Long id) {
        PurchaseOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany() != null ? order.getCompany().getId() : null);
        return order;
    }

    private PurchaseOrderDTO toDTO(PurchaseOrder o) {
        List<PurchaseOrderLineDTO> lines = o.getLines().stream().map(l -> new PurchaseOrderLineDTO(
                l.getId(),
                l.getProduct().getId(),
                l.getProduct().getName(),
                l.getProduct().getSku(),
                l.getQuantity(),
                nz(l.getReceivedQuantity()),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getLineTotal(),
                l.getBatchNumber(),
                l.getExpirationDate(),
                l.getSerialNumber()
        )).toList();
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
                lines
        );
    }
}
