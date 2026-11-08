package mz.multicore.erp.modules.bulkstock.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.bulkstock.dto.*;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurement;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurementMethod;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurementStatus;
import mz.multicore.erp.modules.bulkstock.model.BulkStockSetting;
import mz.multicore.erp.modules.bulkstock.repository.BulkMeasurementRepository;
import mz.multicore.erp.modules.bulkstock.repository.BulkStockSettingRepository;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService;
import mz.multicore.erp.modules.pickup.service.PickupService;
import mz.multicore.erp.modules.units.dto.UnitConversionDTO;
import mz.multicore.erp.modules.units.service.ProductUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Bulk stock measured on the pile (SA-C17, {@code docs/STOCK_GRANEL_SPEC.md}).
 *
 * <p>Sand, gravel and stone cannot be counted, so the yard measures the pile. The measured quantity minus
 * sold material still awaiting pickup is compared with the system stock. A difference within the product
 * tolerance is posted at once as natural loss or gain; a larger one waits for a manager. Every posting goes
 * through {@link StockMovementRegistrationService} as an {@code ADJUSTMENT}.
 */
@Service
public class BulkStockService {

    private static final String CUBIC_METRE = "M3";

    private final BulkStockSettingRepository settingRepository;
    private final BulkMeasurementRepository measurementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final CompanyRepository companyRepository;
    private final StockMovementRegistrationService movementService;
    private final ProductUnitService productUnitService;
    private final PickupService pickupService;
    private final AuditLogService auditLogService;

    public BulkStockService(BulkStockSettingRepository settingRepository, BulkMeasurementRepository measurementRepository,
                            ProductRepository productRepository, WarehouseRepository warehouseRepository,
                            StockRepository stockRepository, CompanyRepository companyRepository,
                            StockMovementRegistrationService movementService, ProductUnitService productUnitService,
                            PickupService pickupService, AuditLogService auditLogService) {
        this.settingRepository = settingRepository;
        this.measurementRepository = measurementRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.companyRepository = companyRepository;
        this.movementService = movementService;
        this.productUnitService = productUnitService;
        this.pickupService = pickupService;
        this.auditLogService = auditLogService;
    }

    // ---- Settings ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BulkStockSettingDTO> settings(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return settingRepository.findByCompany(companyId).stream().map(BulkStockService::toDto).toList();
    }

    @Transactional
    public List<BulkStockSettingDTO> saveSetting(SaveBulkStockSettingRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("definir produtos a granel");
        Product product = requireProduct(request.companyId(), request.productId());
        var existing = settingRepository.findByCompanyIdAndProductId(request.companyId(), product.getId());
        if (request.tolerancePercent() == null) {
            existing.ifPresent(settingRepository::delete);
            auditLogService.logCurrent("BULK_STOCK_SETTING_REMOVE", "Produto " + product.getSku() + " deixou de ser granel");
            return settings(request.companyId());
        }
        if (!product.isStockTracked()) throw new BusinessRuleException("Este produto não controla stock físico.");
        BigDecimal tolerance = BulkMeasurementRules.requireTolerance(request.tolerancePercent());
        BulkStockSetting setting = existing.orElseGet(BulkStockSetting::new);
        if (setting.getId() == null) {
            setting.setCompany(companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada.")));
            setting.setProduct(product);
        }
        setting.setTolerancePercent(tolerance);
        settingRepository.save(setting);
        auditLogService.logCurrent("BULK_STOCK_SETTING_SAVE", "Produto " + product.getSku() + " a granel, tolerância "
                + DisplayNumbers.qty(tolerance) + "%");
        return settings(request.companyId());
    }

    // ---- Measurements --------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BulkMeasurementDTO> measurements(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return measurementRepository.findByCompany(companyId).stream().map(BulkStockService::toDto).toList();
    }

    /** Computes the comparison without saving, so the yard sees the result before confirming. */
    @Transactional(readOnly = true)
    public BulkMeasurementDTO preview(BulkMeasurementRequest request) {
        return toDto(compute(request));
    }

    @Transactional
    public BulkMeasurementDTO record(BulkMeasurementRequest request) {
        PermissionGuard.requireSeller("registar medição de granel");
        BulkMeasurement measurement = compute(request);
        measurement.setCreatedBy(CurrentUserContext.getUsername());
        if (measurement.getDifference().signum() == 0) {
            measurement.setStatus(BulkMeasurementStatus.NO_DIFFERENCE);
        } else if (BulkMeasurementRules.withinTolerance(measurement.getDifferencePercent(), measurement.getTolerancePercent())) {
            measurement.setStatus(BulkMeasurementStatus.APPLIED);
        } else {
            measurement.setStatus(BulkMeasurementStatus.PENDING_APPROVAL);
        }
        BulkMeasurement saved = measurementRepository.saveAndFlush(measurement);
        if (saved.getStatus() == BulkMeasurementStatus.APPLIED) post(saved, "dentro da tolerância");
        auditLogService.logCurrent("BULK_MEASUREMENT_RECORD", describe(saved));
        return toDto(saved);
    }

    @Transactional
    public BulkMeasurementDTO approve(Long id, ReviewBulkMeasurementRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("aprovar acerto de granel acima da tolerância");
        BulkMeasurement measurement = requirePending(request.companyId(), id);
        measurement.setStatus(BulkMeasurementStatus.APPLIED);
        markReviewed(measurement, null);
        post(measurement, "aprovado por " + CurrentUserContext.getUsername());
        auditLogService.logCurrent("BULK_MEASUREMENT_APPROVE", describe(measurement));
        return toDto(measurement);
    }

    @Transactional
    public BulkMeasurementDTO reject(Long id, ReviewBulkMeasurementRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("rejeitar acerto de granel");
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessRuleException("Indique o motivo da rejeição (por exemplo, medir de novo).");
        }
        BulkMeasurement measurement = requirePending(request.companyId(), id);
        measurement.setStatus(BulkMeasurementStatus.REJECTED);
        markReviewed(measurement, request.reason().trim());
        measurementRepository.save(measurement);
        auditLogService.logCurrent("BULK_MEASUREMENT_REJECT", describe(measurement) + ". Motivo: " + request.reason().trim());
        return toDto(measurement);
    }

    // ---- Support -------------------------------------------------------------------------------

    private BulkMeasurement compute(BulkMeasurementRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        Product product = requireProduct(request.companyId(), request.productId());
        BulkStockSetting setting = settingRepository.findByCompanyIdAndProductId(request.companyId(), product.getId())
                .orElseThrow(() -> new BusinessRuleException("O produto " + product.getName()
                        + " não está marcado como granel. Um gestor define-o primeiro, com a tolerância de perda."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .filter(w -> w.getCompany().getId().equals(request.companyId()))
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado nesta empresa."));
        BulkMeasurementMethod method = parseMethod(request.method());

        BulkMeasurement m = new BulkMeasurement();
        m.setCompany(warehouse.getCompany());
        m.setProduct(product);
        m.setWarehouse(warehouse);
        m.setMethod(method);
        if (method == BulkMeasurementMethod.DIRECT) {
            if (request.quantity() == null || request.quantity().signum() < 0) {
                throw new BusinessRuleException("Indique a quantidade medida (zero ou mais).");
            }
            m.setMeasuredQuantity(request.quantity().setScale(3, RoundingMode.HALF_UP));
        } else {
            BigDecimal volume = BulkMeasurementRules.volume(method, request.dimension1(), request.dimension2(), request.dimension3());
            m.setDimension1(request.dimension1());
            m.setDimension2(request.dimension2());
            m.setDimension3(method.dimensions() == 3 ? request.dimension3() : null);
            m.setVolumeM3(volume);
            m.setMeasuredQuantity(toBase(request.companyId(), product, volume));
        }
        BigDecimal system = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
        BigDecimal pending = pickupService.pendingQuantity(request.companyId(), product.getId(), warehouse.getId());
        BigDecimal difference = BulkMeasurementRules.difference(m.getMeasuredQuantity(), pending, system)
                .setScale(3, RoundingMode.HALF_UP);
        if (system.add(difference).signum() < 0) {
            throw new BusinessRuleException("O monte tem menos material do que o já vendido por levantar ("
                    + DisplayNumbers.qty(pending) + "). Meça de novo; se faltar mesmo material, os clientes com saldo têm de ser avisados.");
        }
        m.setSystemQuantity(system);
        m.setPendingPickup(pending);
        m.setDifference(difference);
        m.setDifferencePercent(BulkMeasurementRules.differencePercent(difference, system));
        m.setTolerancePercent(setting.getTolerancePercent());
        m.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        m.setStatus(BulkMeasurementStatus.PENDING_APPROVAL);
        return m;
    }

    private BigDecimal toBase(Long companyId, Product product, BigDecimal volumeM3) {
        String base = ProductUnitService.baseUnitCodeOf(product);
        if (CUBIC_METRE.equals(base)) return volumeM3;
        UnitConversionDTO factor;
        try {
            factor = productUnitService.conversionFactor(companyId, product.getId(), CUBIC_METRE);
        } catch (BusinessRuleException ex) {
            throw new BusinessRuleException("Para medir " + product.getName() + " pelo volume do monte, defina em "
                    + "Unidades do produto quanto vale 1 metro cúbico na unidade base (" + base.toLowerCase(Locale.ROOT)
                    + "), ou use a quantidade directa.");
        }
        int decimals = factor.quantity().intValue();
        return volumeM3.multiply(factor.factorToBase()).setScale(decimals, RoundingMode.HALF_UP).setScale(3, RoundingMode.HALF_UP);
    }

    private void post(BulkMeasurement m, String reason) {
        StockMovement movement = movementService.registerMovement(m.getProduct(), m.getWarehouse(), m.getDifference(),
                StockMovementType.ADJUSTMENT.name(), null, null,
                "Medição de granel n.º " + m.getId() + " (" + reason + "): medido " + DisplayNumbers.qty(m.getMeasuredQuantity())
                        + ", por levantar " + DisplayNumbers.qty(m.getPendingPickup()) + ", sistema "
                        + DisplayNumbers.qty(m.getSystemQuantity()), null);
        if (movement != null) m.setStockMovementId(movement.getId());
        measurementRepository.save(m);
    }

    private BulkMeasurement requirePending(Long companyId, Long id) {
        BulkMeasurement m = measurementRepository.lockByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Medição não encontrada nesta empresa."));
        if (m.getStatus() != BulkMeasurementStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Esta medição já foi decidida (" + m.getStatus().label().toLowerCase(Locale.ROOT) + ").");
        }
        return m;
    }

    private static void markReviewed(BulkMeasurement m, String rejection) {
        m.setReviewedBy(CurrentUserContext.getUsername());
        m.setReviewedAt(LocalDateTime.now());
        m.setRejectionReason(rejection);
    }

    private Product requireProduct(Long companyId, Long productId) {
        return productRepository.findByIdAndCompaniesId(productId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado nesta empresa."));
    }

    private static BulkMeasurementMethod parseMethod(String raw) {
        try {
            return BulkMeasurementMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new BusinessRuleException("Método de medição desconhecido: " + raw);
        }
    }

    private static String describe(BulkMeasurement m) {
        return "Granel " + m.getProduct().getSku() + " em " + m.getWarehouse().getName() + ": diferença "
                + DisplayNumbers.qty(m.getDifference()) + " (" + DisplayNumbers.qty(m.getDifferencePercent()) + "%), "
                + m.getStatus().label().toLowerCase(Locale.ROOT);
    }

    private static BulkStockSettingDTO toDto(BulkStockSetting s) {
        Product p = s.getProduct();
        return new BulkStockSettingDTO(p.getId(), p.getSku(), p.getName(), ProductUnitService.baseUnitCodeOf(p), s.getTolerancePercent());
    }

    private static BulkMeasurementDTO toDto(BulkMeasurement m) {
        boolean within = BulkMeasurementRules.withinTolerance(m.getDifferencePercent(), m.getTolerancePercent());
        return new BulkMeasurementDTO(m.getId(), m.getProduct().getId(), m.getProduct().getName(),
                ProductUnitService.baseUnitCodeOf(m.getProduct()), m.getWarehouse().getId(), m.getWarehouse().getName(),
                m.getMethod().name(), m.getMethod().label(), m.getVolumeM3(), m.getMeasuredQuantity(), m.getPendingPickup(),
                m.getSystemQuantity(), m.getDifference(), m.getDifferencePercent(), m.getTolerancePercent(), within,
                m.getStatus().name(), m.getStatus().label(), m.getNotes(), m.getCreatedBy(), m.getCreatedAt(),
                m.getReviewedBy(), m.getRejectionReason());
    }
}
