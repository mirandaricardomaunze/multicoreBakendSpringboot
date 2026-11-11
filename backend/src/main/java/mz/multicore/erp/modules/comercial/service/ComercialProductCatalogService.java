package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.validation.ImagePayloadValidator;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateProductRequest;
import mz.multicore.erp.modules.comercial.dto.POSCatalogItemDTO;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ComercialProductCatalogService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CompanyLookup companyLookup;
    private final TaxRateRepository taxRateRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public ComercialProductCatalogService(
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository,
            CompanyLookup companyLookup,
            TaxRateRepository taxRateRepository,
            @Autowired(required = false) BusinessUnitRepository businessUnitRepository,
            @Lazy InventoryService inventoryService,
            AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.companyLookup = companyLookup;
        this.taxRateRepository = taxRateRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findDistinctByCompaniesIdOrderByName(CurrentUserContext.getCurrentCompanyId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ProductDTO> getSellableProducts() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Set<Long> inStock = inventoryService.getInStockProductIdsForSale(companyId);
        return productRepository.findDistinctByCompaniesIdOrderByName(companyId)
                .stream().map(this::toDTO)
                .filter(p -> !p.stockTracked() || inStock.contains(p.id()))
                .collect(Collectors.toList());
    }

    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(
            String query, boolean availableOnly, Long warehouseId, Integer page, Integer size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Set<Long> sellableIds = warehouseId != null
                ? inventoryService.getInStockProductIdsForSale(companyId, warehouseId)
                : inventoryService.getInStockProductIdsForSale(companyId);
        Map<Long, BigDecimal> stockQuantities = inventoryService != null
                ? inventoryService.getStockQuantitiesForSale(companyId, warehouseId)
                : Collections.emptyMap();
        Set<Long> queryIds = sellableIds.isEmpty() ? Set.of(-1L) : sellableIds;
        String normalizedQuery = query == null ? "" : query.trim();
        var result = productRepository.findPOSCatalogPage(companyId, normalizedQuery, availableOnly,
                queryIds, PageQuery.of(page, size));
        return PageResponseMapper.from(result, product -> {
            boolean sellable = !product.isStockTracked() || sellableIds.contains(product.getId());
            BigDecimal qty = stockQuantities != null ? stockQuantities.getOrDefault(product.getId(), BigDecimal.ZERO) : BigDecimal.ZERO;
            return new POSCatalogItemDTO(toDTO(product), sellable, qty);
        });
    }

    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(
            String query, boolean availableOnly, Integer page, Integer size) {
        return getPOSCatalogPage(query, availableOnly, null, page, size);
    }

    public PageResponse<ProductDTO> getProductsPage(
            String query, Long categoryId, Integer page, Integer size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String normalizedQuery = query == null ? "" : query.trim();
        var result = productRepository.findCatalogPage(
                companyId, normalizedQuery, categoryId, PageQuery.of(page, size));
        return PageResponseMapper.from(result, this::toDTO);
    }

    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode, Long warehouseId) {
        ProductDTO product = findProductByBarcode(barcode);
        if (product == null) return null;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Set<Long> sellableIds = warehouseId != null
                ? inventoryService.getInStockProductIdsForSale(companyId, warehouseId)
                : inventoryService.getInStockProductIdsForSale(companyId);
        boolean sellable = !product.stockTracked() || sellableIds.contains(product.id());
        Map<Long, BigDecimal> stockQuantities = inventoryService != null
                ? inventoryService.getStockQuantitiesForSale(companyId, warehouseId)
                : Collections.emptyMap();
        BigDecimal qty = stockQuantities != null ? stockQuantities.getOrDefault(product.id(), BigDecimal.ZERO) : BigDecimal.ZERO;
        return new POSCatalogItemDTO(product, sellable, qty);
    }

    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode) {
        return findPOSCatalogItemByBarcode(barcode, null);
    }

    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     Long taxRateId, String description,
                                     BigDecimal wholesalePrice, BigDecimal wholesaleMinQty) {
        String cleanReference = normalizeOptional(reference);
        String cleanBarcode = normalizeOptional(barcode);
        ProductSaleType parsedSaleType = parseSaleType(saleType);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (productRepository.findBySkuAndCompaniesId(sku, companyId).isPresent()) {
            throw new BusinessRuleException("Já existe um produto com o SKU indicado.");
        }
        Product sharedProduct = productRepository.findBySku(sku).orElse(null);
        if (sharedProduct != null) {
            sharedProduct.getCompanies().add(companyLookup.reference(companyId));
            return toDTO(productRepository.save(sharedProduct));
        }
        if (cleanReference != null && productRepository.findByReferenceAndCompaniesId(cleanReference, companyId).isPresent()) {
            throw new BusinessRuleException("Ja existe um produto com a referencia indicada.");
        }
        if (cleanBarcode != null && productRepository.findByBarcodeAndCompaniesId(cleanBarcode, companyId).isPresent()) {
            throw new BusinessRuleException("Ja existe um produto com o codigo de barras indicado.");
        }
        Product product = new Product();
        product.setSku(sku);
        product.setReference(cleanReference);
        product.setBarcode(cleanBarcode);
        product.setName(name);
        product.setUnitPrice(unitPrice);
        product.setPurchasePrice(purchasePrice);
        product.setMinStock(minStock);
        assignPackaging(product, unitsPerBox, null, null);
        product.setWholesalePrice(wholesalePrice);
        product.setWholesaleMinQty(wholesaleMinQty);
        product.setSaleType(parsedSaleType);
        product.setStockTracked(parsedSaleType != ProductSaleType.SERVICE && stockTracked);
        product.setDescription(description);
        if (categoryId != null) {
            product.setCategory(productCategoryRepository.findByIdAndCompaniesId(categoryId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada.")));
        }
        if (taxRateId != null) {
            product.setTaxRate(taxRateRepository.findByIdAndCompaniesId(taxRateId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Taxa de IVA não encontrada.")));
        }
        product.setCreatedBy("SYSTEM");
        product.getCompanies().add(companyLookup.reference(companyId));
        return toDTO(productRepository.save(product));
    }

    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox,
            Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg,
            Long businessUnitId) {
        ProductDTO created = createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description,
                wholesalePrice, wholesaleMinQty);
        Product product = productRepository.findByIdAndCompaniesId(created.id(), CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        assignPackaging(product, unitsPerBox, packagesPerBox, unitsPerPackage);
        assignLogisticsWeights(product, netUnitWeightKg, grossUnitWeightKg);
        if (businessUnitId != null && businessUnitRepository != null) {
            product.setBusinessUnit(businessUnitRepository.findByIdAndCompanyId(businessUnitId, CurrentUserContext.getCurrentCompanyId())
                    .orElseThrow(() -> new BusinessRuleException("Linha de negócio não encontrada.")));
        }
        return toDTO(productRepository.save(product));
    }

    public ProductDTO updateProduct(Long id, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     Long taxRateId, String description,
                                     BigDecimal wholesalePrice, BigDecimal wholesaleMinQty) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Product product = productRepository.findByIdAndCompaniesId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        String cleanReference = normalizeOptional(reference);
        String cleanBarcode = normalizeOptional(barcode);
        if (cleanReference != null) {
            productRepository.findByReferenceAndCompaniesId(cleanReference, companyId)
                    .filter(p -> !p.getId().equals(id))
                    .ifPresent(p -> { throw new BusinessRuleException("Ja existe outro produto com a referencia indicada."); });
        }
        if (cleanBarcode != null) {
            productRepository.findByBarcodeAndCompaniesId(cleanBarcode, companyId)
                    .filter(p -> !p.getId().equals(id))
                    .ifPresent(p -> { throw new BusinessRuleException("Ja existe outro produto com o codigo de barras indicado."); });
        }
        ProductSaleType parsedSaleType = parseSaleType(saleType);
        product.setReference(cleanReference);
        product.setBarcode(cleanBarcode);
        product.setName(name);
        product.setUnitPrice(unitPrice);
        product.setPurchasePrice(purchasePrice);
        product.setMinStock(minStock);
        assignPackaging(product, unitsPerBox, null, null);
        product.setWholesalePrice(wholesalePrice);
        product.setWholesaleMinQty(wholesaleMinQty);
        product.setSaleType(parsedSaleType);
        product.setStockTracked(parsedSaleType != ProductSaleType.SERVICE && stockTracked);
        product.setDescription(normalizeOptional(description));
        product.setCategory(categoryId != null
                ? productCategoryRepository.findByIdAndCompaniesId(categoryId, companyId)
                        .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada."))
                : null);
        product.setTaxRate(taxRateId != null
                ? taxRateRepository.findByIdAndCompaniesId(taxRateId, companyId)
                        .orElseThrow(() -> new BusinessRuleException("Taxa de IVA não encontrada."))
                : null);
        product = productRepository.save(product);
        auditLogService.logCurrent("PRODUCT_UPDATE",
                "Produto " + product.getSku() + " (" + product.getName() + ") actualizado.");
        return toDTO(product);
    }

    public ProductDTO updateProduct(Long id, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox,
            Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg,
            Long businessUnitId) {
        updateProduct(id, reference, barcode, name, unitPrice, purchasePrice, minStock, unitsPerBox,
                categoryId, saleType, stockTracked, taxRateId, description, wholesalePrice, wholesaleMinQty);
        Product product = productRepository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        assignPackaging(product, unitsPerBox, packagesPerBox, unitsPerPackage);
        assignLogisticsWeights(product, netUnitWeightKg, grossUnitWeightKg);
        if (businessUnitId != null && businessUnitRepository != null) {
            product.setBusinessUnit(businessUnitRepository.findByIdAndCompanyId(businessUnitId, CurrentUserContext.getCurrentCompanyId())
                    .orElseThrow(() -> new BusinessRuleException("Linha de negócio não encontrada.")));
        } else {
            product.setBusinessUnit(null);
        }
        return toDTO(productRepository.save(product));
    }

    private void assignPackaging(Product product, int legacyUnitsPerBox,
                                 Integer packagesPerBox, Integer unitsPerPackage) {
        PackagingComposition composition;
        if (packagesPerBox == null && unitsPerPackage == null) {
            composition = PackagingComposition.legacy(legacyUnitsPerBox);
        } else if (packagesPerBox == null || unitsPerPackage == null) {
            throw new BusinessRuleException("Indique as embalagens por caixa e as unidades por embalagem.");
        } else {
            composition = PackagingComposition.of(packagesPerBox, unitsPerPackage);
        }
        product.setPackagesPerBox(composition.packagesPerBox());
        product.setUnitsPerPackage(composition.unitsPerPackage());
        product.setUnitsPerBox(composition.unitsPerBox());
    }

    private void assignLogisticsWeights(Product product, BigDecimal netKg, BigDecimal grossKg) {
        LogisticsLoadCalculator.validateWeights(netKg, grossKg);
        product.setNetUnitWeightKg(netKg);
        product.setGrossUnitWeightKg(grossKg);
    }

    public ProductDTO findProductByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        return productRepository.findByBarcodeAndCompaniesId(barcode.trim(), CurrentUserContext.getCurrentCompanyId())
                .map(this::toDTO).orElse(null);
    }

    public List<ProductCategoryDTO> getAllCategories() {
        return productCategoryRepository.findDistinctByCompaniesIdOrderByNameAsc(CurrentUserContext.getCurrentCompanyId()).stream()
                .map(c -> new ProductCategoryDTO(c.getId(), c.getCode(), c.getName(), c.getColorHex(), c.isActive()))
                .toList();
    }

    public List<ProductCategoryDTO> getActiveCategories() {
        return productCategoryRepository.findDistinctByCompaniesIdAndActiveTrueOrderByNameAsc(CurrentUserContext.getCurrentCompanyId()).stream()
                .map(c -> new ProductCategoryDTO(c.getId(), c.getCode(), c.getName(), c.getColorHex(), c.isActive()))
                .toList();
    }

    public List<TaxRateDTO> getActiveVatRates() {
        return taxRateRepository.findDistinctByCompaniesIdAndActiveTrueOrderByTypeAscRateDesc(CurrentUserContext.getCurrentCompanyId()).stream()
                .filter(t -> t.getType() != null && t.getType().name().startsWith("IVA"))
                .map(t -> new TaxRateDTO(t.getId(), t.getCode(), t.getName(), t.getType().name(),
                        t.getRate(), t.getLegalBasis(), t.isActive()))
                .toList();
    }

    public ProductDTO toDTO(Product p) {
        boolean hasRate = p.getTaxRate() != null;
        BigDecimal effectiveRate = hasRate ? p.getTaxRate().getRate() : TaxRates.STANDARD_VAT;
        Long taxRateId = hasRate ? p.getTaxRate().getId() : null;
        String taxRateLabel = hasRate ? p.getTaxRate().getName() : "IVA Normal (16%)";
        PackagingComposition packaging = storedPackaging(p);
        return new ProductDTO(
                p.getId(), p.getSku(), p.getReference(), p.getBarcode(), p.getName(),
                p.getUnitPrice(),
                p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO,
                p.getMinStock() != null ? p.getMinStock() : BigDecimal.ZERO,
                p.getWholesalePrice(), p.getWholesaleMinQty(),
                packaging.unitsPerBox(), packaging.packagesPerBox(), packaging.unitsPerPackage(),
                p.getSaleType() != null ? p.getSaleType().name() : ProductSaleType.UNIT.name(),
                p.isStockTracked(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                taxRateId, effectiveRate, taxRateLabel,
                p.getDescription(), p.getImageData(),
                p.getNetUnitWeightKg(), p.getGrossUnitWeightKg(), p.getVersion(),
                p.getBusinessUnit() != null ? p.getBusinessUnit().getId() : null,
                p.getBusinessUnit() != null ? p.getBusinessUnit().getName() : null,
                p.getBusinessUnit() != null ? p.getBusinessUnit().getCode() : null,
                p.isTracksExpiry()
        );
    }

    /** Pedido HTTP completo, incluindo a opção de produto perecível ({@code tracksExpiry}). */
    public ProductDTO createProduct(CreateProductRequest r) {
        ProductDTO created = createProduct(r.sku(), r.reference(), r.barcode(), r.name(), r.unitPrice(),
                r.purchasePrice(), r.minStock(), r.unitsPerBox(), r.packagesPerBox(), r.unitsPerPackage(),
                r.categoryId(), r.saleType(), r.stockTracked(), r.taxRateId(), r.description(), r.wholesalePrice(),
                r.wholesaleMinQty(), r.netUnitWeightKg(), r.grossUnitWeightKg(), r.businessUnitId());
        return applyExpiryTracking(created.id(), r.tracksExpiry());
    }

    public ProductDTO updateProduct(Long id, CreateProductRequest r) {
        updateProduct(id, r.reference(), r.barcode(), r.name(), r.unitPrice(), r.purchasePrice(), r.minStock(),
                r.unitsPerBox(), r.packagesPerBox(), r.unitsPerPackage(), r.categoryId(), r.saleType(),
                r.stockTracked(), r.taxRateId(), r.description(), r.wholesalePrice(), r.wholesaleMinQty(),
                r.netUnitWeightKg(), r.grossUnitWeightKg(), r.businessUnitId());
        return applyExpiryTracking(id, r.tracksExpiry());
    }

    /** Perecível = validade obrigatória nas entradas. {@code null} (cliente anterior) mantém o valor actual. */
    private ProductDTO applyExpiryTracking(Long productId, Boolean tracksExpiry) {
        Product product = productRepository.findByIdAndCompaniesId(productId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        if (tracksExpiry != null && tracksExpiry != product.isTracksExpiry()) {
            product.setTracksExpiry(tracksExpiry);
            product = productRepository.save(product);
        }
        return toDTO(product);
    }

    private PackagingComposition storedPackaging(Product product) {
        int storedTotal = Math.max(1, product.getUnitsPerBox());
        if (product.getPackagesPerBox() > 0 && product.getUnitsPerPackage() > 0) {
            try {
                PackagingComposition composition = PackagingComposition.of(
                        product.getPackagesPerBox(), product.getUnitsPerPackage());
                if (composition.unitsPerBox() == storedTotal) return composition;
            } catch (BusinessRuleException ignored) {}
        }
        return PackagingComposition.legacy(storedTotal);
    }

    public void updateProductImage(Long productId, byte[] imageData) {
        ImagePayloadValidator.validate(imageData, 2 * 1024 * 1024);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Product product = productRepository.findById(productId)
                .filter(p -> p.belongsToCompany(companyId))
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        product.setImageData(imageData);
        productRepository.save(product);
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductSaleType parseSaleType(String value) {
        if (value == null || value.isBlank()) return ProductSaleType.UNIT;
        try {
            return ProductSaleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de venda inválido. Use UNIT, BOX, WEIGHT ou SERVICE.");
        }
    }
}
