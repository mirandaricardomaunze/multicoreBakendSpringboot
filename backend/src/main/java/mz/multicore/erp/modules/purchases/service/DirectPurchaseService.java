package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DirectPurchaseService {

    private final DirectPurchaseRegistrationService registrationService;
    private final DirectPurchaseQueryService queryService;

    @Autowired
    public DirectPurchaseService(
            DirectPurchaseRegistrationService registrationService,
            DirectPurchaseQueryService queryService
    ) {
        this.registrationService = registrationService;
        this.queryService = queryService;
    }

    public DirectPurchaseService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            InventoryService inventoryService,
            FinanceService financeService,
            DocumentNumberService documentNumberService,
            ApplicationEventPublisher eventPublisher
    ) {
        this(
                new DirectPurchaseRegistrationService(
                        supplierRepository, purchaseRepository, productRepository, warehouseRepository,
                        companyRepository, inventoryService, financeService, documentNumberService, eventPublisher),
                new DirectPurchaseQueryService(purchaseRepository)
        );
    }

    @Transactional
    public Purchase createPurchase(CreatePurchaseRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        return registrationService.createPurchase(request);
    }

    @Transactional
    public PurchaseDTO createPurchaseDTO(CreatePurchaseRequest request) {
        return toDTO(createPurchase(request));
    }

    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.getPurchasesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDTO> findPurchasesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.findPurchasesByCompany(companyId);
    }

    public PurchaseDTO toDTO(Purchase p) {
        return queryService.toDTO(p);
    }
}
