package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockAdjustmentOperation;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockAdjustmentOperationRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Coordenador transacional de ajustes de stock baseados em contagem física.
 */
@Service
public class StockAdjustmentCoordinator {

    private final StockAdjustmentOperationRepository stockAdjustmentOperationRepository;
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final CompanyLookup companyLookup;
    private final AuditLogService auditLogService;
    private final StockMovementRegistrationService registrationService;
    private final StockMovementQueryService queryService;

    @Autowired
    public StockAdjustmentCoordinator(
            StockAdjustmentOperationRepository stockAdjustmentOperationRepository,
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            CompanyLookup companyLookup,
            AuditLogService auditLogService,
            StockMovementRegistrationService registrationService,
            StockMovementQueryService queryService
    ) {
        this.stockAdjustmentOperationRepository = stockAdjustmentOperationRepository;
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.companyLookup = companyLookup;
        this.auditLogService = auditLogService;
        this.registrationService = registrationService;
        this.queryService = queryService;
    }

    @Transactional
    public StockMovementDTO adjustStock(CreateStockAdjustmentRequest request) {
        PermissionGuard.requireManagerOrAdmin("ajustar stock");
        CurrentUserContext.requireCompany(request.companyId());
        String key = request.idempotencyKey();
        String fingerprint = key == null ? null : adjustmentFingerprint(request);
        if (key != null) {
            var previous = stockAdjustmentOperationRepository.findByCompanyIdAndIdempotencyKey(request.companyId(), key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException("A chave de idempotência deste ajuste já foi usada com dados diferentes.");
                }
                if (previous.get().getStockMovement() == null) {
                    throw new BusinessRuleException("O ajuste anterior ainda está em processamento. Tente novamente.");
                }
                return queryService.toDTO(previous.get().getStockMovement());
            }
        }
        Product product = productRepository.findByIdAndCompaniesId(request.productId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        if (!product.isStockTracked()) {
            throw new BusinessRuleException("Este produto não controla stock físico.");
        }
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }

        StockAdjustmentOperation operation = null;
        if (key != null) {
            Company company = companyLookup.require(request.companyId());
            operation = new StockAdjustmentOperation();
            operation.setCompany(company);
            operation.setProduct(product);
            operation.setWarehouse(warehouse);
            operation.setIdempotencyKey(key);
            operation.setRequestFingerprint(fingerprint);
            operation.setCreatedBy(CurrentUserContext.getUsername());
            operation = stockAdjustmentOperationRepository.saveAndFlush(operation);
        }

        BigDecimal current = stockRepository.findForReservation(product.getId(), warehouse.getId())
                .map(Stock::getQuantity)
                .orElse(BigDecimal.ZERO);
        BigDecimal delta = request.countedQuantity().subtract(current);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("A contagem informada é igual ao stock actual. Nenhum ajuste foi necessário.");
        }

        StockMovement movement = registrationService.registerMovement(
                product,
                warehouse,
                delta,
                StockMovementType.ADJUSTMENT.name(),
                null,
                null,
                "Ajuste de stock. Motivo: " + request.reason()
                        + ". Stock anterior: " + current + "; contado: " + request.countedQuantity(),
                null
        );
        if (operation != null) {
            operation.setStockMovement(movement);
            stockAdjustmentOperationRepository.save(operation);
        }
        auditLogService.logCurrent("STOCK_ADJUSTMENT",
                "Produto " + product.getSku() + " no armazém " + warehouse.getName()
                        + ". Anterior: " + current + "; contado: " + request.countedQuantity()
                        + "; diferença: " + delta + ". Motivo: " + request.reason());
        return queryService.toDTO(movement);
    }

    private static String adjustmentFingerprint(CreateStockAdjustmentRequest request) {
        String canonical = request.companyId() + "|" + request.productId() + "|" + request.warehouseId()
                + "|" + request.countedQuantity().stripTrailingZeros().toPlainString()
                + "|" + request.reason().trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }
}
