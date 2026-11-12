package mz.multicore.erp.modules.butchery.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.butchery.dto.*;
import mz.multicore.erp.modules.butchery.model.*;
import mz.multicore.erp.modules.butchery.repository.BreakdownTemplateRepository;
import mz.multicore.erp.modules.butchery.repository.CarcassBreakdownRepository;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.AverageCostUpdater;
import mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Carcass breakdown of the butchery (F3, {@code docs/DESMANCHA_SPEC.md}). A whole ox or a quarter enters by
 * weight and leaves as cuts; each cut takes its share of the carcass cost by sale value, so the butcher finally
 * knows what a kg of steak costs. Stock moves through {@link StockMovementRegistrationService} only.
 */
@Service
public class CarcassBreakdownService {

    private static final String MOVEMENT_TYPE = "PRODUCTION";

    private final BreakdownTemplateRepository templateRepository;
    private final CarcassBreakdownRepository breakdownRepository;
    private final ProductLookup productLookup;
    private final CompanyLookup companyLookup;
    private final WarehouseLookup warehouseLookup;
    private final StockRepository stockRepository;
    private final StockMovementRegistrationService movementService;
    private final AverageCostUpdater averageCost;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public CarcassBreakdownService(BreakdownTemplateRepository templateRepository, CarcassBreakdownRepository breakdownRepository,
                                   ProductLookup productLookup, CompanyLookup companyLookup,
                                   WarehouseLookup warehouseLookup, StockRepository stockRepository,
                                   StockMovementRegistrationService movementService, AverageCostUpdater averageCost,
                                   DocumentNumberService documentNumberService, AuditLogService auditLogService,
                                   org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.templateRepository = templateRepository;
        this.breakdownRepository = breakdownRepository;
        this.productLookup = productLookup;
        this.companyLookup = companyLookup;
        this.warehouseLookup = warehouseLookup;
        this.stockRepository = stockRepository;
        this.movementService = movementService;
        this.averageCost = averageCost;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    // ─── Models ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BreakdownTemplateDTO> templates(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return templateRepository.findByCompany(companyId).stream().map(CarcassBreakdownService::toDto).toList();
    }

    @Transactional
    public BreakdownTemplateDTO saveTemplate(SaveBreakdownTemplateRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("definir modelo de desmancha");
        BreakdownTemplate template = request.id() == null ? new BreakdownTemplate()
                : templateRepository.findByIdAndCompanyId(request.id(), request.companyId())
                        .orElseThrow(() -> new BusinessRuleException("Modelo de desmancha não encontrado."));
        if (template.getCompany() == null) template.setCompany(company(request.companyId()));
        Product source = trackedProduct(request.companyId(), request.sourceProductId());
        template.setName(request.name().trim());
        template.setSourceProduct(source);
        template.setLossTolerancePct(request.lossTolerancePct());
        template.setActive(request.active());
        Set<Long> seen = new HashSet<>();
        BigDecimal expected = BigDecimal.ZERO;
        template.getLines().clear();
        int order = 0;
        for (TemplateLineRequest l : request.lines()) {
            if (l.productId().equals(source.getId())) throw new BusinessRuleException("A carcaça não pode ser um dos cortes.");
            if (!seen.add(l.productId())) throw new BusinessRuleException("Cada corte só aparece uma vez no modelo.");
            BreakdownTemplateLine line = new BreakdownTemplateLine();
            line.setTemplate(template);
            line.setProduct(trackedProduct(request.companyId(), l.productId()));
            line.setExpectedYieldPct(l.expectedYieldPct());
            line.setSortOrder(order++);
            template.getLines().add(line);
            expected = expected.add(l.expectedYieldPct());
        }
        if (expected.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("O rendimento esperado dos cortes soma " + DisplayNumbers.qty(expected)
                    + "%: não pode passar de 100%.");
        }
        template = templateRepository.save(template);
        auditLogService.logCurrent("BREAKDOWN_TEMPLATE_SAVE", "Modelo de desmancha '" + template.getName() + "' guardado.");
        return toDto(template);
    }

    // ─── Breakdowns ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CarcassBreakdownDTO> breakdowns(Long companyId, int days) {
        CurrentUserContext.requireCompany(companyId);
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, Math.min(days, 366)));
        return breakdownRepository.findByCompanySince(companyId, from).stream().map(CarcassBreakdownService::toDto).toList();
    }

    @Transactional
    public CarcassBreakdownDTO post(PostBreakdownRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("registar desmancha");
        var existing = breakdownRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());

        Warehouse warehouse = warehouseLookup.requireForCompany(request.warehouseId(), request.companyId());
        Product source = trackedProduct(request.companyId(), request.sourceProductId());
        BreakdownTemplate template = request.templateId() == null ? null
                : templateRepository.findByIdAndCompanyId(request.templateId(), request.companyId())
                        .orElseThrow(() -> new BusinessRuleException("Modelo de desmancha não encontrado."));
        if (template != null && !template.getSourceProduct().getId().equals(source.getId())) {
            throw new BusinessRuleException("O modelo '" + template.getName() + "' é de outra carcaça.");
        }

        List<Product> cuts = new ArrayList<>();
        List<BreakdownCostRules.Cut> costing = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        BigDecimal outputKg = BigDecimal.ZERO;
        for (BreakdownLineRequest l : request.lines()) {
            if (l.productId().equals(source.getId())) throw new BusinessRuleException("A carcaça não pode ser um dos cortes.");
            if (!seen.add(l.productId())) throw new BusinessRuleException("Cada corte só aparece uma vez: some as pesagens.");
            Product cut = trackedProduct(request.companyId(), l.productId());
            cuts.add(cut);
            costing.add(new BreakdownCostRules.Cut(cut.getId(), l.kg(), cut.getUnitPrice()));
            outputKg = outputKg.add(l.kg());
        }
        BreakdownCostRules.requireOutputWithinInput(request.inputKg(), outputKg);
        BigDecimal inputCost = request.inputCost() != null ? request.inputCost()
                : nz(source.getPurchasePrice()).multiply(request.inputKg()).setScale(2, RoundingMode.HALF_UP);
        List<BreakdownCostRules.Allocation> allocations = BreakdownCostRules.allocate(inputCost, costing);

        CarcassBreakdown breakdown = new CarcassBreakdown();
        breakdown.setCompany(warehouse.getCompany());
        breakdown.setNumber(documentNumberService.next(DocumentSeries.CARCASS_BREAKDOWN));
        breakdown.setWarehouse(warehouse);
        breakdown.setTemplate(template);
        breakdown.setSourceProduct(source);
        breakdown.setInputKg(request.inputKg());
        breakdown.setInputCost(inputCost);
        breakdown.setOutputKg(outputKg);
        breakdown.setLossTolerancePct(template == null ? null : template.getLossTolerancePct());
        breakdown.setReference(blankToNull(request.reference()));
        breakdown.setNotes(blankToNull(request.notes()));
        breakdown.setRequestKey(request.requestKey());
        breakdown.setCreatedBy(CurrentUserContext.getUsername());

        String description = "Desmancha " + breakdown.getNumber() + " - " + source.getName();
        BigDecimal carcassBookValue = nz(source.getPurchasePrice()).multiply(request.inputKg()).setScale(2, RoundingMode.HALF_UP);
        movementService.registerMovement(source, warehouse, request.inputKg().negate(), MOVEMENT_TYPE, null, null, description, null);
        for (int i = 0; i < cuts.size(); i++) {
            Product cut = cuts.get(i);
            BreakdownCostRules.Allocation allocation = allocations.get(i);
            BigDecimal kg = request.lines().get(i).kg();
            averageCost.beforeEntry(cut, warehouse, kg, allocation.cost());
            movementService.registerMovement(cut, warehouse, kg, MOVEMENT_TYPE, breakdown.getNumber(), null, description, null);
            CarcassBreakdownLine line = new CarcassBreakdownLine();
            line.setBreakdown(breakdown);
            line.setProduct(cut);
            line.setKg(kg);
            line.setUnitSalePrice(nz(cut.getUnitPrice()));
            line.setAllocatedCost(allocation.cost());
            line.setCostPerKg(allocation.costPerKg());
            line.setExpectedYieldPct(expectedYield(template, cut.getId()));
            breakdown.getLines().add(line);
        }
        breakdown = breakdownRepository.save(breakdown);
        // The carcass leaves stock at its book value and the cuts enter at the cost shared among them.
        eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.InventoryValueChangedEvent(warehouse.getCompany().getId(),
                mz.multicore.erp.modules.accounting.model.JournalSource.CARCASS_BREAKDOWN, breakdown.getId(), breakdown.getNumber(),
                java.time.LocalDate.now(), carcassBookValue, inputCost, null));
        CarcassBreakdownDTO dto = toDto(breakdown);
        auditLogService.logCurrent("CARCASS_BREAKDOWN_POST", "Desmancha " + breakdown.getNumber() + ": "
                + DisplayNumbers.qty(request.inputKg()) + " kg de " + source.getName() + " deram "
                + DisplayNumbers.qty(outputKg) + " kg em " + cuts.size() + " cortes (perda " + DisplayNumbers.qty(dto.lossPct()) + "%).");
        return dto;
    }

    @Transactional
    public CarcassBreakdownDTO cancel(Long id, CancelBreakdownRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("anular desmancha");
        CarcassBreakdown breakdown = breakdownRepository.lockByIdAndCompanyId(id, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Desmancha não encontrada."));
        if (breakdown.getStatus() != CarcassBreakdownStatus.POSTED) {
            throw new BusinessRuleException("Esta desmancha já foi anulada.");
        }
        String description = "Anulação da desmancha " + breakdown.getNumber() + " - " + request.reason().trim();
        for (CarcassBreakdownLine line : breakdown.getLines()) {
            BigDecimal available = stockRepository.findByProductIdAndWarehouseId(line.getProduct().getId(), breakdown.getWarehouse().getId())
                    .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
            if (available.compareTo(line.getKg()) < 0) {
                throw new BusinessRuleException("Já não há " + DisplayNumbers.qty(line.getKg()) + " kg de " + line.getProduct().getName()
                        + " em stock (há " + DisplayNumbers.qty(available) + " kg): parte já foi vendida e a desmancha não se anula.");
            }
            movementService.registerMovement(line.getProduct(), breakdown.getWarehouse(), line.getKg().negate(), "REVERSAL",
                    null, null, description, null);
        }
        movementService.registerMovement(breakdown.getSourceProduct(), breakdown.getWarehouse(), breakdown.getInputKg(), "REVERSAL",
                null, null, description, null);
        eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.InventoryValueChangedEvent(
                breakdown.getCompany().getId(), mz.multicore.erp.modules.accounting.model.JournalSource.CARCASS_BREAKDOWN_CANCEL,
                breakdown.getId(), breakdown.getNumber(), java.time.LocalDate.now(), breakdown.getInputCost(),
                nz(breakdown.getSourceProduct().getPurchasePrice()).multiply(breakdown.getInputKg()).setScale(2, RoundingMode.HALF_UP), null));
        breakdown.setStatus(CarcassBreakdownStatus.CANCELLED);
        breakdown.setCancellationReason(request.reason().trim());
        breakdown.setCancelledAt(LocalDateTime.now());
        breakdown.setCancelledBy(CurrentUserContext.getUsername());
        breakdown = breakdownRepository.save(breakdown);
        auditLogService.logCurrent("CARCASS_BREAKDOWN_CANCEL", "Desmancha " + breakdown.getNumber() + " anulada: " + request.reason().trim());
        return toDto(breakdown);
    }

    private Product trackedProduct(Long companyId, Long productId) {
        Product product = productLookup.findForCompany(productId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado nesta empresa (ID " + productId + ")."));
        if (!product.isStockTracked()) {
            throw new BusinessRuleException("O artigo '" + product.getName() + "' não controla stock e não entra numa desmancha.");
        }
        return product;
    }

    private Company company(Long companyId) {
        return companyLookup.require(companyId);
    }

    private static BigDecimal expectedYield(BreakdownTemplate template, Long productId) {
        if (template == null) return null;
        return template.getLines().stream().filter(l -> l.getProduct().getId().equals(productId))
                .map(BreakdownTemplateLine::getExpectedYieldPct).findFirst().orElse(null);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static BreakdownTemplateDTO toDto(BreakdownTemplate t) {
        List<BreakdownTemplateLineDTO> lines = t.getLines().stream().map(l -> new BreakdownTemplateLineDTO(
                l.getProduct().getId(), l.getProduct().getName(), l.getProduct().getSku(), l.getExpectedYieldPct(),
                l.getProduct().getUnitPrice())).toList();
        BigDecimal expected = t.getLines().stream().map(BreakdownTemplateLine::getExpectedYieldPct).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BreakdownTemplateDTO(t.getId(), t.getName(), t.getSourceProduct().getId(), t.getSourceProduct().getName(),
                t.getLossTolerancePct(), t.isActive(), expected, lines);
    }

    static CarcassBreakdownDTO toDto(CarcassBreakdown b) {
        BigDecimal loss = b.getInputKg().subtract(b.getOutputKg());
        BigDecimal lossPct = BreakdownCostRules.percentOf(loss, b.getInputKg());
        List<CarcassBreakdownLineDTO> lines = b.getLines().stream().map(l -> new CarcassBreakdownLineDTO(
                l.getProduct().getId(), l.getProduct().getName(), l.getKg(), l.getUnitSalePrice(), l.getAllocatedCost(),
                l.getCostPerKg(), l.getExpectedYieldPct(), BreakdownCostRules.percentOf(l.getKg(), b.getInputKg()))).toList();
        return new CarcassBreakdownDTO(b.getId(), b.getNumber(), b.getCreatedAt(), b.getCreatedBy(), b.getWarehouse().getId(),
                b.getWarehouse().getName(), b.getTemplate() == null ? null : b.getTemplate().getId(),
                b.getTemplate() == null ? null : b.getTemplate().getName(), b.getSourceProduct().getId(),
                b.getSourceProduct().getName(), b.getInputKg(), b.getInputCost(), b.getOutputKg(), loss, lossPct,
                b.getLossTolerancePct(), BreakdownCostRules.lossAboveTolerance(lossPct, b.getLossTolerancePct()),
                b.getStatus(), b.getReference(), b.getNotes(), b.getCancellationReason(), lines);
    }
}
