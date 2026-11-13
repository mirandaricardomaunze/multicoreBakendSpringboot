package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.dto.ApproveWasteRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gestão do ciclo de vida, registo com idempotência e decisão de aprovação de quebras de stock.
 */
@Service
public class StockWasteLifecycleService {

    public static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("2500.00");

    private final StockWasteRepository wasteRepository;
    private final ProductLookup productLookup;
    private final WarehouseRepository warehouseRepository;
    private final ProductBatchRepository batchRepository;
    private final InventoryService inventoryService;
    private final CompanyLookup companyLookup;
    private final AuditLogService auditLogService;
    private final ApprovalService approvalService;

    public StockWasteLifecycleService(
            StockWasteRepository wasteRepository,
            ProductLookup productLookup,
            WarehouseRepository warehouseRepository,
            ProductBatchRepository batchRepository,
            InventoryService inventoryService,
            CompanyLookup companyLookup,
            AuditLogService auditLogService,
            ApprovalService approvalService
    ) {
        this.wasteRepository = wasteRepository;
        this.productLookup = productLookup;
        this.warehouseRepository = warehouseRepository;
        this.batchRepository = batchRepository;
        this.inventoryService = inventoryService;
        this.companyLookup = companyLookup;
        this.auditLogService = auditLogService;
        this.approvalService = approvalService;
    }

    @Transactional
    public StockWasteDTO registerWaste(CreateStockWasteRequest req) {
        if (req == null) {
            throw new BusinessRuleException("Dados de registo de quebra não fornecidos.");
        }
        if (req.companyId() == null) {
            throw new BusinessRuleException("Empresa é obrigatória.");
        }
        if (req.warehouseId() == null) {
            throw new BusinessRuleException("Armazém é obrigatório.");
        }
        if (req.productId() == null) {
            throw new BusinessRuleException("Produto é obrigatório.");
        }
        if (req.quantity() == null || req.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("A quantidade de quebra deve ser maior que zero.");
        }
        if (req.reason() == null) {
            throw new BusinessRuleException("O motivo da quebra é obrigatório.");
        }

        CurrentUserContext.requireCompany(req.companyId());
        String key = req.idempotencyKey();
        String fingerprint = key == null ? null : wasteFingerprint(req);
        if (key != null) {
            Optional<StockWaste> previous = wasteRepository.findByCompanyIdAndIdempotencyKey(req.companyId(), key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException(
                            "A chave de idempotência desta quebra já foi usada com dados diferentes.");
                }
                return StockWasteQueryService.toDTO(previous.get());
            }
        }

        Company company = companyLookup.require(req.companyId());
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado: " + req.warehouseId()));
        Product product = productLookup.require(req.productId());

        ProductBatch batch = null;
        if (req.batchId() != null) {
            batch = batchRepository.findById(req.batchId())
                    .orElseThrow(() -> new BusinessRuleException("Lote não encontrado: " + req.batchId()));
            if (!batch.getProduct().getId().equals(product.getId())) {
                throw new BusinessRuleException("O lote selecionado não pertence ao produto indicado.");
            }
        }

        BigDecimal unitCost = resolveUnitCost(product);
        BigDecimal totalCost = unitCost.multiply(req.quantity()).setScale(2, RoundingMode.HALF_UP);

        String username = CurrentUserContext.getUsername();
        String role = CurrentUserContext.getRole() != null ? CurrentUserContext.getRole().toUpperCase() : "";
        boolean isManagerOrAdmin = role.contains("ADMIN") || role.contains("MANAGER") || role.contains("GERENTE");

        StockWaste waste = new StockWaste();
        waste.setCompany(company);
        waste.setWarehouse(warehouse);
        waste.setProduct(product);
        waste.setBatch(batch);
        waste.setQuantity(req.quantity());
        waste.setUnitCost(unitCost);
        waste.setTotalCost(totalCost);
        waste.setReason(req.reason());
        waste.setNotes(req.notes());
        waste.setIdempotencyKey(key);
        waste.setRequestFingerprint(fingerprint);
        waste.setRegisteredBy(username != null && !username.isBlank() ? username : "SISTEMA");

        if (totalCost.compareTo(APPROVAL_THRESHOLD) <= 0 || isManagerOrAdmin) {
            waste.setStatus(WasteStatus.APPROVED);
            waste.setApprovedBy(waste.getRegisteredBy());
            waste.setApprovedAt(LocalDateTime.now());
        } else {
            waste.setStatus(WasteStatus.PENDING_APPROVAL);
        }

        StockWaste saved = key == null ? waste : wasteRepository.saveAndFlush(waste);
        if (waste.getStatus() == WasteStatus.APPROVED) {
            StockMovement movement = deductStockForWaste(product, warehouse, batch, req.quantity(), req.reason());
            saved.setStockMovement(movement);
        }
        saved = wasteRepository.save(saved);
        if (saved.getStatus() == WasteStatus.PENDING_APPROVAL && approvalService != null) {
            approvalService.submitRequest(
                    "STOCK_WASTE",
                    saved.getId(),
                    saved.getTotalCost(),
                    "Quebra de stock #" + saved.getId() + " - " + product.getName() + " (" + req.reason() + ")",
                    "MANAGER",
                    false
            );
        }
        if (auditLogService != null) {
            auditLogService.logEvent(saved.getRegisteredBy(), company.getId(), "STOCK_WASTE_REGISTER",
                    "Registo de quebra de stock #" + saved.getId() + " (" + product.getSku() + " - " + product.getName()
                            + "): quantidade=" + req.quantity() + ", total=" + totalCost + " MZN, motivo=" + req.reason()
                            + ", estado=" + saved.getStatus());
        }
        return StockWasteQueryService.toDTO(saved);
    }

    @Transactional
    public StockWasteDTO approveWaste(Long id, ApproveWasteRequest req) {
        StockWaste waste = wasteRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo de quebra não encontrado: " + id));

        if (waste.getStatus() != WasteStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Este registo já foi processado (estado atual: " + waste.getStatus() + ").");
        }

        String approver = CurrentUserContext.getUsername();
        if (approver == null || approver.isBlank()) {
            approver = "GESTOR";
        }

        if (req != null && req.approved()) {
            waste.setStatus(WasteStatus.APPROVED);
            waste.setApprovedBy(approver);
            waste.setApprovedAt(LocalDateTime.now());

            if (req.notes() != null && !req.notes().isBlank()) {
                String existing = waste.getNotes() != null ? waste.getNotes() + " | " : "";
                waste.setNotes(existing + "Aprovado: " + req.notes());
            }

            StockMovement movement = deductStockForWaste(
                    waste.getProduct(), waste.getWarehouse(), waste.getBatch(), waste.getQuantity(), waste.getReason());
            waste.setStockMovement(movement);
        } else {
            waste.setStatus(WasteStatus.REJECTED);
            waste.setApprovedBy(approver);
            waste.setApprovedAt(LocalDateTime.now());

            if (req != null && req.notes() != null && !req.notes().isBlank()) {
                String existing = waste.getNotes() != null ? waste.getNotes() + " | " : "";
                waste.setNotes(existing + "Rejeitado: " + req.notes());
            }
        }

        StockWaste saved = wasteRepository.save(waste);
        if (approvalService != null) {
            approvalService.cancelPendingForDocument("STOCK_WASTE", id, "Decidido diretamente na gestão de quebras.");
        }
        if (auditLogService != null) {
            String action = (req != null && req.approved()) ? "STOCK_WASTE_APPROVE" : "STOCK_WASTE_REJECT";
            auditLogService.logEvent(approver, saved.getCompany().getId(), action,
                    "Quebra de stock #" + saved.getId() + " (" + saved.getProduct().getSku() + ") "
                            + (saved.getStatus() == WasteStatus.APPROVED ? "aprovada" : "rejeitada")
                            + " por " + approver + ", quantidade=" + saved.getQuantity() + ", valor=" + saved.getTotalCost() + " MZN");
        }
        return StockWasteQueryService.toDTO(saved);
    }

    private StockMovement deductStockForWaste(
            Product product, Warehouse warehouse, ProductBatch batch, BigDecimal quantity, WasteReason reason) {
        String batchNumber = batch != null ? batch.getBatchNumber() : null;
        LocalDate expirationDate = batch != null ? batch.getExpirationDate() : null;

        return inventoryService.registerMovement(
                product,
                warehouse,
                quantity.negate(),
                "WASTE",
                batchNumber,
                null,
                "Quebra de Stock (" + reason.getDescription() + ")",
                expirationDate
        );
    }

    public static BigDecimal resolveUnitCost(Product product) {
        if (product.getPurchasePrice() != null && product.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getPurchasePrice();
        }
        if (product.getUnitPrice() != null && product.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getUnitPrice();
        }
        return BigDecimal.ZERO;
    }

    private static String wasteFingerprint(CreateStockWasteRequest request) {
        StringBuilder canonical = new StringBuilder(192);
        appendFingerprintPart(canonical, request.companyId());
        appendFingerprintPart(canonical, request.warehouseId());
        appendFingerprintPart(canonical, request.productId());
        appendFingerprintPart(canonical, request.batchId());
        appendFingerprintPart(canonical, request.quantity().stripTrailingZeros().toPlainString());
        appendFingerprintPart(canonical, request.reason());
        appendFingerprintPart(canonical, request.notes());
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }

    private static void appendFingerprintPart(StringBuilder target, Object value) {
        String text = value == null ? "<null>" : value.toString().trim();
        target.append(text.length()).append(':').append(text).append('|');
    }
}
