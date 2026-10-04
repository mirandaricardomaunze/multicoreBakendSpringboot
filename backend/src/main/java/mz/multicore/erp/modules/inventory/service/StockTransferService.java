package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateStockTransferRequest;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates stock transfers between warehouses of the same company.
 * Atomic: a transfer either fully moves every line (consuming origin FEFO,
 * mirroring batch/expiration into destination, logging movements) or
 * nothing — rolling back via the transaction on any rule violation.
 */
@Service
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductBatchService productBatchService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public StockTransferService(
            StockTransferRepository transferRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            ProductBatchService productBatchService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
        this.transferRepository = transferRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productBatchService = productBatchService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StockTransferDTO create(CreateStockTransferRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        if (request.originWarehouseId().equals(request.destinationWarehouseId())) {
            throw new BusinessRuleException("O armazém de origem e o armazém de destino devem ser diferentes.");
        }

        String driver = request.driverName() != null && !request.driverName().isBlank()
                ? request.driverName().trim()
                : (request.responsible() != null && !request.responsible().isBlank() ? request.responsible().trim() : null);
        if (driver == null || driver.isBlank()) {
            throw new BusinessRuleException("O nome do motorista é obrigatório para emitir a guia de transferência.");
        }

        String plate = request.vehiclePlate() != null && !request.vehiclePlate().isBlank()
                ? request.vehiclePlate().trim()
                : (request.vehicle() != null && !request.vehicle().isBlank() ? request.vehicle().trim() : null);
        if (plate == null || plate.isBlank()) {
            throw new BusinessRuleException("A matrícula do veículo é obrigatória para emitir a guia de transferência.");
        }

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse origin = warehouseRepository.findById(request.originWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de origem não encontrado."));
        Warehouse destination = warehouseRepository.findById(request.destinationWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de destino não encontrado."));

        if (!origin.getCompany().getId().equals(company.getId())
                || !destination.getCompany().getId().equals(company.getId())) {
            throw new BusinessRuleException("Os armazéns devem pertencer à mesma empresa da transferência.");
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber(generateTransferNumber());
        transfer.setTransferDate(LocalDateTime.now());
        transfer.setCompany(company);
        transfer.setOriginWarehouse(origin);
        transfer.setDestinationWarehouse(destination);
        // O rascunho pode ser revisto; só a aprovação posterior move stock.
        transfer.setStatus(TransferStatus.DRAFT);
        transfer.setResponsible(blankToNull(request.responsible()) != null ? blankToNull(request.responsible()) : driver);
        transfer.setVehicle(blankToNull(request.vehicle()) != null ? blankToNull(request.vehicle()) : plate);
        transfer.setDriverName(driver);
        transfer.setVehiclePlate(plate);
        transfer.setNotes(blankToNull(request.notes()));
        transfer.setCreatedBy(CurrentUserContext.getUsername());

        for (CreateStockTransferLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Produto não encontrado ID: " + lineReq.productId()));
            // Não move stock — só regista a intenção. O lote (FEFO) é decidido na aprovação.
            StockTransferLine line = new StockTransferLine();
            line.setTransfer(transfer);
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            transfer.getLines().add(line);
        }

        validateAvailability(transfer);

        transfer = transferRepository.save(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_DRAFT_CREATE",
                "Rascunho da guia " + transfer.getTransferNumber() + " criado.");
        return toDTO(transfer);
    }

    /** Substitui a fotografia completa de uma guia enquanto ela ainda é rascunho. */
    @Transactional
    public StockTransferDTO update(Long id, UpdateStockTransferRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        requireDraft(transfer, "actualizada");
        if (!Objects.equals(transfer.getVersion(), request.version())) {
            throw new BusinessRuleException(
                    "Esta transferência foi alterada por outro utilizador. Actualize a lista e tente novamente.");
        }
        if (request.originWarehouseId().equals(request.destinationWarehouseId())) {
            throw new BusinessRuleException("O armazém de origem e o armazém de destino devem ser diferentes.");
        }

        Warehouse origin = warehouseRepository.findById(request.originWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de origem não encontrado."));
        Warehouse destination = warehouseRepository.findById(request.destinationWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de destino não encontrado."));
        if (!origin.getCompany().getId().equals(companyId)
                || !destination.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("Os armazéns devem pertencer à empresa activa.");
        }

        String driver = requiredDriver(request.driverName(), request.responsible());
        String plate = requiredPlate(request.vehiclePlate(), request.vehicle());
        transfer.setOriginWarehouse(origin);
        transfer.setDestinationWarehouse(destination);
        transfer.setResponsible(blankToNull(request.responsible()) != null
                ? blankToNull(request.responsible()) : driver);
        transfer.setVehicle(blankToNull(request.vehicle()) != null ? blankToNull(request.vehicle()) : plate);
        transfer.setDriverName(driver);
        transfer.setVehiclePlate(plate);
        transfer.setNotes(blankToNull(request.notes()));
        transfer.getLines().clear();
        for (CreateStockTransferLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Produto não encontrado ID: " + lineReq.productId()));
            StockTransferLine line = new StockTransferLine();
            line.setTransfer(transfer);
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            transfer.getLines().add(line);
        }
        validateAvailability(transfer);

        StockTransfer saved = transferRepository.saveAndFlush(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_DRAFT_UPDATE",
                "Rascunho da guia " + saved.getTransferNumber() + " actualizado.");
        return toDTO(saved);
    }

    /** Fecha a edição do rascunho e encaminha-o para decisão. */
    @Transactional
    public StockTransferDTO submit(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(
                        id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        requireDraft(transfer, "submetida");
        validateAvailability(transfer);
        transfer.setStatus(TransferStatus.PENDING_APPROVAL);
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_SUBMIT",
                "Guia " + saved.getTransferNumber() + " submetida para aprovação.");
        return toDTO(saved);
    }

    /**
     * Quantidade que pode sair deste armazém — <b>a mesma pergunta que a saída autoritativa
     * responde</b> ({@code InventoryService.registerExit}).
     *
     * <p>Perguntar só aos lotes ({@code productBatchService.sumQuantity}) dava zero em stock que
     * existe: os lotes são uma subdivisão do stock, materializada <b>preguiçosamente</b> na
     * primeira saída ({@code ensureLegacyBatchIfNeeded} cria um lote LEGACY a cobrir a diferença).
     * Instalações anteriores aos lotes — e a base de demonstração — têm {@code stocks} preenchido e
     * {@code product_batches} vazio, pelo que a guia era recusada por falta de stock que lá estava.
     *
     * <p>É a forma exacta do defeito que este projecto já apanhou três vezes: a mesma regra em duas
     * portas, a responder coisas diferentes.
     */
    private BigDecimal availableForExit(Long productId, Long warehouseId) {
        BigDecimal fromBatches = productBatchService.sumQuantity(productId, warehouseId);
        final BigDecimal batches = fromBatches == null ? BigDecimal.ZERO : fromBatches;
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(stock -> stock.getQuantity().max(batches))
                .orElse(batches);
    }

    /**
     * Aprova a guia e move efetivamente o stock origem → destino (FEFO), gravando os movimentos
     * de stock {@code TRANSFER} que aparecem na rastreabilidade. Só MANAGER/ADMIN podem aprovar.
     */
    @Transactional
    public StockTransferDTO approve(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        requireApproverRole();
        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser aprovadas. Estado atual: " + transfer.getStatus().getLabel());
        }

        Warehouse origin = transfer.getOriginWarehouse();
        Warehouse destination = transfer.getDestinationWarehouse();
        for (StockTransferLine line : transfer.getLines()) {
            String batchSummary = moveProduct(transfer, line.getProduct(), origin, destination, line.getQuantity());
            line.setBatchNumber(batchSummary);
        }

        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(CurrentUserContext.getUsername());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_APPROVE",
                "Guia " + saved.getTransferNumber() + " aprovada.");
        announce(saved, StockTransferResolvedEvent.Outcome.APPROVED);
        return toDTO(saved);
    }

    /**
     * Liga a transferência à encomenda de reposição que a originou (ou que foi registada a partir
     * dela). Ver {@code docs/REPOSICAO_INTERNA_SPEC.md} §4.
     */
    @Transactional
    public StockTransferDTO linkToOrder(Long transferId, Long orderId, String orderNumber) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(
                        transferId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        transfer.setOrderId(orderId);
        transfer.setOrderNumber(orderNumber);
        // Devolve já ligada: quem converteu tem de ver a encomenda de origem na resposta, e não
        // uma fotografia tirada antes da ligação existir.
        return toDTO(transferRepository.save(transfer));
    }

    /**
     * Avisa quem esteja à espera do desfecho da transferência.
     *
     * <p>Só quando veio de uma encomenda: uma transferência feita directamente não tem nada a
     * actualizar. O inventário não conhece o comercial — ver {@link StockTransferResolvedEvent}.
     */
    private void announce(StockTransfer transfer, StockTransferResolvedEvent.Outcome outcome) {
        if (transfer.getOrderId() == null) return;
        eventPublisher.publishEvent(new StockTransferResolvedEvent(transfer.getOrderId(), outcome));
    }

    /** Rejeita uma guia pendente — não move stock. Só MANAGER/ADMIN. */
    @Transactional
    public StockTransferDTO reject(Long id, String rejectionReason) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        requireApproverRole();
        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias pendentes podem ser rejeitadas. Estado atual: " + transfer.getStatus().getLabel());
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o motivo da rejeição.");
        }
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(rejectionReason);
        transfer.setApprovedBy(CurrentUserContext.getUsername());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_REJECT",
                "Guia " + saved.getTransferNumber() + " rejeitada. Motivo: " + rejectionReason);
        announce(saved, StockTransferResolvedEvent.Outcome.REJECTED);
        return toDTO(saved);
    }

    /** Cancela um rascunho ou uma guia pendente (sem efeito no stock). */
    @Transactional
    public StockTransferDTO cancel(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() != TransferStatus.DRAFT
                && transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Apenas guias em rascunho ou pendentes podem ser canceladas. Estado atual: "
                            + transfer.getStatus().getLabel());
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_CANCEL",
                "Guia " + saved.getTransferNumber() + " cancelada.");
        announce(saved, StockTransferResolvedEvent.Outcome.CANCELLED);
        return toDTO(saved);
    }

    private void requireApproverRole() {
        PermissionGuard.requireManagerOrAdmin("aprovar ou rejeitar guias de transferência");
    }

    private void requireDraft(StockTransfer transfer, String operation) {
        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new BusinessRuleException("Apenas guias em rascunho podem ser " + operation
                    + ". Estado atual: " + transfer.getStatus().getLabel());
        }
    }

    private String requiredDriver(String driverName, String responsible) {
        String driver = blankToNull(driverName) != null ? blankToNull(driverName) : blankToNull(responsible);
        if (driver == null) {
            throw new BusinessRuleException("O nome do motorista é obrigatório para emitir a guia de transferência.");
        }
        return driver;
    }

    private String requiredPlate(String vehiclePlate, String vehicle) {
        String plate = blankToNull(vehiclePlate) != null ? blankToNull(vehiclePlate) : blankToNull(vehicle);
        if (plate == null) {
            throw new BusinessRuleException("A matrícula do veículo é obrigatória para emitir a guia de transferência.");
        }
        return plate;
    }

    /** Validação agregada por produto usada ao gravar e ao submeter o rascunho. */
    private void validateAvailability(StockTransfer transfer) {
        java.util.Map<Long, BigDecimal> requested = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Product> products = new java.util.LinkedHashMap<>();
        for (StockTransferLine line : transfer.getLines()) {
            Long productId = line.getProduct().getId();
            requested.merge(productId, line.getQuantity(), BigDecimal::add);
            products.putIfAbsent(productId, line.getProduct());
        }
        for (var entry : requested.entrySet()) {
            Product product = products.get(entry.getKey());
            BigDecimal available = availableForExit(entry.getKey(), transfer.getOriginWarehouse().getId());
            if (available.compareTo(entry.getValue()) < 0) {
                throw new BusinessRuleException(String.format(
                        "Stock insuficiente de '%s' no armazém de origem '%s'. Requerido: %s, Disponível: %s",
                        product.getName(), transfer.getOriginWarehouse().getName(), entry.getValue(), available));
            }
        }
    }

    /**
     * Consume FEFO from origin and replicate the same batches in destination.
     * Returns a comma-separated batch summary for display on the transfer line.
     */
    private String moveProduct(StockTransfer transfer, Product product, Warehouse origin,
                                Warehouse destination, BigDecimal quantity) {
        // Mesma migração preguiçosa que a saída de venda faz: stock antigo sem lote é materializado
        // antes de se consumir FEFO. Sem isto a transferência encontrava "Disponível em lotes: 0"
        // em stock que existe — e nunca funcionou sobre dados anteriores ao rastreio de lote.
        productBatchService.materialiseLegacyIfNeeded(product, origin,
                stockRepository.findByProductIdAndWarehouseId(product.getId(), origin.getId())
                        .map(Stock::getQuantity)
                        .orElse(null));

        List<ProductBatchService.BatchConsumption> debits =
                productBatchService.consumeFEFO(product, origin, quantity);

        StringBuilder summary = new StringBuilder();
        for (ProductBatchService.BatchConsumption d : debits) {
            ProductBatch sourceBatch = d.batch();
            BigDecimal moved = d.quantity();

            productBatchService.addToBatch(
                    product,
                    destination,
                    sourceBatch.getBatchNumber(),
                    sourceBatch.getExpirationDate(),
                    moved
            );

            saveMovement(product, origin, moved.negate(), sourceBatch, transfer);
            saveMovement(product, destination, moved, sourceBatch, transfer);

            if (summary.length() > 0) summary.append(", ");
            summary.append(sourceBatch.getBatchNumber());
        }

        adjustStock(product, origin, quantity.negate());
        adjustStock(product, destination, quantity);
        return summary.toString();
    }

    private void saveMovement(Product product, Warehouse warehouse, BigDecimal qty,
                               ProductBatch batch, StockTransfer transfer) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(qty);
        movement.setMovementType(StockMovementType.TRANSFER);
        movement.setBatchNumber(batch.getBatchNumber());
        movement.setBatch(batch);
        movement.setDescription("Transferência " + transfer.getTransferNumber()
                + " — " + transfer.getOriginWarehouse().getName()
                + " → " + transfer.getDestinationWarehouse().getName());
        movement.setMovementDate(transfer.getTransferDate());
        movement.setCreatedBy(transfer.getCreatedBy());
        stockMovementRepository.save(movement);
    }

    private void adjustStock(Product product, Warehouse warehouse, BigDecimal delta) {
        Stock stock = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    Stock s = new Stock();
                    s.setProduct(product);
                    s.setWarehouse(warehouse);
                    s.setQuantity(BigDecimal.ZERO);
                    return s;
                });
        stock.setQuantity(stock.getQuantity().add(delta));
        stockRepository.save(stock);
    }

    @Transactional(readOnly = true)
    public List<StockTransferDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return transferRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public StockTransferDTO findById(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        return toDTO(transfer);
    }

    @Transactional(readOnly = true)
    public StockTransfer loadForPrint(Long id) {
        return transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
    }

    private String generateTransferNumber() {
        return documentNumberService.next(DocumentSeries.STOCK_TRANSFER);
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private StockTransferDTO toDTO(StockTransfer t) {
        List<StockTransferLineDTO> lineDTOs = t.getLines().stream()
                .map(l -> {
                    var p = l.getProduct();
                    BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;
                    String ref = p != null && p.getReference() != null && !p.getReference().isBlank()
                            ? p.getReference() : (p != null ? p.getSku() : null);
                    String barcode = p != null ? p.getBarcode() : null;
                    int pkgs = p != null && p.getPackagesPerBox() > 0 ? p.getPackagesPerBox() : 1;
                    int units = p != null && p.getUnitsPerPackage() > 0 ? p.getUnitsPerPackage() : 1;
                    BigDecimal price = (p != null && p.effectiveUnitPrice(qty) != null)
                            ? p.effectiveUnitPrice(qty) : BigDecimal.ZERO;
                    BigDecimal tax = p != null ? p.effectiveTaxRate() : BigDecimal.ZERO;
                    return new StockTransferLineDTO(
                            l.getId(),
                            p != null ? p.getId() : null,
                            p != null ? p.getSku() : null,
                            ref,
                            barcode,
                            p != null ? p.getName() : null,
                            qty,
                            l.getBatchNumber(),
                            pkgs,
                            units,
                            price,
                            tax
                    );
                }).toList();
        return new StockTransferDTO(
                t.getId(),
                t.getTransferNumber(),
                t.getTransferDate(),
                t.getCompany() != null ? t.getCompany().getId() : null,
                t.getOriginWarehouse().getId(),
                t.getOriginWarehouse().getName(),
                t.getDestinationWarehouse().getId(),
                t.getDestinationWarehouse().getName(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getResponsible(),
                t.getVehicle(),
                t.getNotes(),
                t.getApprovedBy(),
                t.getApprovedAt(),
                t.getRejectionReason(),
                lineDTOs,
                t.getOrderId(),
                t.getOrderNumber(),
                t.getDriverName(),
                t.getVehiclePlate(),
                t.getVersion()
        );
    }
}
