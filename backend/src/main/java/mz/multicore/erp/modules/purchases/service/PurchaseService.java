package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestrador de Compras e Gestão de Fornecedores (Fase 112).
 * Decomposto em colaboradores especializados para conformidade estrita com SOLID/SRP.
 * Ver docs/PURCHASE_SERVICE_DECOMPOSITION_SPEC.md.
 */
@Service
public class PurchaseService {

    private final SupplierManagementService supplierService;
    private final DirectPurchaseService directPurchaseService;
    private final SupplierPaymentService supplierPaymentService;

    @Autowired
    public PurchaseService(SupplierManagementService supplierService,
                           DirectPurchaseService directPurchaseService,
                           SupplierPaymentService supplierPaymentService) {
        this.supplierService = supplierService;
        this.directPurchaseService = directPurchaseService;
        this.supplierPaymentService = supplierPaymentService;
    }

    /**
     * Construtor retrocompatível preservado para compatibilidade integral com testes legados.
     */
    public PurchaseService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            ProductLookup productLookup,
            WarehouseRepository warehouseRepository,
            CompanyLookup companyLookup,
            InventoryService inventoryService,
            FinanceService financeService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        this(new SupplierManagementService(supplierRepository, companyLookup, auditLogService),
                createDirectPurchaseService(supplierRepository, purchaseRepository, productLookup,
                        warehouseRepository, companyLookup, inventoryService, financeService,
                        documentNumberService, eventPublisher),
                new SupplierPaymentService(purchaseRepository, financeService, eventPublisher, auditLogService,
                        createDirectPurchaseService(supplierRepository, purchaseRepository, productLookup,
                                warehouseRepository, companyLookup, inventoryService, financeService,
                                documentNumberService, eventPublisher)));
    }

    private static DirectPurchaseService createDirectPurchaseService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            ProductLookup productLookup,
            WarehouseRepository warehouseRepository,
            CompanyLookup companyLookup,
            InventoryService inventoryService,
            FinanceService financeService,
            DocumentNumberService documentNumberService,
            ApplicationEventPublisher eventPublisher) {
        return new DirectPurchaseService(supplierRepository, purchaseRepository, productLookup,
                warehouseRepository, companyLookup, inventoryService, financeService,
                documentNumberService, eventPublisher);
    }

    @Transactional
    public Supplier createSupplier(String name, String taxId, String email, String address, Long companyId) {
        return supplierService.createSupplier(name, taxId, email, address, companyId);
    }

    @Transactional(readOnly = true)
    public List<Supplier> getSuppliersByCompany(Long companyId) {
        return supplierService.getSuppliersByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByCompany(Long companyId) {
        return directPurchaseService.getPurchasesByCompany(companyId);
    }

    @Transactional
    public Purchase createPurchase(CreatePurchaseRequest request) {
        return directPurchaseService.createPurchase(request);
    }

    @Transactional
    public SupplierDTO createSupplier(CreateSupplierRequest request) {
        return supplierService.createSupplier(request);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, CreateSupplierRequest request) {
        return supplierService.updateSupplier(id, request);
    }

    @Transactional
    public SupplierDTO setSupplierActive(Long id, Long companyId, boolean active) {
        return supplierService.setSupplierActive(id, companyId, active);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> searchSuppliers(Long companyId, String query) {
        return supplierService.searchSuppliers(companyId, query);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> findSuppliersByCompany(Long companyId) {
        return supplierService.findSuppliersByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDTO> findPurchasesByCompany(Long companyId) {
        return directPurchaseService.findPurchasesByCompany(companyId);
    }

    @Transactional
    public PurchaseDTO createPurchaseDTO(CreatePurchaseRequest request) {
        return directPurchaseService.createPurchaseDTO(request);
    }

    public SupplierDTO toDTO(Supplier s) {
        return supplierService.toDTO(s);
    }

    public PurchaseDTO toDTO(Purchase p) {
        return directPurchaseService.toDTO(p);
    }

    @Transactional(readOnly = true)
    public List<PayableDTO> findPayablesByCompany(Long companyId) {
        return supplierPaymentService.findPayablesByCompany(companyId);
    }

    @Transactional
    public PurchaseDTO registerSupplierPayment(Long purchaseId, BigDecimal amount, Long financeAccountId, String reference) {
        return supplierPaymentService.registerSupplierPayment(purchaseId, amount, financeAccountId, reference);
    }
}
