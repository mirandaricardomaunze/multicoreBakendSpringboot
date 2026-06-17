package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.dto.CreateStockTransferLineRequest;
import com.phcpro.modules.inventory.dto.CreateStockTransferRequest;
import com.phcpro.modules.inventory.dto.StockTransferDTO;
import com.phcpro.modules.inventory.dto.StockTransferLineDTO;
import com.phcpro.modules.inventory.model.ProductBatch;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.model.StockMovement;
import com.phcpro.modules.inventory.model.StockMovementType;
import com.phcpro.modules.inventory.model.StockTransfer;
import com.phcpro.modules.inventory.model.StockTransferLine;
import com.phcpro.modules.inventory.model.TransferStatus;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.StockMovementRepository;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.repository.StockTransferRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    public StockTransferService(
            StockTransferRepository transferRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            ProductBatchService productBatchService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
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
        // A guia nasce pendente: o stock só sai na aprovação (ver approve()).
        transfer.setStatus(TransferStatus.PENDING_APPROVAL);
        transfer.setResponsible(blankToNull(request.responsible()));
        transfer.setVehicle(blankToNull(request.vehicle()));
        transfer.setNotes(blankToNull(request.notes()));
        transfer.setCreatedBy(CurrentUserContext.getUsername());

        // Soma o pedido por produto, para validar a disponibilidade agregada na origem.
        java.util.Map<Long, BigDecimal> requestedByProduct = new java.util.HashMap<>();
        for (CreateStockTransferLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Produto não encontrado ID: " + lineReq.productId()));

            requestedByProduct.merge(product.getId(), lineReq.quantity(), BigDecimal::add);

            // Não move stock — só regista a intenção. O lote (FEFO) é decidido na aprovação.
            StockTransferLine line = new StockTransferLine();
            line.setTransfer(transfer);
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            transfer.getLines().add(line);
        }

        // Falha cedo se já não há stock para a guia — evita criar guias que nunca poderão ser
        // aprovadas. A verificação autoritativa (FEFO) repete-se na aprovação.
        for (StockTransferLine line : transfer.getLines()) {
            Long productId = line.getProduct().getId();
            BigDecimal needed = requestedByProduct.get(productId);
            if (needed == null) continue; // já validado noutra linha do mesmo produto
            BigDecimal available = productBatchService.sumQuantity(productId, origin.getId());
            if (available == null) available = BigDecimal.ZERO;
            if (available.compareTo(needed) < 0) {
                throw new BusinessRuleException(String.format(
                        "Stock insuficiente de '%s' no armazém de origem '%s'. Requerido: %s, Disponível: %s",
                        line.getProduct().getName(), origin.getName(), needed, available));
            }
            requestedByProduct.remove(productId); // valida só uma vez por produto
        }

        transfer = transferRepository.save(transfer);
        return toDTO(transfer);
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
        return toDTO(saved);
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
        return toDTO(saved);
    }

    /** Cancela uma guia ainda pendente (sem efeito no stock). */
    @Transactional
    public StockTransferDTO cancel(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() == TransferStatus.APPROVED) {
            throw new BusinessRuleException("Guias já aprovadas não podem ser canceladas — o stock já foi movido.");
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logCurrent("STOCK_TRANSFER_CANCEL",
                "Guia " + saved.getTransferNumber() + " cancelada.");
        return toDTO(saved);
    }

    private void requireApproverRole() {
        PermissionGuard.requireManagerOrAdmin("aprovar ou rejeitar guias de transferência");
    }

    /**
     * Consume FEFO from origin and replicate the same batches in destination.
     * Returns a comma-separated batch summary for display on the transfer line.
     */
    private String moveProduct(StockTransfer transfer, Product product, Warehouse origin,
                                Warehouse destination, BigDecimal quantity) {
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
                .map(l -> new StockTransferLineDTO(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getBatchNumber()
                )).toList();
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
                lineDTOs
        );
    }
}
