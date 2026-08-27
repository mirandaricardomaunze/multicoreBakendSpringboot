package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;
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
    private final mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository discrepancyRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository orderRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            InventoryService inventoryService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository discrepancyRepository
    ) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.inventoryService = inventoryService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.discrepancyRepository = discrepancyRepository;
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
            // A conferência tem de fechar contra o que foi encomendado: boas + danificadas +
            // em falta não podem ultrapassar o que ainda estava por receber. Sem esta guarda,
            // registar 5 danificados numa linha com 2 por receber inventava divergências.
            BigDecimal accounted = item.totalAccountedFor();
            if (accounted.compareTo(outstanding) > 0) {
                throw new BusinessRuleException(String.format(
                        "Linha %s: %s recebidas + %s danificadas + %s em falta excedem as %s por receber.",
                        line.getProduct().getName(), qty.toPlainString(),
                        item.safeDamaged().toPlainString(), item.safeMissing().toPlainString(),
                        outstanding.toPlainString()));
            }
            receiveLine(order, line, qty);
            recordDiscrepancies(order, line, item);
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

    /**
     * Grava o que correu mal nesta linha da conferência.
     *
     * <p>Mercadoria danificada e mercadoria em falta são registos <b>separados</b>: a primeira
     * chegou (o fornecedor vai querer receber por ela), a segunda não. Metê-las no mesmo saco
     * perdia a única informação que serve para reclamar.
     *
     * <p>A quantidade em falta <b>conta como recebida</b> na linha da encomenda, apesar de não
     * entrar em stock: é o fecho curto — declarar que aquelas unidades não virão, para a
     * encomenda poder fechar em vez de ficar eternamente por receber.
     */
    private void recordDiscrepancies(PurchaseOrder order, PurchaseOrderLine line,
                                     ReceivePurchaseOrderRequest.ReceiveLine item) {
        if (!item.hasDiscrepancy()) return;

        if (item.safeDamaged().signum() > 0) {
            discrepancyRepository.save(newDiscrepancy(order, line,
                    mz.multicore.erp.modules.purchases.model.DiscrepancyType.DAMAGED,
                    item.safeDamaged(), item.notes()));
        }
        if (item.safeMissing().signum() > 0) {
            discrepancyRepository.save(newDiscrepancy(order, line,
                    mz.multicore.erp.modules.purchases.model.DiscrepancyType.MISSING,
                    item.safeMissing(), item.notes()));
            // Fecho curto: a linha dá-se por resolvida também nas unidades que não vieram.
            line.setReceivedQuantity(nz(line.getReceivedQuantity()).add(item.safeMissing()));
        }

        auditLogService.logCurrent("PURCHASE_ORDER_DISCREPANCY", String.format(
                "Encomenda %s, artigo %s: %s danificada(s), %s em falta. %s",
                order.getOrderNumber(), line.getProduct().getName(),
                item.safeDamaged().toPlainString(), item.safeMissing().toPlainString(),
                item.notes() == null ? "" : item.notes()));
    }

    private mz.multicore.erp.modules.purchases.model.GoodsReceiptDiscrepancy newDiscrepancy(
            PurchaseOrder order, PurchaseOrderLine line,
            mz.multicore.erp.modules.purchases.model.DiscrepancyType type,
            BigDecimal quantity, String notes) {
        var discrepancy = new mz.multicore.erp.modules.purchases.model.GoodsReceiptDiscrepancy();
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
        discrepancy.setOccurredOn(java.time.LocalDate.now());
        discrepancy.setCreatedBy(CurrentUserContext.getUsername());
        return discrepancy;
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
