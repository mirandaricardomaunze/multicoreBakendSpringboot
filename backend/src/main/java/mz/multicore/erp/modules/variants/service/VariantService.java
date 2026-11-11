package mz.multicore.erp.modules.variants.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.variants.dto.*;
import mz.multicore.erp.modules.variants.model.ProductFamily;
import mz.multicore.erp.modules.variants.model.ProductVariant;
import mz.multicore.erp.modules.variants.repository.ProductFamilyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Product variants (SA-C04, {@code docs/VARIANTES_ARTIGO_SPEC.md}). A family (shirt by size and colour, rebar by
 * thickness, cake by flavour) creates one ordinary product per combination, copying price, cost, category, VAT and
 * stock rules from a template product. Sales, stock and invoices keep working on products, unchanged.
 */
@Service
public class VariantService {

    private static final int MAX_VARIANTS = 200;

    private final ProductFamilyRepository familyRepository;
    private final ProductRepository productRepository;
    private final CompanyLookup companyLookup;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public VariantService(ProductFamilyRepository familyRepository, ProductRepository productRepository,
                          CompanyLookup companyLookup, InventoryService inventoryService, AuditLogService auditLogService) {
        this.familyRepository = familyRepository;
        this.productRepository = productRepository;
        this.companyLookup = companyLookup;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<VariantFamilyDTO> families(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Map<Long, BigDecimal> stock = new HashMap<>();
        for (StockDTO s : inventoryService.findStocksByCompany(companyId)) {
            stock.merge(s.productId(), s.quantity() == null ? BigDecimal.ZERO : s.quantity(), BigDecimal::add);
        }
        return familyRepository.findByCompany(companyId).stream().map(f -> toDto(f, stock)).toList();
    }

    @Transactional
    public VariantFamilyDTO create(CreateVariantFamilyRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("criar variantes de artigo");
        String prefix = request.skuPrefix().trim().toUpperCase(Locale.ROOT);
        if (familyRepository.existsByCompanyIdAndSkuPrefixIgnoreCase(request.companyId(), prefix)) {
            throw new BusinessRuleException("Já existe uma família com o prefixo " + prefix + ".");
        }
        Product template = productRepository.findByIdAndCompaniesId(request.templateProductId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Artigo modelo não encontrado nesta empresa.")).requireOrdinarySale();
        List<String> values1 = VariantRules.clean(request.values1());
        List<String> values2 = VariantRules.clean(request.values2());
        boolean twoAttributes = request.attribute2() != null && !request.attribute2().isBlank();
        if (values1.isEmpty()) throw new BusinessRuleException("Indique pelo menos um valor de " + request.attribute1().trim() + ".");
        if (twoAttributes && values2.isEmpty()) throw new BusinessRuleException("Indique os valores de " + request.attribute2().trim() + ".");
        if (!twoAttributes) values2 = List.of();
        Company company = companyLookup.require(request.companyId());

        ProductFamily family = new ProductFamily();
        family.setCompany(company);
        family.setName(request.name().trim());
        family.setSkuPrefix(prefix);
        family.setAttribute1(request.attribute1().trim());
        family.setAttribute2(twoAttributes ? request.attribute2().trim() : null);
        family.setTemplateProduct(template);
        int created = addCombinations(family, VariantRules.combinations(values1, values2));
        family = familyRepository.save(family);
        auditLogService.logCurrent("VARIANT_FAMILY_CREATE", "Família " + family.getName() + " criada com " + created + " variantes.");
        return toDto(family, Map.of());
    }

    @Transactional
    public VariantFamilyDTO addValues(Long familyId, AddVariantValuesRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("acrescentar variantes");
        ProductFamily family = requireFamily(request.companyId(), familyId);
        Set<String> known1 = new LinkedHashSet<>();
        Set<String> known2 = new LinkedHashSet<>();
        family.getVariants().forEach(v -> {
            known1.add(v.getValue1());
            if (v.getValue2() != null) known2.add(v.getValue2());
        });
        List<String> values1 = new ArrayList<>(known1);
        values1.addAll(VariantRules.clean(request.values1()));
        List<String> values2 = new ArrayList<>(known2);
        if (family.getAttribute2() != null) values2.addAll(VariantRules.clean(request.values2()));
        int created = addCombinations(family, VariantRules.combinations(VariantRules.clean(values1),
                family.getAttribute2() == null ? List.of() : VariantRules.clean(values2)));
        if (created == 0) throw new BusinessRuleException("Não há combinações novas para criar.");
        family = familyRepository.save(family);
        auditLogService.logCurrent("VARIANT_FAMILY_ADD", created + " variantes novas em " + family.getName() + ".");
        return toDto(family, Map.of());
    }

    @Transactional
    public VariantFamilyDTO setPrice(Long familyId, SetFamilyPriceRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("alterar preço das variantes");
        ProductFamily family = requireFamily(request.companyId(), familyId);
        for (ProductVariant v : family.getVariants()) {
            v.getProduct().setUnitPrice(request.unitPrice());
            productRepository.save(v.getProduct());
        }
        auditLogService.logCurrent("VARIANT_FAMILY_PRICE", "Preço de " + family.getName() + " passou a "
                + DisplayNumbers.money(request.unitPrice()) + " MT em " + family.getVariants().size() + " variantes.");
        return toDto(family, Map.of());
    }

    /** Creates the combinations not yet in the family; returns how many were created. */
    private int addCombinations(ProductFamily family, List<VariantRules.Combination> combinations) {
        Set<String> existing = new HashSet<>();
        family.getVariants().forEach(v -> existing.add(key(v.getValue1(), v.getValue2())));
        int created = 0;
        for (VariantRules.Combination c : combinations) {
            if (!existing.add(key(c.value1(), c.value2()))) continue;
            if (family.getVariants().size() >= MAX_VARIANTS) {
                throw new BusinessRuleException("Uma família tem no máximo " + MAX_VARIANTS + " variantes.");
            }
            String sku = VariantRules.sku(family.getSkuPrefix(), c);
            if (productRepository.findBySku(sku).isPresent()) {
                throw new BusinessRuleException("O código " + sku + " já existe noutro artigo: escolha outro prefixo.");
            }
            ProductVariant variant = new ProductVariant();
            variant.setFamily(family);
            variant.setProduct(productRepository.save(copyOf(family.getTemplateProduct(), family.getCompany(), sku,
                    VariantRules.name(family.getName(), c))));
            variant.setValue1(c.value1());
            variant.setValue2(c.value2());
            family.getVariants().add(variant);
            created++;
        }
        return created;
    }

    private static Product copyOf(Product template, Company company, String sku, String name) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setUnitPrice(template.getUnitPrice());
        p.setPurchasePrice(template.getPurchasePrice());
        p.setMinStock(template.getMinStock());
        p.setWholesalePrice(template.getWholesalePrice());
        p.setWholesaleMinQty(template.getWholesaleMinQty());
        p.setSaleType(template.getSaleType());
        p.setStockTracked(template.isStockTracked());
        p.setTracksExpiry(template.isTracksExpiry());
        p.setCategory(template.getCategory());
        p.setBusinessUnit(template.getBusinessUnit());
        p.setTaxRate(template.getTaxRate());
        p.setBaseUnitCode(template.getBaseUnitCode());
        p.setDescription(template.getDescription());
        p.getCompanies().add(company);
        return p;
    }

    private ProductFamily requireFamily(Long companyId, Long familyId) {
        return familyRepository.findByIdAndCompanyId(familyId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Família de variantes não encontrada."));
    }

    private static String key(String a, String b) {
        return a.toLowerCase(Locale.ROOT) + "|" + (b == null ? "" : b.toLowerCase(Locale.ROOT));
    }

    static VariantFamilyDTO toDto(ProductFamily f, Map<Long, BigDecimal> stock) {
        List<VariantDTO> variants = f.getVariants().stream().map(v -> new VariantDTO(v.getId(), v.getProduct().getId(),
                v.getProduct().getSku(), v.getProduct().getName(), v.getValue1(), v.getValue2(), v.getProduct().getUnitPrice(),
                stock.getOrDefault(v.getProduct().getId(), BigDecimal.ZERO))).toList();
        BigDecimal total = variants.stream().map(VariantDTO::stock).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new VariantFamilyDTO(f.getId(), f.getName(), f.getSkuPrefix(), f.getAttribute1(), f.getAttribute2(),
                f.getTemplateProduct().getId(), f.getTemplateProduct().getName(), f.isActive(), total, variants);
    }
}
