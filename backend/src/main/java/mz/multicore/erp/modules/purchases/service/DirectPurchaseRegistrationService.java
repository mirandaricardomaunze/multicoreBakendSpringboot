package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class DirectPurchaseRegistrationService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductLookup productLookup;
    private final WarehouseLookup warehouseLookup;
    private final CompanyLookup companyLookup;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final DocumentNumberService documentNumberService;
    private final ApplicationEventPublisher eventPublisher;

    public DirectPurchaseRegistrationService(
            SupplierRepository supplierRepository, PurchaseRepository purchaseRepository,
            ProductLookup productLookup, WarehouseLookup warehouseLookup,
            CompanyLookup companyLookup, InventoryService inventoryService,
            FinanceService financeService, DocumentNumberService documentNumberService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.productLookup = productLookup;
        this.warehouseLookup = warehouseLookup;
        this.companyLookup = companyLookup;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
        this.documentNumberService = documentNumberService;
        this.eventPublisher = eventPublisher;
    }

    public Purchase createPurchase(CreatePurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));
        if (!request.companyId().equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        if (!supplier.isActive()) {
            throw new BusinessRuleException("Fornecedor inactivo não pode ser usado em compras.");
        }

        Warehouse warehouse = warehouseLookup.require(request.warehouseId());
        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }

        Company company = companyLookup.require(request.companyId());

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setWarehouse(warehouse);
        purchase.setCompany(company);
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setPurchaseNumber(documentNumberService.next(DocumentSeries.PURCHASE));

        BigDecimal total = BigDecimal.ZERO, totalTax = BigDecimal.ZERO;

        for (CreatePurchaseLineRequest lineReq : request.lines()) {
            Product product = productLookup.findForCompany(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            PurchaseLine line = new PurchaseLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(lineReq.unitPrice());

            BigDecimal taxRate = lineReq.taxRate() != null ? lineReq.taxRate() : product.effectiveTaxRate();
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

            String desc = String.format("Compra %s - Fornecedor %s", purchase.getPurchaseNumber(), supplier.getName());
            inventoryService.registerPurchase(
                    product, warehouse, lineReq.quantity(), lineReq.unitPrice(),
                    lineReq.batchNumber(), lineReq.serialNumber(), desc, lineReq.expirationDate()
            );
        }

        purchase.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        purchase.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));

        boolean payNow = request.financeAccountId() != null;
        purchase.setAmountPaid(payNow ? purchase.getTotalAmount() : BigDecimal.ZERO);
        purchase = purchaseRepository.save(purchase);

        boolean cashPayment = false;
        if (payNow) {
            String description = "Pagamento Compra " + purchase.getPurchaseNumber() + " - Fornecedor " + supplier.getName();
            financeService.registerTransaction(request.financeAccountId(), "CREDIT", total, description);
            cashPayment = financeService.accountType(request.financeAccountId()) == TreasuryAccountType.CASH;
        }

        eventPublisher.publishEvent(new PurchaseRegisteredEvent(
                request.companyId(), purchase.getId(), purchase.getPurchaseNumber(),
                purchase.getPurchaseDate().toLocalDate(),
                purchase.getTotalAmount().subtract(purchase.getTaxAmount()),
                purchase.getTaxAmount(), purchase.getTotalAmount(), purchase.getAmountPaid(), cashPayment));

        return purchase;
    }
}
