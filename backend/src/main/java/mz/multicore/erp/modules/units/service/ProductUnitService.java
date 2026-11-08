package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.units.dto.*;
import mz.multicore.erp.modules.units.model.ProductUnit;
import mz.multicore.erp.modules.units.model.UnitDimension;
import mz.multicore.erp.modules.units.model.UnitOfMeasure;
import mz.multicore.erp.modules.units.repository.ProductUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base unit and alternative units of each product (SA-C02, {@code docs/UNIDADES_MEDIDA_SPEC.md}).
 *
 * <p>Invariants: stock, prices and documents remain in the base unit; the base unit only changes while the
 * product is unused; the box and package composition of the product record remains the source of truth and
 * only appears here as derived rows.
 */
@Service
public class ProductUnitService {

    static final String DEFAULT_COUNT_UNIT = "UN";
    static final String DEFAULT_WEIGHT_UNIT = "KG";
    private static final Set<String> PACKAGING_CODES = Set.of("CX", "EMB");

    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;
    private final UnitOfMeasureService unitService;
    private final AuditLogService auditLogService;

    public ProductUnitService(ProductUnitRepository productUnitRepository, ProductRepository productRepository,
                              UnitOfMeasureService unitService, AuditLogService auditLogService) {
        this.productUnitRepository = productUnitRepository;
        this.productRepository = productRepository;
        this.unitService = unitService;
        this.auditLogService = auditLogService;
    }

    /** Effective base unit: the explicit one, otherwise KG for weighed products and UN for the rest. */
    public static String baseUnitCodeOf(Product product) {
        if (product.getBaseUnitCode() != null && !product.getBaseUnitCode().isBlank()) {
            return product.getBaseUnitCode();
        }
        return product.getSaleType() == ProductSaleType.WEIGHT ? DEFAULT_WEIGHT_UNIT : DEFAULT_COUNT_UNIT;
    }

    @Transactional(readOnly = true)
    public ProductUnitsDTO getProductUnits(Long companyId, Long productId) {
        Product product = requireProduct(companyId, productId);
        UnitOfMeasure base = unitService.requireVisible(baseUnitCodeOf(product), companyId);
        String lockReason = baseUnitLockReason(product);

        List<ProductUnitDTO> units = new ArrayList<>(derivedPackagingRows(product, base));
        productUnitRepository.findByProductIdOrderByFactorToBase(product.getId())
                .forEach(pu -> units.add(toDto(pu)));
        return new ProductUnitsDTO(product.getId(), product.getName(), UnitOfMeasureService.toDto(base),
                lockReason != null, lockReason, units);
    }

    @Transactional
    public ProductUnitsDTO setBaseUnit(SetBaseUnitRequest request) {
        Product product = requireProduct(request.companyId(), request.productId());
        UnitOfMeasure unit = unitService.requireVisible(request.unitCode(), request.companyId());
        String current = baseUnitCodeOf(product);
        if (current.equals(unit.getCode())) return getProductUnits(request.companyId(), product.getId());

        String lockReason = baseUnitLockReason(product);
        if (lockReason != null) throw new BusinessRuleException(lockReason);
        if (unit.getDimension() == UnitDimension.PACKAGE) {
            throw new BusinessRuleException("A unidade base tem de ser uma unidade física (un, kg, L, m, m², m³). "
                    + "Embalagens como " + unit.getName() + " entram como unidade alternativa.");
        }
        if (product.getSaleType() == ProductSaleType.WEIGHT && unit.getDimension() != UnitDimension.MASS) {
            throw new BusinessRuleException("Um produto vendido ao peso tem de ter a unidade base em massa (kg, g ou t).");
        }
        product.setBaseUnitCode(unit.getCode());
        productRepository.save(product);
        auditLogService.logCurrent("PRODUCT_BASE_UNIT_SET",
                "Produto " + product.getSku() + ": unidade base " + current + " -> " + unit.getCode());
        return getProductUnits(request.companyId(), product.getId());
    }

    @Transactional
    public ProductUnitsDTO saveUnit(SaveProductUnitRequest request) {
        Product product = requireProduct(request.companyId(), request.productId());
        UnitOfMeasure base = unitService.requireVisible(baseUnitCodeOf(product), request.companyId());
        UnitOfMeasure unit = unitService.requireVisible(request.unitCode(), request.companyId());
        if (unit.getId().equals(base.getId())) {
            throw new BusinessRuleException(unit.getName() + " já é a unidade base deste produto.");
        }
        if (PACKAGING_CODES.contains(unit.getCode()) && hasPackagingComposition(product)) {
            throw new BusinessRuleException("A " + unit.getName().toLowerCase() + " deste produto já está definida no cadastro ("
                    + product.getUnitsPerBox() + " por caixa). Altere-a no cadastro do produto.");
        }
        if (!request.sellable() && !request.purchasable()) {
            throw new BusinessRuleException("Marque se a unidade serve para vender, para comprar, ou ambos.");
        }

        BigDecimal factor = resolveFactor(request.factorToBase(), unit, base);
        String barcode = normalizeBarcode(request.barcode());

        ProductUnit row = productUnitRepository.findByProductIdAndUnitId(product.getId(), unit.getId())
                .orElseGet(ProductUnit::new);
        boolean created = row.getId() == null;
        if (barcode != null) requireFreeBarcode(barcode, row.getId());

        row.setProduct(product);
        row.setUnit(unit);
        row.setFactorToBase(factor);
        row.setBarcode(barcode);
        row.setSellable(request.sellable());
        row.setPurchasable(request.purchasable());
        row.setDefaultSale(request.defaultSale() && request.sellable());
        row.setDefaultPurchase(request.defaultPurchase() && request.purchasable());
        productUnitRepository.save(row);
        if (row.isDefaultSale() || row.isDefaultPurchase()) clearOtherDefaults(product.getId(), row);

        auditLogService.logCurrent(created ? "PRODUCT_UNIT_CREATE" : "PRODUCT_UNIT_UPDATE",
                "Produto " + product.getSku() + ": 1 " + unit.getCode() + " = "
                        + DisplayNumbers.qty(factor) + " " + base.getCode());
        return getProductUnits(request.companyId(), product.getId());
    }

    @Transactional
    public ProductUnitsDTO deleteUnit(Long companyId, Long productUnitId) {
        ProductUnit row = productUnitRepository.findById(productUnitId)
                .orElseThrow(() -> new BusinessRuleException("Unidade do produto não encontrada."));
        Product product = requireProduct(companyId, row.getProduct().getId());
        productUnitRepository.delete(row);
        auditLogService.logCurrent("PRODUCT_UNIT_DELETE",
                "Produto " + product.getSku() + ": unidade " + row.getUnit().getCode() + " removida");
        return getProductUnits(companyId, product.getId());
    }

    @Transactional(readOnly = true)
    public UnitConversionDTO convert(Long companyId, Long productId, String unitCode, BigDecimal quantity) {
        Product product = requireProduct(companyId, productId);
        UnitOfMeasure base = unitService.requireVisible(baseUnitCodeOf(product), companyId);
        UnitOfMeasure unit = unitService.requireVisible(unitCode, companyId);
        BigDecimal factor = factorFor(product, unit, base);
        BigDecimal baseQuantity = UnitConversionRules.toBase(quantity, factor, base.getDecimals(), base.getName());
        return new UnitConversionDTO(product.getId(), unit.getCode(), quantity, factor, base.getCode(), baseQuantity);
    }

    /**
     * Unrounded factor from {@code unitCode} to the product base unit, plus the base decimals, for callers that
     * convert measured values (bulk volumes) and must round instead of rejecting fractions.
     */
    @Transactional(readOnly = true)
    public UnitConversionDTO conversionFactor(Long companyId, Long productId, String unitCode) {
        Product product = requireProduct(companyId, productId);
        UnitOfMeasure base = unitService.requireVisible(baseUnitCodeOf(product), companyId);
        UnitOfMeasure unit = unitService.requireVisible(unitCode, companyId);
        BigDecimal factor = factorFor(product, unit, base);
        return new UnitConversionDTO(product.getId(), unit.getCode(), BigDecimal.valueOf(base.getDecimals()), factor,
                base.getCode(), factor);
    }

    private BigDecimal factorFor(Product product, UnitOfMeasure unit, UnitOfMeasure base) {
        if (unit.getId().equals(base.getId())) return BigDecimal.ONE;
        var explicit = productUnitRepository.findByProductIdAndUnitId(product.getId(), unit.getId());
        if (explicit.isPresent()) return explicit.get().getFactorToBase();
        for (ProductUnitDTO derived : derivedPackagingRows(product, base)) {
            if (derived.unitCode().equals(unit.getCode())) return derived.factorToBase();
        }
        BigDecimal fixed = UnitConversionRules.fixedFactor(unit.getDimension(), unit.getDimensionFactor(),
                base.getDimension(), base.getDimensionFactor());
        if (fixed != null) return fixed;
        throw new BusinessRuleException("O produto " + product.getName() + " não tem a unidade " + unit.getName()
                + ". Defina-a em Unidades do produto.");
    }

    private static BigDecimal resolveFactor(BigDecimal requested, UnitOfMeasure unit, UnitOfMeasure base) {
        BigDecimal fixed = UnitConversionRules.fixedFactor(unit.getDimension(), unit.getDimensionFactor(),
                base.getDimension(), base.getDimensionFactor());
        if (fixed != null) {
            if (requested != null && requested.compareTo(fixed) != 0) {
                throw new BusinessRuleException("1 " + unit.getName() + " é sempre " + DisplayNumbers.qty(fixed) + " "
                        + base.getName() + "; o factor não se escolhe.");
            }
            if (base.getDecimals() == 0 && fixed.scale() > 0) {
                throw new BusinessRuleException(unit.getName() + " não pode ser unidade de um produto com base em "
                        + base.getName() + " (daria fracções da base).");
            }
            return fixed;
        }
        return UnitConversionRules.requireValidFactor(requested, base.getDecimals(), unit.getName(), base.getName());
    }

    /** Explains why the base unit is locked, or returns {@code null} when it can still change. */
    String baseUnitLockReason(Product product) {
        Long id = product.getId();
        if (productUnitRepository.existsByProductId(id)) {
            return "O produto já tem unidades alternativas calculadas sobre a base actual. Remova-as primeiro.";
        }
        if (productUnitRepository.hasStockMovements(id) || productUnitRepository.hasInvoiceLines(id)
                || productUnitRepository.hasOrderLines(id) || productUnitRepository.hasQuotationLines(id)
                || productUnitRepository.hasPurchaseLines(id)) {
            return "O produto já tem stock ou documentos na unidade actual; mudar a base alteraria o seu significado.";
        }
        return null;
    }

    private static boolean hasPackagingComposition(Product product) {
        return product.getUnitsPerBox() > 1;
    }

    private static List<ProductUnitDTO> derivedPackagingRows(Product product, UnitOfMeasure base) {
        if (!hasPackagingComposition(product) || !DEFAULT_COUNT_UNIT.equals(base.getCode())) return List.of();
        List<ProductUnitDTO> rows = new ArrayList<>();
        if (product.getPackagesPerBox() > 1 && product.getUnitsPerPackage() > 1) {
            rows.add(new ProductUnitDTO(null, "EMB", "Embalagem", "emb", UnitDimension.PACKAGE.name(),
                    BigDecimal.valueOf(product.getUnitsPerPackage()), null, true, true, false, false, true));
        }
        rows.add(new ProductUnitDTO(null, "CX", "Caixa", "cx", UnitDimension.PACKAGE.name(),
                BigDecimal.valueOf(product.getUnitsPerBox()), null, true, true, false, false, true));
        return rows;
    }

    private void clearOtherDefaults(Long productId, ProductUnit kept) {
        for (ProductUnit other : productUnitRepository.findByProductIdOrderByFactorToBase(productId)) {
            if (other.getId().equals(kept.getId())) continue;
            boolean changed = false;
            if (kept.isDefaultSale() && other.isDefaultSale()) { other.setDefaultSale(false); changed = true; }
            if (kept.isDefaultPurchase() && other.isDefaultPurchase()) { other.setDefaultPurchase(false); changed = true; }
            if (changed) productUnitRepository.save(other);
        }
    }

    private void requireFreeBarcode(String barcode, Long currentRowId) {
        productUnitRepository.findByBarcode(barcode)
                .filter(other -> !other.getId().equals(currentRowId))
                .ifPresent(other -> {
                    throw new BusinessRuleException("O código de barras " + barcode + " já pertence a "
                            + other.getProduct().getName() + " (" + other.getUnit().getName() + ").");
                });
        productRepository.findByBarcode(barcode).ifPresent(p -> {
            throw new BusinessRuleException("O código de barras " + barcode + " já é o código do produto " + p.getName() + ".");
        });
    }

    private static String normalizeBarcode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String barcode = raw.trim();
        if (barcode.length() > 64) throw new BusinessRuleException("O código de barras tem no máximo 64 caracteres.");
        return barcode;
    }

    private Product requireProduct(Long companyId, Long productId) {
        CurrentUserContext.requireCompany(companyId);
        if (productId == null) throw new BusinessRuleException("Produto é obrigatório.");
        return productRepository.findByIdAndCompaniesId(productId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado nesta empresa."));
    }

    private static ProductUnitDTO toDto(ProductUnit pu) {
        UnitOfMeasure u = pu.getUnit();
        return new ProductUnitDTO(pu.getId(), u.getCode(), u.getName(), u.getSymbol(), u.getDimension().name(),
                pu.getFactorToBase(), pu.getBarcode(), pu.isSellable(), pu.isPurchasable(),
                pu.isDefaultSale(), pu.isDefaultPurchase(), false);
    }
}
