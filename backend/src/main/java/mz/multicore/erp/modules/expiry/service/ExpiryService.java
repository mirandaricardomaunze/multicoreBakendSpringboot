package mz.multicore.erp.modules.expiry.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.expiry.dto.*;
import mz.multicore.erp.modules.expiry.model.MarkdownRule;
import mz.multicore.erp.modules.expiry.repository.MarkdownRuleRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.service.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Expiry dates and markdowns (SA-C14, SA-C15, {@code docs/VALIDADES_REMARCACAO_SPEC.md}). Sales and production
 * already leave stock by FEFO; this service shows what is about to expire, marks it down through an ordinary
 * store promotion (the POS applies it by itself) and writes off what expired through the stock waste flow, with
 * its approval threshold. Nothing here moves stock or prices directly.
 */
@Service
public class ExpiryService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final InventoryService inventoryService;
    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final MarkdownRuleRepository ruleRepository;
    private final StockWasteRepository wasteRepository;
    private final StockWasteService wasteService;
    private final PromotionService promotionService;
    private final AuditLogService auditLogService;

    public ExpiryService(InventoryService inventoryService, ProductBatchRepository batchRepository, ProductRepository productRepository,
                         ProductCategoryRepository categoryRepository, CompanyRepository companyRepository,
                         MarkdownRuleRepository ruleRepository, StockWasteRepository wasteRepository, StockWasteService wasteService,
                         PromotionService promotionService, AuditLogService auditLogService) {
        this.inventoryService = inventoryService;
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.companyRepository = companyRepository;
        this.ruleRepository = ruleRepository;
        this.wasteRepository = wasteRepository;
        this.wasteService = wasteService;
        this.promotionService = promotionService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public ExpiryBoardDTO board(Long companyId, int horizonDays) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate today = LocalDate.now();
        List<ExpiryRules.Rule> rules = activeRules(companyId);
        List<ProductBatchDTO> batches = inventoryService.findExpiringBatchesByCompany(companyId,
                today.plusDays(Math.max(0, Math.min(horizonDays, 90))));
        Map<Long, Product> products = productRepository.findAllById(batches.stream().map(ProductBatchDTO::productId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        List<ExpiryLotDTO> lots = new ArrayList<>();
        BigDecimal expired = BigDecimal.ZERO;
        BigDecimal atRisk = BigDecimal.ZERO;
        long markedDown = 0;
        for (ProductBatchDTO b : batches) {
            Product product = products.get(b.productId());
            if (product == null || b.expirationDate() == null) continue;
            ExpiryLotDTO lot = toLot(companyId, b, product, today, rules);
            lots.add(lot);
            if (lot.daysLeft() < 0) expired = expired.add(lot.costValue());
            else if (lot.daysLeft() <= 7) atRisk = atRisk.add(lot.costValue());
            if (lot.markdownPct() != null && lot.markdownPct().signum() > 0) markedDown++;
        }
        return new ExpiryBoardDTO(lots, expired, atRisk, markedDown, lossesThisMonth(companyId, today));
    }

    @Transactional(readOnly = true)
    public List<MarkdownRuleDTO> rules(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return ruleRepository.findByCompany(companyId).stream().map(ExpiryService::toDto).toList();
    }

    @Transactional
    public MarkdownRuleDTO saveRule(SaveMarkdownRuleRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("definir regras de remarcação");
        MarkdownRule rule = request.id() == null ? new MarkdownRule()
                : ruleRepository.findByIdAndCompanyId(request.id(), request.companyId())
                        .orElseThrow(() -> new BusinessRuleException("Regra de remarcação não encontrada."));
        if (rule.getCompany() == null) {
            rule.setCompany(companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada.")));
        }
        rule.setCategory(request.categoryId() == null ? null
                : categoryRepository.findByIdAndCompaniesId(request.categoryId(), request.companyId())
                        .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada nesta empresa.")));
        rule.setDaysBefore(request.daysBefore());
        rule.setPercent(request.percent());
        rule.setActive(request.active());
        rule = ruleRepository.save(rule);
        auditLogService.logCurrent("MARKDOWN_RULE_SAVE", "Regra de remarcação: " + rule.getDaysBefore() + " dias antes, "
                + DisplayNumbers.qty(rule.getPercent()) + "%" + (rule.getCategory() == null ? "" : " em " + rule.getCategory().getName()) + ".");
        return toDto(rule);
    }

    /** Marks one lot down now; the discount lasts until the lot expires. */
    @Transactional
    public ExpiryLotDTO markdown(MarkdownRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("remarcar artigo");
        ProductBatch batch = requireBatch(request.companyId(), request.batchId());
        LocalDate today = LocalDate.now();
        if (batch.getExpirationDate() == null || batch.getExpirationDate().isBefore(today)) {
            throw new BusinessRuleException("O lote " + batch.getBatchNumber() + " já passou da validade: não se vende, abate-se.");
        }
        BigDecimal current = currentMarkdown(request.companyId(), batch.getProduct());
        if (current != null && current.compareTo(request.percent()) >= 0) {
            throw new BusinessRuleException("'" + batch.getProduct().getName() + "' já tem " + DisplayNumbers.qty(current) + "% de desconto hoje.");
        }
        createPromotion(request.companyId(), batch, request.percent());
        return lotOf(request.companyId(), batch);
    }

    /** Applies the rules to every lot whose window has started and that does not already have that discount. */
    @Transactional
    public ApplyMarkdownsResultDTO applyRules(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireManagerOrAdmin("aplicar remarcações");
        LocalDate today = LocalDate.now();
        List<ExpiryRules.Rule> rules = activeRules(companyId);
        if (rules.isEmpty()) return new ApplyMarkdownsResultDTO(0, 0, List.of());
        int maxWindow = rules.stream().mapToInt(ExpiryRules.Rule::daysBefore).max().orElse(0);
        int created = 0;
        int already = 0;
        List<String> names = new ArrayList<>();
        Set<Long> handled = new HashSet<>();
        for (ProductBatchDTO b : inventoryService.findExpiringBatchesByCompany(companyId, today.plusDays(maxWindow))) {
            if (b.expirationDate() == null || b.expirationDate().isBefore(today) || !handled.add(b.productId())) continue;
            ProductBatch batch = requireBatch(companyId, b.id());
            Long categoryId = batch.getProduct().getCategory() == null ? null : batch.getProduct().getCategory().getId();
            BigDecimal suggested = ExpiryRules.suggested(rules, categoryId, ExpiryRules.daysLeft(today, batch.getExpirationDate()));
            if (suggested == null) continue;
            BigDecimal current = currentMarkdown(companyId, batch.getProduct());
            if (current != null && current.compareTo(suggested) >= 0) {
                already++;
                continue;
            }
            createPromotion(companyId, batch, suggested);
            created++;
            names.add(batch.getProduct().getName() + " (" + DisplayNumbers.qty(suggested) + "%)");
        }
        if (created > 0) auditLogService.logCurrent("MARKDOWN_RULES_APPLY", created + " remarcações criadas: " + String.join(", ", names));
        return new ApplyMarkdownsResultDTO(created, already, names);
    }

    /** Writes off an expired lot through the stock waste flow (approval above the usual threshold). */
    @Transactional
    public StockWasteDTO writeOff(WriteOffExpiredRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        ProductBatch batch = requireBatch(request.companyId(), request.batchId());
        if (batch.getExpirationDate() == null || batch.getExpirationDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("O lote " + batch.getBatchNumber() + " ainda está dentro da validade: remarque-o em vez de abater.");
        }
        if (batch.getQuantity() == null || batch.getQuantity().signum() <= 0) {
            throw new BusinessRuleException("O lote " + batch.getBatchNumber() + " já não tem stock.");
        }
        String notes = "Validade " + batch.getExpirationDate() + (request.notes() == null || request.notes().isBlank() ? "" : " - " + request.notes().trim());
        return wasteService.registerWaste(new CreateStockWasteRequest(request.companyId(), batch.getWarehouse().getId(),
                batch.getProduct().getId(), batch.getId(), batch.getQuantity(), WasteReason.EXPIRED, notes, request.requestKey()));
    }

    private void createPromotion(Long companyId, ProductBatch batch, BigDecimal percent) {
        String name = "Remarcação validade: " + batch.getProduct().getName() + " (lote " + batch.getBatchNumber() + ")";
        promotionService.createPromotion(new CreatePromotionRequest(companyId, name.length() > 120 ? name.substring(0, 120) : name,
                "PERCENT", batch.getProduct().getId(), null, percent, null, null, LocalDate.now(), batch.getExpirationDate()));
        auditLogService.logCurrent("EXPIRY_MARKDOWN", batch.getProduct().getName() + " remarcado " + DisplayNumbers.qty(percent)
                + "% até " + batch.getExpirationDate() + " (lote " + batch.getBatchNumber() + ").");
    }

    private BigDecimal currentMarkdown(Long companyId, Product product) {
        Long categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        return promotionService.bestPromotion(companyId, product.getId(), categoryId, ONE)
                .map(PromotionService.AppliedPromotion::discountPercent).orElse(null);
    }

    private ProductBatch requireBatch(Long companyId, Long batchId) {
        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessRuleException("Lote não encontrado."));
        if (batch.getWarehouse() == null || batch.getWarehouse().getCompany() == null
                || !companyId.equals(batch.getWarehouse().getCompany().getId())) {
            throw new BusinessRuleException("Lote não encontrado nesta empresa.");
        }
        return batch;
    }

    private ExpiryLotDTO lotOf(Long companyId, ProductBatch batch) {
        ProductBatchDTO dto = new ProductBatchDTO(batch.getId(), batch.getProduct().getId(), batch.getProduct().getSku(),
                batch.getProduct().getName(), batch.getWarehouse().getId(), batch.getWarehouse().getName(), batch.getBatchNumber(),
                batch.getExpirationDate(), null, batch.getQuantity());
        return toLot(companyId, dto, batch.getProduct(), LocalDate.now(), activeRules(companyId));
    }

    private ExpiryLotDTO toLot(Long companyId, ProductBatchDTO b, Product product, LocalDate today, List<ExpiryRules.Rule> rules) {
        long daysLeft = ExpiryRules.daysLeft(today, b.expirationDate());
        BigDecimal qty = b.quantity() == null ? BigDecimal.ZERO : b.quantity();
        BigDecimal cost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        BigDecimal price = product.getUnitPrice() == null ? BigDecimal.ZERO : product.getUnitPrice();
        Long categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        return new ExpiryLotDTO(b.id(), product.getId(), product.getName(), product.getSku(),
                product.getCategory() == null ? null : product.getCategory().getName(), b.warehouseId(), b.warehouseName(),
                b.batchNumber(), b.expirationDate(), daysLeft, qty, cost, cost.multiply(qty).setScale(2, RoundingMode.HALF_UP),
                price, price.multiply(qty).setScale(2, RoundingMode.HALF_UP), ExpiryRules.stage(daysLeft),
                daysLeft < 0 ? null : currentMarkdown(companyId, product), ExpiryRules.suggested(rules, categoryId, daysLeft));
    }

    private List<ExpiryRules.Rule> activeRules(Long companyId) {
        return ruleRepository.findByCompany(companyId).stream().filter(MarkdownRule::isActive)
                .map(r -> new ExpiryRules.Rule(r.getCategory() == null ? null : r.getCategory().getId(), r.getDaysBefore(), r.getPercent()))
                .toList();
    }

    private BigDecimal lossesThisMonth(Long companyId, LocalDate today) {
        return wasteRepository.findByCompanyIdAndPeriod(companyId, today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream().filter(w -> w.getReason() == WasteReason.EXPIRED && w.getStatus() == WasteStatus.APPROVED)
                .map(w -> w.getTotalCost() == null ? BigDecimal.ZERO : w.getTotalCost()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static MarkdownRuleDTO toDto(MarkdownRule r) {
        return new MarkdownRuleDTO(r.getId(), r.getCategory() == null ? null : r.getCategory().getId(),
                r.getCategory() == null ? null : r.getCategory().getName(), r.getDaysBefore(), r.getPercent(), r.isActive());
    }
}
