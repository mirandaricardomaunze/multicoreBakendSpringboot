package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StockWasteService {

    public static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("2500.00");

    private final StockWasteRepository wasteRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductBatchRepository batchRepository;
    private final InventoryService inventoryService;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    public StockWasteService(
            StockWasteRepository wasteRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProductBatchRepository batchRepository,
            InventoryService inventoryService,
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository
    ) {
        this.wasteRepository = wasteRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.batchRepository = batchRepository;
        this.inventoryService = inventoryService;
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
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

        Company company = companyRepository.findById(req.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + req.companyId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado: " + req.warehouseId()));
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado: " + req.productId()));

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
        waste.setRegisteredBy(username != null && !username.isBlank() ? username : "SISTEMA");

        // Regra de Aprovação: <= 2.500 MZN ou registado por Gerente/Admin -> auto-aprova e abate stock
        if (totalCost.compareTo(APPROVAL_THRESHOLD) <= 0 || isManagerOrAdmin) {
            waste.setStatus(WasteStatus.APPROVED);
            waste.setApprovedBy(waste.getRegisteredBy());
            waste.setApprovedAt(LocalDateTime.now());

            StockMovement movement = deductStockForWaste(product, warehouse, batch, req.quantity(), req.reason());
            waste.setStockMovement(movement);
        } else {
            waste.setStatus(WasteStatus.PENDING_APPROVAL);
        }

        StockWaste saved = wasteRepository.save(waste);
        return toDTO(saved);
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
        return toDTO(saved);
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

    private BigDecimal resolveUnitCost(Product product) {
        if (product.getPurchasePrice() != null && product.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getPurchasePrice();
        }
        if (product.getUnitPrice() != null && product.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getUnitPrice();
        }
        return BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<StockWasteDTO> findByCompany(Long companyId) {
        return wasteRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockWasteDTO> findByCompanyAndStatus(Long companyId, WasteStatus status) {
        return wasteRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpiringBatchAlertDTO> getExpiringBatchesRadar(Long companyId, int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        List<ProductBatch> batches = batchRepository.findExpiringByCompanyId(companyId, cutoff);

        return batches.stream().map(b -> {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpirationDate());
            String alertLevel;
            if (daysUntil < 0) {
                alertLevel = "VENCIDO";
            } else if (daysUntil <= 3) {
                alertLevel = "CRÍTICO";
            } else if (daysUntil <= 7) {
                alertLevel = "ALTO";
            } else if (daysUntil <= 15) {
                alertLevel = "MÉDIO";
            } else {
                alertLevel = "ATENÇÃO";
            }

            BigDecimal unitCost = resolveUnitCost(b.getProduct());
            BigDecimal potentialLoss = unitCost.multiply(b.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            String category = b.getProduct().getCategory() != null ? b.getProduct().getCategory().getName() : "Geral";

            return new ExpiringBatchAlertDTO(
                    b.getId(),
                    b.getBatchNumber(),
                    b.getProduct().getId(),
                    b.getProduct().getSku(),
                    b.getProduct().getName(),
                    category,
                    b.getWarehouse().getId(),
                    b.getWarehouse().getName(),
                    b.getQuantity(),
                    unitCost,
                    potentialLoss,
                    b.getExpirationDate(),
                    daysUntil,
                    alertLevel
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public WasteSummaryDTO getSummary(Long companyId, LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        List<StockWaste> records = wasteRepository.findByCompanyIdAndPeriod(companyId, startDt, endDt);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        long totalCount = 0;

        Map<WasteReason, BigDecimal> costByReason = new EnumMap<>(WasteReason.class);
        for (WasteReason r : WasteReason.values()) {
            costByReason.put(r, BigDecimal.ZERO);
        }

        Map<String, BigDecimal> costByCategory = new HashMap<>();

        for (StockWaste w : records) {
            if (w.getStatus() == WasteStatus.APPROVED) {
                totalCost = totalCost.add(w.getTotalCost());
                totalQty = totalQty.add(w.getQuantity());
                totalCount++;

                costByReason.merge(w.getReason(), w.getTotalCost(), BigDecimal::add);

                String catName = (w.getProduct() != null && w.getProduct().getCategory() != null)
                        ? w.getProduct().getCategory().getName()
                        : "Outro";
                costByCategory.merge(catName, w.getTotalCost(), BigDecimal::add);
            }
        }

        // Faturação no período para apurar rácio de quebra sobre vendas
        List<Invoice> invoices = invoiceRepository.findByCompanyIdAndCreatedAtBetween(companyId, startDt, endDt);
        BigDecimal totalRevenue = invoices.stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal wasteRate = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            wasteRate = totalCost.multiply(new BigDecimal("100"))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        return new WasteSummaryDTO(
                totalCost,
                totalQty,
                totalCount,
                totalRevenue,
                wasteRate,
                costByReason,
                costByCategory
        );
    }

    public StockWasteDTO toDTO(StockWaste w) {
        return new StockWasteDTO(
                w.getId(),
                w.getCompany() != null ? w.getCompany().getId() : null,
                w.getWarehouse() != null ? w.getWarehouse().getId() : null,
                w.getWarehouse() != null ? w.getWarehouse().getName() : "-",
                w.getProduct() != null ? w.getProduct().getId() : null,
                w.getProduct() != null ? w.getProduct().getSku() : "-",
                w.getProduct() != null ? w.getProduct().getName() : "-",
                w.getBatch() != null ? w.getBatch().getId() : null,
                w.getBatch() != null ? w.getBatch().getBatchNumber() : null,
                w.getQuantity(),
                w.getUnitCost(),
                w.getTotalCost(),
                w.getReason(),
                w.getStatus(),
                w.getNotes(),
                w.getRegisteredBy(),
                w.getApprovedBy(),
                w.getCreatedAt(),
                w.getApprovedAt(),
                w.getStockMovement() != null ? w.getStockMovement().getId() : null
        );
    }
}
