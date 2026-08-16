package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.validation.TaxIdValidator;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseLineDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public PurchaseService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            InventoryService inventoryService,
            FinanceService financeService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Supplier createSupplier(String name, String taxId, String email, String address, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        TaxIdValidator.validate(taxId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setTaxId(taxId);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setCompany(company);
        supplier.setCreatedBy("SYSTEM");

        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<Supplier> getSuppliersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return supplierRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return purchaseRepository.findByCompanyId(companyId);
    }

    @Transactional
    public Purchase createPurchase(CreatePurchaseRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));

        if (!request.companyId().equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        if (!supplier.isActive()) {
            throw new BusinessRuleException("Fornecedor inactivo não pode ser usado em compras.");
        }
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));

        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setWarehouse(warehouse);
        purchase.setCompany(company);
        purchase.setPurchaseDate(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreatePurchaseLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            PurchaseLine line = new PurchaseLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(lineReq.unitPrice());
            
            // IVA da COMPRA: manda a factura do fornecedor. Quando o operador não a indica,
            // aplica-se a taxa do artigo (e nunca 16% cego, que inflava o IVA dedutível em bens
            // isentos). Ver docs/IVA_TAXA_CANONICA_SPEC.md §4.
            BigDecimal taxRate = lineReq.taxRate() != null
                    ? lineReq.taxRate()
                    : product.effectiveTaxRate();
            line.setTaxRate(taxRate);

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    lineReq.unitPrice(), lineReq.quantity(), BigDecimal.ZERO, taxRate);

            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
            line.setExpirationDate(lineReq.expirationDate());
            line.setSerialNumber(lineReq.serialNumber());

            purchase.addLine(line);

            total = total.add(amounts.total());
            totalTax = totalTax.add(amounts.tax());

            // Register positive stock movement (entry into the batch with the received expiration date)
            String desc = String.format("Compra %s - Fornecedor %s", purchase.getPurchaseNumber(), supplier.getName());
            inventoryService.registerMovement(
                    product,
                    warehouse,
                    lineReq.quantity(),
                    "PURCHASE",
                    lineReq.batchNumber(),
                    lineReq.serialNumber(),
                    desc,
                    lineReq.expirationDate()
            );
        }

        purchase.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        purchase.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        
        purchase.setPurchaseNumber(documentNumberService.next(DocumentSeries.PURCHASE));

        boolean payNow = request.financeAccountId() != null;
        purchase.setAmountPaid(payNow ? purchase.getTotalAmount() : BigDecimal.ZERO);

        purchase = purchaseRepository.save(purchase);

        // Pagamento imediato → saída de tesouraria (CREDIT). Sem conta → fica a crédito (conta a pagar).
        if (payNow) {
            String description = "Pagamento Compra " + purchase.getPurchaseNumber() + " - Fornecedor " + supplier.getName();
            financeService.registerTransaction(request.financeAccountId(), "CREDIT", total, description);
        }

        return purchase;
    }

    @Transactional
    public SupplierDTO createSupplier(CreateSupplierRequest request) {
        Supplier supplier = createSupplier(request.name(), request.taxId(), request.email(), request.address(), request.companyId());
        supplier.setPhone(request.phone());
        supplier.setContactPerson(request.contactPerson());
        supplier = supplierRepository.save(supplier);
        auditLogService.logCurrent("SUPPLIER_CREATE", "Fornecedor " + supplier.getName() + " (" + supplier.getTaxId() + ")");
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, CreateSupplierRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        TaxIdValidator.validate(request.taxId());
        Supplier supplier = loadSupplierForCompany(id, request.companyId());
        supplier.setName(request.name());
        supplier.setTaxId(request.taxId());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setPhone(request.phone());
        supplier.setContactPerson(request.contactPerson());
        supplier = supplierRepository.save(supplier);
        auditLogService.logCurrent("SUPPLIER_UPDATE", "Fornecedor #" + id + " " + supplier.getName());
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO setSupplierActive(Long id, Long companyId, boolean active) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireManagerOrAdmin(active ? "activar fornecedor" : "desactivar fornecedor");
        Supplier supplier = loadSupplierForCompany(id, companyId);
        supplier.setActive(active);
        supplier = supplierRepository.save(supplier);
        auditLogService.logCurrent("SUPPLIER_STATUS",
                "Fornecedor #" + id + " -> " + (active ? "ACTIVO" : "INACTIVO"));
        return toDTO(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> searchSuppliers(Long companyId, String query) {
        CurrentUserContext.requireCompany(companyId);
        String term = query == null ? "" : query.trim().toLowerCase();
        return supplierRepository.findByCompanyId(companyId).stream()
                .filter(s -> term.isEmpty()
                        || (s.getName() != null && s.getName().toLowerCase().contains(term))
                        || (s.getTaxId() != null && s.getTaxId().toLowerCase().contains(term)))
                .map(this::toDTO)
                .toList();
    }

    private Supplier loadSupplierForCompany(Long id, Long companyId) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));
        if (supplier.getCompany() == null || !companyId.equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        return supplier;
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> findSuppliersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return supplierRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseDTO> findPurchasesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return purchaseRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public PurchaseDTO createPurchaseDTO(CreatePurchaseRequest request) {
        return toDTO(createPurchase(request));
    }

    public SupplierDTO toDTO(Supplier s) {
        return new SupplierDTO(
                s.getId(),
                s.getName(),
                s.getTaxId(),
                s.getEmail(),
                s.getAddress(),
                s.getPhone(),
                s.getContactPerson(),
                s.isActive(),
                s.getCompany() != null ? s.getCompany().getId() : null
        );
    }

    public PurchaseDTO toDTO(Purchase p) {
        List<PurchaseLineDTO> lines = p.getLines().stream().map(l -> new PurchaseLineDTO(
                l.getId(),
                l.getProduct().getId(),
                l.getProduct().getName(),
                l.getProduct().getSku(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getLineTotal(),
                l.getBatchNumber(),
                l.getExpirationDate(),
                l.getSerialNumber()
        )).toList();
        return new PurchaseDTO(
                p.getId(),
                p.getPurchaseNumber(),
                p.getSupplier().getId(),
                p.getSupplier().getName(),
                p.getWarehouse() != null ? p.getWarehouse().getId() : null,
                p.getCompany() != null ? p.getCompany().getId() : null,
                p.getTotalAmount(),
                p.getTaxAmount(),
                p.getAmountPaid(),
                p.getStatus(),
                p.getPurchaseDate(),
                lines
        );
    }

    // ===== Contas a pagar a fornecedor (Fase 4) =====

    @Transactional(readOnly = true)
    public List<PayableDTO> findPayablesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return purchaseRepository.findByCompanyId(companyId).stream()
                .filter(p -> !"CANCELLED".equals(p.getStatus()))
                .filter(p -> p.getOutstanding().signum() > 0)
                .map(p -> new PayableDTO(
                        p.getId(),
                        p.getPurchaseNumber(),
                        p.getSupplier().getId(),
                        p.getSupplier().getName(),
                        p.getTotalAmount(),
                        p.getAmountPaid(),
                        p.getOutstanding(),
                        p.getPurchaseDate()))
                .toList();
    }

    @Transactional
    public PurchaseDTO registerSupplierPayment(Long purchaseId, BigDecimal amount, Long financeAccountId, String reference) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Valor do pagamento deve ser positivo.");
        }
        if (financeAccountId == null) {
            throw new BusinessRuleException("Conta de tesouraria é obrigatória.");
        }
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessRuleException("Fatura de compra não encontrada."));
        CurrentUserContext.requireCompany(purchase.getCompany() != null ? purchase.getCompany().getId() : null);
        if ("CANCELLED".equals(purchase.getStatus())) {
            throw new BusinessRuleException("Compra anulada não tem saldo a pagar.");
        }
        BigDecimal outstanding = purchase.getOutstanding();
        if (outstanding.signum() <= 0) {
            throw new BusinessRuleException("Esta compra já está totalmente paga.");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Valor excede o saldo em dívida (" + outstanding.toPlainString() + " MT).");
        }

        purchase.setAmountPaid(purchase.getAmountPaid().add(amount));
        purchase = purchaseRepository.save(purchase);

        String desc = "Pagamento a fornecedor " + purchase.getSupplier().getName()
                + " — Compra " + purchase.getPurchaseNumber()
                + (reference != null && !reference.isBlank() ? " (ref. " + reference + ")" : "");
        financeService.registerTransaction(financeAccountId, "CREDIT", amount, desc);
        auditLogService.logCurrent("SUPPLIER_PAYMENT",
                "Compra " + purchase.getPurchaseNumber() + " pagamento " + amount.toPlainString()
                        + " MT (saldo " + purchase.getOutstanding().toPlainString() + " MT)");
        return toDTO(purchase);
    }
}
