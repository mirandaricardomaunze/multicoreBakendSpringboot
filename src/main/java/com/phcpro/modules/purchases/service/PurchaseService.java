package com.phcpro.modules.purchases.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.validation.TaxIdValidator;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.purchases.dto.CreatePurchaseLineRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseRequest;
import com.phcpro.modules.purchases.dto.CreateSupplierRequest;
import com.phcpro.modules.purchases.dto.PurchaseDTO;
import com.phcpro.modules.purchases.dto.PurchaseLineDTO;
import com.phcpro.modules.purchases.dto.SupplierDTO;
import com.phcpro.modules.purchases.model.Purchase;
import com.phcpro.modules.purchases.model.PurchaseLine;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.repository.PurchaseRepository;
import com.phcpro.modules.purchases.repository.SupplierRepository;
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

    public PurchaseService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            InventoryService inventoryService,
            FinanceService financeService
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
    }

    @Transactional
    public Supplier createSupplier(String name, String taxId, String email, String address, Long companyId) {
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
        return supplierRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByCompany(Long companyId) {
        return purchaseRepository.findByCompanyId(companyId);
    }

    @Transactional
    public Purchase createPurchase(CreatePurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));

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
            Product product = productRepository.findById(lineReq.productId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            PurchaseLine line = new PurchaseLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(lineReq.unitPrice());
            
            // Default VAT (IVA) rate: 17% (standard in Mozambique) or 23% depending on tax ID
            BigDecimal taxRate = supplier.getTaxId().startsWith("5") ? new BigDecimal("0.23") : new BigDecimal("0.17");
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
        
        long count = purchaseRepository.count() + 1;
        purchase.setPurchaseNumber("V/FT-2026/" + count);

        purchase = purchaseRepository.save(purchase);

        // Record finance transaction (CREDIT = cash out)
        String description = "Pagamento Compra " + purchase.getPurchaseNumber() + " - Fornecedor " + supplier.getName();
        financeService.registerTransaction(request.financeAccountId(), "CREDIT", total, description);

        return purchase;
    }

    @Transactional
    public SupplierDTO createSupplier(CreateSupplierRequest request) {
        Supplier supplier = createSupplier(request.name(), request.taxId(), request.email(), request.address(), request.companyId());
        return toDTO(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> findSuppliersByCompany(Long companyId) {
        return supplierRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseDTO> findPurchasesByCompany(Long companyId) {
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
                p.getStatus(),
                p.getPurchaseDate(),
                lines
        );
    }
}
