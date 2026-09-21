package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.InventoryPhysicalItemRepository;
import mz.multicore.erp.modules.inventory.repository.InventoryPhysicalSessionRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryPhysicalCountingService {

    private final InventoryPhysicalSessionRepository sessionRepository;
    private final InventoryPhysicalItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    public InventoryPhysicalCountingService(
            InventoryPhysicalSessionRepository sessionRepository,
            InventoryPhysicalItemRepository itemRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService
    ) {
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public InventorySessionDTO createSession(CreateInventorySessionRequest request, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        if (sessionRepository.existsByCompanyIdAndStatus(companyId, InventoryStatus.IN_PROGRESS)) {
            throw new BusinessRuleException("Já existe uma sessão de inventário físico em curso. Conclua ou cancele a sessão activa antes de iniciar outra.");
        }

        long count = sessionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).size() + 1;
        String invNumber = String.format("INV-%d/%03d", LocalDateTime.now().getYear(), count);

        InventoryPhysicalSession session = new InventoryPhysicalSession();
        session.setInventoryNumber(invNumber);
        session.setDescription(request.description() != null && !request.description().isBlank()
                ? request.description().trim()
                : "Inventário Físico de Stock");
        session.setBlindCounting(request.blindCounting());
        session.setStatus(InventoryStatus.DRAFT);
        session.setStartDate(LocalDateTime.now());
        session.setCompanyId(companyId);

        List<Product> products = productRepository.findDistinctByCompaniesIdOrderByName(companyId).stream()
                .filter(Product::isStockTracked)
                .toList();

        if (request.categoryFilter() != null && !request.categoryFilter().isBlank()) {
            String cat = request.categoryFilter().trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getName() != null && p.getCategory().getName().toLowerCase().contains(cat))
                    .toList();
        }

        if (products.isEmpty()) {
            throw new BusinessRuleException("Nenhum produto activo com controlo de stock encontrado para o filtro seleccionado.");
        }

        for (Product product : products) {
            BigDecimal currentStock = calculateTotalStock(product.getId());
            InventoryPhysicalItem item = new InventoryPhysicalItem();
            item.setSession(session);
            item.setProduct(product);
            item.setProductCode(product.getSku());
            item.setProductName(product.getName());
            item.setBarcode(product.getBarcode());
            item.setExpectedQuantity(currentStock);
            item.setCountedQuantity(BigDecimal.ZERO);
            item.setUnitCost(product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
            item.recalculate();
            session.getItems().add(item);
        }

        session.recalculateTotals();
        InventoryPhysicalSession saved = sessionRepository.save(session);
        return toDTO(saved, false);
    }

    @Transactional
    public InventorySessionDTO startCounting(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        InventoryPhysicalSession session = getSessionEntity(sessionId, companyId);
        if (session.getStatus() != InventoryStatus.DRAFT) {
            throw new BusinessRuleException("Apenas inventários em estado de Rascunho podem ser iniciados.");
        }
        session.setStatus(InventoryStatus.IN_PROGRESS);
        return toDTO(sessionRepository.save(session), false);
    }

    @Transactional
    public InventorySessionDTO recordCount(Long sessionId, UpdateInventoryItemCountRequest request, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        InventoryPhysicalSession session = getSessionEntity(sessionId, companyId);
        if (session.getStatus() == InventoryStatus.CLOSED || session.getStatus() == InventoryStatus.CANCELLED) {
            throw new BusinessRuleException("Não é possível alterar a contagem de uma sessão já encerrada ou cancelada.");
        }

        InventoryPhysicalItem item = null;
        if (request.itemId() != null) {
            item = itemRepository.findById(request.itemId()).orElse(null);
        } else if (request.barcode() != null && !request.barcode().isBlank()) {
            item = itemRepository.findBySessionIdAndBarcode(sessionId, request.barcode().trim()).orElse(null);
        }

        if (item == null) {
            throw new BusinessRuleException("Item de inventário não encontrado para atualização.");
        }

        BigDecimal newCount = request.countedQuantity() != null ? request.countedQuantity() : BigDecimal.ZERO;
        if (request.incrementMode()) {
            BigDecimal current = item.getCountedQuantity() != null ? item.getCountedQuantity() : BigDecimal.ZERO;
            newCount = current.add(newCount.compareTo(BigDecimal.ZERO) > 0 ? newCount : BigDecimal.ONE);
        }

        if (newCount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("A quantidade contada não pode ser negativa.");
        }

        item.setCountedQuantity(newCount);
        if (request.notes() != null) {
            item.setNotes(request.notes().trim());
        }
        item.recalculate();
        itemRepository.save(item);

        session.recalculateTotals();
        return toDTO(sessionRepository.save(session), false);
    }

    @Transactional
    public InventorySessionDTO closeAndAdjustStock(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        InventoryPhysicalSession session = getSessionEntity(sessionId, companyId);
        if (session.getStatus() == InventoryStatus.CLOSED) {
            throw new BusinessRuleException("Esta sessão de inventário já se encontra encerrada.");
        }
        if (session.getStatus() == InventoryStatus.CANCELLED) {
            throw new BusinessRuleException("Sessões canceladas não podem ser encerradas.");
        }

        List<Warehouse> warehouses = warehouseRepository.findByCompanyId(companyId);
        if (warehouses.isEmpty()) {
            throw new BusinessRuleException("Nenhum armazém encontrado para a empresa.");
        }
        Warehouse defaultWarehouse = warehouses.stream()
                .filter(Warehouse::isActive)
                .findFirst()
                .orElse(warehouses.get(0));

        session.recalculateTotals();

        // Ajustar stock para os itens com diferença
        for (InventoryPhysicalItem item : session.getItems()) {
            BigDecimal diff = item.getDifference();
            if (diff != null && diff.compareTo(BigDecimal.ZERO) != 0) {
                String desc = String.format("Ajuste de Inventário Físico (%s)", session.getInventoryNumber());
                inventoryService.registerMovement(
                        item.getProduct(),
                        defaultWarehouse,
                        diff,
                        "ADJUSTMENT",
                        null,
                        null,
                        desc
                );
            }
        }

        session.setStatus(InventoryStatus.CLOSED);
        session.setEndDate(LocalDateTime.now());
        return toDTO(sessionRepository.save(session), false);
    }

    @Transactional
    public InventorySessionDTO cancelSession(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        InventoryPhysicalSession session = getSessionEntity(sessionId, companyId);
        if (session.getStatus() == InventoryStatus.CLOSED) {
            throw new BusinessRuleException("Não é possível cancelar uma sessão de inventário já encerrada.");
        }
        session.setStatus(InventoryStatus.CANCELLED);
        session.setEndDate(LocalDateTime.now());
        return toDTO(sessionRepository.save(session), false);
    }

    @Transactional(readOnly = true)
    public InventorySessionDTO getSessionById(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        InventoryPhysicalSession session = getSessionEntity(sessionId, companyId);
        return toDTO(session, false);
    }

    @Transactional(readOnly = true)
    public List<InventorySessionDTO> listSessions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return sessionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(s -> toDTO(s, true))
                .toList();
    }

    private BigDecimal calculateTotalStock(Long productId) {
        List<Stock> stocks = stockRepository.findByProductId(productId);
        BigDecimal total = BigDecimal.ZERO;
        for (Stock s : stocks) {
            if (s.getQuantity() != null) {
                total = total.add(s.getQuantity());
            }
        }
        return total;
    }

    private InventoryPhysicalSession getSessionEntity(Long id, Long companyId) {
        return sessionRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de inventário não encontrada."));
    }

    private InventorySessionDTO toDTO(InventoryPhysicalSession session, boolean summaryOnly) {
        List<InventoryItemDTO> itemDTOs = summaryOnly ? List.of() : session.getItems().stream()
                .map(item -> new InventoryItemDTO(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductCode(),
                        item.getProductName(),
                        item.getBarcode(),
                        item.getExpectedQuantity(),
                        item.getCountedQuantity(),
                        item.getDifference(),
                        item.getUnitCost(),
                        item.getFinancialImpact(),
                        item.getNotes()
                ))
                .toList();

        return new InventorySessionDTO(
                session.getId(),
                session.getInventoryNumber(),
                session.getDescription(),
                session.getStatus(),
                session.isBlindCounting(),
                session.getStartDate(),
                session.getEndDate(),
                session.getCompanyId(),
                session.getTotalItems(),
                session.getItemsCounted(),
                session.getTotalSurplusValue(),
                session.getTotalDeficitValue(),
                session.getNetFinancialImpact(),
                itemDTOs,
                session.getNotes()
        );
    }
}
