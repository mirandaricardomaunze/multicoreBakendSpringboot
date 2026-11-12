package mz.multicore.erp.modules.productionplan.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.AverageCostUpdater;
import mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import mz.multicore.erp.modules.manufacturing.model.BillOfMaterials;
import mz.multicore.erp.modules.manufacturing.model.BomItem;
import mz.multicore.erp.modules.manufacturing.model.BomType;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import mz.multicore.erp.modules.productionplan.dto.*;
import mz.multicore.erp.modules.productionplan.model.ProductionPlan;
import mz.multicore.erp.modules.productionplan.model.ProductionPlanLine;
import mz.multicore.erp.modules.productionplan.model.ProductionPlanStatus;
import mz.multicore.erp.modules.productionplan.model.ProductionRun;
import mz.multicore.erp.modules.productionplan.repository.ProductionPlanLineRepository;
import mz.multicore.erp.modules.productionplan.repository.ProductionPlanRepository;
import mz.multicore.erp.modules.productionplan.repository.ProductionRunRepository;
import mz.multicore.erp.modules.productionplan.repository.ProductionSalesHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Production plan of the bakery (F4, {@code docs/PLANO_PRODUCAO_SPEC.md}). Recipes are the ordinary bills of
 * materials. The plan says how much of each to make today; the suggestion averages what was sold on the same
 * weekday; the requirements list the flour, sugar and yeast still missing. Each batch made consumes ingredients
 * by FEFO and enters the product with its expiry and real cost, through {@link StockMovementRegistrationService}.
 */
@Service
public class ProductionPlanService {

    private static final String MOVEMENT_TYPE = "PRODUCTION";
    private static final int SUGGESTION_WEEKS = 4;
    private static final DateTimeFormatter BATCH_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final ProductionPlanRepository planRepository;
    private final ProductionRunRepository runRepository;
    private final ProductionPlanLineRepository lineRepository;
    private final ProductionSalesHistoryRepository salesHistory;
    private final BillOfMaterialsRepository bomRepository;
    private final WarehouseLookup warehouseLookup;
    private final StockRepository stockRepository;
    private final StockMovementRegistrationService movementService;
    private final AverageCostUpdater averageCost;
    private final StockWasteService wasteService;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ProductionPlanService(ProductionPlanRepository planRepository, ProductionRunRepository runRepository,
                                 ProductionPlanLineRepository lineRepository,
                                 ProductionSalesHistoryRepository salesHistory, BillOfMaterialsRepository bomRepository,
                                 WarehouseLookup warehouseLookup, StockRepository stockRepository,
                                 StockMovementRegistrationService movementService, AverageCostUpdater averageCost,
                                 StockWasteService wasteService, AuditLogService auditLogService,
                                 org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.planRepository = planRepository;
        this.runRepository = runRepository;
        this.lineRepository = lineRepository;
        this.salesHistory = salesHistory;
        this.bomRepository = bomRepository;
        this.warehouseLookup = warehouseLookup;
        this.stockRepository = stockRepository;
        this.movementService = movementService;
        this.averageCost = averageCost;
        this.wasteService = wasteService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public ProductionPlanDTO day(Long companyId, Long warehouseId, LocalDate date) {
        CurrentUserContext.requireCompany(companyId);
        Warehouse warehouse = requireWarehouse(companyId, warehouseId);
        return planRepository.findDay(companyId, warehouseId, date).map(this::toDto)
                .orElse(new ProductionPlanDTO(null, date, warehouse.getId(), warehouse.getName(), ProductionPlanStatus.OPEN,
                        null, List.of(), List.of(), BigDecimal.ZERO, null));
    }

    @Transactional(readOnly = true)
    public List<ProductionSuggestionDTO> suggestions(Long companyId, LocalDate date) {
        CurrentUserContext.requireCompany(companyId);
        List<BillOfMaterials> recipes = recipes(companyId);
        if (recipes.isEmpty()) return List.of();
        Set<Long> productIds = new HashSet<>();
        recipes.forEach(r -> productIds.add(r.getFinishedProduct().getId()));
        Map<Long, Map<LocalDate, BigDecimal>> sold = new HashMap<>();
        for (Object[] row : salesHistory.soldLines(companyId, date.minusWeeks(SUGGESTION_WEEKS).atStartOfDay(), date.atStartOfDay(),
                InvoiceStatus.realisedSaleStatuses(), productIds)) {
            LocalDate day = ((LocalDateTime) row[1]).toLocalDate();
            sold.computeIfAbsent((Long) row[0], k -> new HashMap<>()).merge(day, (BigDecimal) row[2], BigDecimal::add);
        }
        List<ProductionSuggestionDTO> result = new ArrayList<>();
        for (BillOfMaterials r : recipes) {
            Product p = r.getFinishedProduct();
            result.add(new ProductionSuggestionDTO(r.getId(), p.getId(), p.getName(),
                    ProductionPlanRules.suggestion(sold.getOrDefault(p.getId(), Map.of()), date, SUGGESTION_WEEKS), SUGGESTION_WEEKS));
        }
        return result;
    }

    @Transactional
    public ProductionPlanDTO save(SaveProductionPlanRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("planear produção");
        Warehouse warehouse = requireWarehouse(request.companyId(), request.warehouseId());
        ProductionPlan plan = planRepository.findDay(request.companyId(), warehouse.getId(), request.planDate()).orElseGet(() -> {
            ProductionPlan created = new ProductionPlan();
            created.setCompany(warehouse.getCompany());
            created.setWarehouse(warehouse);
            created.setPlanDate(request.planDate());
            return created;
        });
        requireOpen(plan);
        plan.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        Map<Long, ProductionPlanLine> existing = new HashMap<>();
        plan.getLines().forEach(l -> existing.put(l.getBom().getId(), l));
        Set<Long> requested = new HashSet<>();
        int order = 0;
        for (PlanLineRequest l : request.lines()) {
            if (!requested.add(l.bomId())) throw new BusinessRuleException("Cada receita só aparece uma vez no plano.");
            ProductionPlanLine line = existing.get(l.bomId());
            if (line == null) {
                if (l.plannedQty().signum() == 0) continue;
                BillOfMaterials bom = requireRecipe(request.companyId(), l.bomId());
                line = new ProductionPlanLine();
                line.setPlan(plan);
                line.setBom(bom);
                line.setProduct(bom.getFinishedProduct());
                plan.getLines().add(line);
            }
            line.setPlannedQty(l.plannedQty());
            line.setSortOrder(order++);
        }
        plan.getLines().removeIf(l -> !requested.contains(l.getBom().getId()) && l.getProducedQty().signum() == 0
                || l.getPlannedQty().signum() == 0 && l.getProducedQty().signum() == 0);
        plan = planRepository.save(plan);
        auditLogService.logCurrent("PRODUCTION_PLAN_SAVE", "Plano de produção de " + plan.getPlanDate() + " ("
                + warehouse.getName() + "): " + plan.getLines().size() + " receitas.");
        return toDto(plan);
    }

    /**
     * Adds quantities to the plan of a day (custom orders sent to production). Existing lines grow; recipes not
     * yet planned get a new line. A closed day refuses it.
     */
    @Transactional
    public ProductionPlanDTO addToPlan(Long companyId, Long warehouseId, LocalDate date, Map<Long, BigDecimal> quantitiesByBom) {
        CurrentUserContext.requireCompany(companyId);
        Warehouse warehouse = requireWarehouse(companyId, warehouseId);
        ProductionPlan plan = planRepository.findDay(companyId, warehouseId, date).orElseGet(() -> {
            ProductionPlan created = new ProductionPlan();
            created.setCompany(warehouse.getCompany());
            created.setWarehouse(warehouse);
            created.setPlanDate(date);
            return created;
        });
        requireOpen(plan);
        int order = plan.getLines().size();
        for (Map.Entry<Long, BigDecimal> entry : quantitiesByBom.entrySet()) {
            ProductionPlanLine line = plan.getLines().stream().filter(l -> l.getBom().getId().equals(entry.getKey())).findFirst()
                    .orElse(null);
            if (line == null) {
                BillOfMaterials bom = requireRecipe(companyId, entry.getKey());
                line = new ProductionPlanLine();
                line.setPlan(plan);
                line.setBom(bom);
                line.setProduct(bom.getFinishedProduct());
                line.setSortOrder(order++);
                plan.getLines().add(line);
            }
            line.setPlannedQty(line.getPlannedQty().add(entry.getValue()));
        }
        return toDto(planRepository.save(plan));
    }

    /** First active recipe that makes the product, if any. */
    @Transactional(readOnly = true)
    public Optional<Long> recipeFor(Long companyId, Long productId) {
        return recipes(companyId).stream().filter(b -> b.getFinishedProduct().getId().equals(productId)).map(BillOfMaterials::getId).findFirst();
    }

    @Transactional(readOnly = true)
    public List<MaterialRequirementDTO> requirements(Long companyId, Long planId) {
        CurrentUserContext.requireCompany(companyId);
        ProductionPlan plan = planRepository.findById(planId).filter(p -> p.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new BusinessRuleException("Plano de produção não encontrado."));
        Map<Long, BigDecimal> required = new LinkedHashMap<>();
        Map<Long, BomItem> items = new HashMap<>();
        for (ProductionPlanLine line : plan.getLines()) {
            BigDecimal remaining = ProductionPlanRules.remaining(line.getPlannedQty(), line.getProducedQty());
            for (BomItem item : line.getBom().getItems()) {
                BigDecimal qty = ProductionPlanRules.ingredient(remaining, line.getBom().getBaseQuantity(), item.getRequiredQuantity(),
                        item.getScrapPercentage());
                required.merge(item.getRawMaterialProduct().getId(), qty, BigDecimal::add);
                items.putIfAbsent(item.getRawMaterialProduct().getId(), item);
            }
        }
        List<MaterialRequirementDTO> result = new ArrayList<>();
        required.forEach((productId, qty) -> {
            BomItem item = items.get(productId);
            Product raw = item.getRawMaterialProduct();
            BigDecimal stock = raw.isStockTracked() ? stockOf(productId, plan.getWarehouse().getId()) : null;
            BigDecimal shortage = stock == null ? BigDecimal.ZERO : qty.subtract(stock).max(BigDecimal.ZERO);
            result.add(new MaterialRequirementDTO(productId, raw.getName(), item.getUnitOfMeasure(), qty.setScale(3, RoundingMode.HALF_UP),
                    stock, shortage.setScale(3, RoundingMode.HALF_UP)));
        });
        result.sort(Comparator.comparing(MaterialRequirementDTO::shortage).reversed().thenComparing(MaterialRequirementDTO::productName));
        return result;
    }

    @Transactional
    public ProductionRunDTO recordRun(RecordRunRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("apontar produção");
        var existing = runRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());
        ProductionPlanLine line = findLine(request.companyId(), request.planLineId());
        ProductionPlan plan = planRepository.lockByIdAndCompanyId(line.getPlan().getId(), request.companyId()).orElseThrow();
        requireOpen(plan);
        Product product = line.getProduct();
        if (product.isTracksExpiry() && request.expirationDate() == null) {
            throw new BusinessRuleException("'" + product.getName() + "' é perecível: indique a validade do lote produzido.");
        }
        if (request.expirationDate() != null && request.expirationDate().isBefore(plan.getPlanDate())) {
            throw new BusinessRuleException("A validade não pode ser anterior ao dia da produção.");
        }
        Warehouse warehouse = plan.getWarehouse();
        BillOfMaterials bom = line.getBom();
        String batch = "PD" + plan.getPlanDate().format(BATCH_DATE) + "-" + line.getId() + "-"
                + (runRepository.findByPlan(plan.getId()).stream().filter(r -> r.getLine().getId().equals(line.getId())).count() + 1);
        String description = "Produção " + batch + " - " + product.getName();
        BigDecimal ingredientCost = BigDecimal.ZERO;
        for (BomItem item : bom.getItems()) {
            Product raw = item.getRawMaterialProduct();
            BigDecimal qty = ProductionPlanRules.ingredient(request.quantity(), bom.getBaseQuantity(), item.getRequiredQuantity(),
                    item.getScrapPercentage());
            if (qty.signum() == 0) continue;
            if (raw.isStockTracked()) {
                BigDecimal stock = stockOf(raw.getId(), warehouse.getId());
                if (stock.compareTo(qty) < 0) {
                    throw new BusinessRuleException("Falta " + raw.getName() + ": precisa de " + DisplayNumbers.qty(qty)
                            + " e há " + DisplayNumbers.qty(stock) + " em " + warehouse.getName() + ".");
                }
                movementService.registerMovement(raw, warehouse, qty.negate(), MOVEMENT_TYPE, null, null, description, null);
            }
            BigDecimal cost = raw.getPurchasePrice() == null ? BigDecimal.ZERO : raw.getPurchasePrice();
            ingredientCost = ingredientCost.add(qty.multiply(cost));
        }
        BigDecimal base = bom.getBaseQuantity() == null || bom.getBaseQuantity().signum() <= 0 ? BigDecimal.ONE : bom.getBaseQuantity();
        BigDecimal overhead = nz(bom.getEstimatedLaborCost()).add(nz(bom.getEstimatedOverheadCost()))
                .multiply(request.quantity()).divide(base, 2, RoundingMode.HALF_UP);
        ingredientCost = ingredientCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = ingredientCost.add(overhead);
        if (product.isStockTracked()) {
            averageCost.beforeEntry(product, warehouse, request.quantity(), total);
            movementService.registerMovement(product, warehouse, request.quantity(), MOVEMENT_TYPE, batch, null, description,
                    request.expirationDate());
        }
        line.setProducedQty(line.getProducedQty().add(request.quantity()));
        ProductionRun run = new ProductionRun();
        run.setCompany(plan.getCompany());
        run.setLine(line);
        run.setQuantity(request.quantity());
        run.setBatchNumber(batch);
        run.setExpirationDate(request.expirationDate());
        run.setIngredientCost(ingredientCost);
        run.setTotalCost(total);
        run.setUnitCost(total.divide(request.quantity(), 4, RoundingMode.HALF_UP));
        run.setRequestKey(request.requestKey());
        run.setCreatedBy(CurrentUserContext.getUsername());
        run = runRepository.save(run);
        // Ingredients leave at cost (consumption); the product enters at its full cost, labour and overhead included.
        eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.InventoryValueChangedEvent(plan.getCompany().getId(),
                mz.multicore.erp.modules.accounting.model.JournalSource.PRODUCTION_RUN, run.getId(), batch, plan.getPlanDate(),
                ingredientCost, product.isStockTracked() ? total : BigDecimal.ZERO,
                mz.multicore.erp.modules.accounting.service.PgcNirfChart.VARIACAO_PRODUCAO, product.getId()));
        auditLogService.logCurrent("PRODUCTION_RUN", "Produzido " + DisplayNumbers.qty(request.quantity()) + " de " + product.getName()
                + " (lote " + batch + ", custo " + DisplayNumbers.money(total) + " MT).");
        return toDto(run);
    }

    @Transactional
    public ProductionPlanDTO close(Long planId, ClosePlanRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("fechar o dia de produção");
        ProductionPlan plan = planRepository.lockByIdAndCompanyId(planId, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Plano de produção não encontrado."));
        requireOpen(plan);
        for (LeftoverRequest leftover : request.leftovers()) {
            if (leftover.quantity().signum() == 0) continue;
            ProductionPlanLine line = plan.getLines().stream().filter(l -> l.getId().equals(leftover.planLineId())).findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Linha do plano não encontrada."));
            if (leftover.quantity().compareTo(line.getProducedQty()) > 0) {
                throw new BusinessRuleException("Sobra de " + line.getProduct().getName() + " maior do que o produzido ("
                        + DisplayNumbers.qty(line.getProducedQty()) + ").");
            }
            line.setLeftoverQty(leftover.quantity());
            if (leftover.writeOff() && line.getProduct().isStockTracked()) {
                wasteService.registerWaste(new CreateStockWasteRequest(request.companyId(), plan.getWarehouse().getId(),
                        line.getProduct().getId(), null, leftover.quantity(), WasteReason.EXPIRED,
                        "Sobra do dia " + plan.getPlanDate(), "PD-CLOSE-" + plan.getId() + "-" + line.getId()));
            }
        }
        plan.setStatus(ProductionPlanStatus.CLOSED);
        plan.setClosedAt(LocalDateTime.now());
        plan.setClosedBy(CurrentUserContext.getUsername());
        plan = planRepository.save(plan);
        auditLogService.logCurrent("PRODUCTION_PLAN_CLOSE", "Dia de produção " + plan.getPlanDate() + " fechado.");
        return toDto(plan);
    }

    private List<BillOfMaterials> recipes(Long companyId) {
        return bomRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId).stream()
                .filter(b -> b.getBomType() == null || b.getBomType() == BomType.STANDARD).toList();
    }

    private BillOfMaterials requireRecipe(Long companyId, Long bomId) {
        BillOfMaterials bom = bomRepository.findByIdAndCompanyId(bomId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Receita não encontrada."));
        if (!bom.isActive() || (bom.getBomType() != null && bom.getBomType() != BomType.STANDARD)) {
            throw new BusinessRuleException("A receita '" + bom.getName() + "' não está activa para produção.");
        }
        return bom;
    }

    private ProductionPlanLine findLine(Long companyId, Long lineId) {
        return lineRepository.findByIdAndCompanyId(lineId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Linha do plano não encontrada."));
    }

    private Warehouse requireWarehouse(Long companyId, Long warehouseId) {
        return warehouseLookup.requireForCompany(warehouseId, companyId);
    }

    private static void requireOpen(ProductionPlan plan) {
        if (plan.getStatus() == ProductionPlanStatus.CLOSED) {
            throw new BusinessRuleException("O dia de produção " + plan.getPlanDate() + " já foi fechado.");
        }
    }

    private BigDecimal stockOf(Long productId, Long warehouseId) {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId).map(Stock::getQuantity).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ProductionPlanDTO toDto(ProductionPlan plan) {
        List<ProductionRun> runs = plan.getId() == null ? List.of() : runRepository.findByPlan(plan.getId());
        List<ProductionPlanLineDTO> lines = plan.getLines().stream().map(l -> {
            List<BigDecimal> last = l.getProduct().getId() == null ? List.of()
                    : runRepository.lastUnitCosts(l.getProduct().getId(), PageRequest.of(0, 1));
            return new ProductionPlanLineDTO(l.getId(), l.getBom().getId(), l.getBom().getName(), l.getProduct().getId(),
                    l.getProduct().getName(), l.getPlannedQty(), l.getProducedQty(),
                    ProductionPlanRules.remaining(l.getPlannedQty(), l.getProducedQty()), l.getLeftoverQty(),
                    last.isEmpty() ? null : last.get(0), l.getProduct().getUnitPrice());
        }).toList();
        BigDecimal cost = runs.stream().map(ProductionRun::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProductionPlanDTO(plan.getId(), plan.getPlanDate(), plan.getWarehouse().getId(), plan.getWarehouse().getName(),
                plan.getStatus(), plan.getNotes(), lines, runs.stream().map(ProductionPlanService::toDto).toList(), cost, plan.getClosedBy());
    }

    static ProductionRunDTO toDto(ProductionRun r) {
        return new ProductionRunDTO(r.getId(), r.getLine().getId(), r.getLine().getProduct().getName(), r.getQuantity(),
                r.getBatchNumber(), r.getExpirationDate(), r.getIngredientCost(), r.getTotalCost(), r.getUnitCost(),
                r.getCreatedBy(), r.getCreatedAt());
    }
}
