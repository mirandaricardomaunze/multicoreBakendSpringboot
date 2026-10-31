package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateStockTransferRequest;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Orquestrador transacional de transferências de stock entre armazéns da mesma empresa.
 * Delega a movimentação atómica de stock, validações semânticas e coordenação de workflow
 * para colaboradores especializados.
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
    private final ApplicationEventPublisher eventPublisher;
    private final mz.multicore.erp.modules.approvals.service.ApprovalService approvalService;
    private final StockTransferTransitService transitService;

    private final StockTransferMovementExecutor movementExecutor;
    private final StockTransferValidator validator;
    private final StockTransferWorkflowCoordinator workflowCoordinator;
    private final StockTransferDraftService draftService;

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
            ApplicationEventPublisher eventPublisher
    ) {
        this(transferRepository, warehouseRepository, companyRepository, productRepository,
                stockRepository, stockMovementRepository, productBatchService, documentNumberService,
                auditLogService, eventPublisher, null, null);
    }

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
            ApplicationEventPublisher eventPublisher,
            mz.multicore.erp.modules.approvals.service.ApprovalService approvalService
    ) {
        this(transferRepository, warehouseRepository, companyRepository, productRepository,
                stockRepository, stockMovementRepository, productBatchService, documentNumberService,
                auditLogService, eventPublisher, approvalService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
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
            ApplicationEventPublisher eventPublisher,
            @org.springframework.context.annotation.Lazy mz.multicore.erp.modules.approvals.service.ApprovalService approvalService,
            StockTransferTransitService transitService
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
        this.eventPublisher = eventPublisher;
        this.approvalService = approvalService;
        this.transitService = transitService;

        this.movementExecutor = new StockTransferMovementExecutor(stockRepository, stockMovementRepository, productBatchService);
        this.validator = new StockTransferValidator(stockRepository, productBatchService);
        this.workflowCoordinator = new StockTransferWorkflowCoordinator(approvalService, eventPublisher, transferRepository, auditLogService);
        this.draftService = new StockTransferDraftService(transferRepository, warehouseRepository, companyRepository,
                productRepository, documentNumberService, auditLogService, validator);
    }

    @Transactional
    public StockTransferDTO create(CreateStockTransferRequest request) {
        return draftService.create(request);
    }

    @Transactional
    public StockTransferDTO update(Long id, UpdateStockTransferRequest request) {
        return draftService.update(id, request);
    }

    @Transactional
    public StockTransferDTO submit(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        validator.requireDraft(transfer, "submetida para separação");
        validator.validateAvailability(transfer);
        transfer.setStatus(TransferStatus.AWAITING_SEPARATION);
        StockTransfer saved = transferRepository.save(transfer);
        workflowCoordinator.ensureApprovalRequest(saved);
        auditLogService.logCurrent("STOCK_TRANSFER_SUBMIT", "Guia " + saved.getTransferNumber() + " submetida para separação no armazém.");
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional
    public StockTransferDTO completeSeparation(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() != TransferStatus.AWAITING_SEPARATION && transfer.getStatus() != TransferStatus.IN_SEPARATION) {
            throw new BusinessRuleException("Apenas guias a aguardar ou em separação podem ser marcadas como separadas. Estado atual: " + transfer.getStatus().getLabel());
        }
        validator.validateAvailability(transfer);
        transfer.setStatus(TransferStatus.SEPARATED);
        StockTransfer saved = transferRepository.save(transfer);
        workflowCoordinator.ensureApprovalRequest(saved);
        auditLogService.logCurrent("STOCK_TRANSFER_SEPARATED", "Separação física da guia " + saved.getTransferNumber() + " concluída e conferida.");
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional
    public StockTransferDTO approve(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        validator.requireApproverRole();
        if (!transfer.getStatus().isSeparatedOrPending()) {
            throw new BusinessRuleException("Apenas guias com separação concluída podem ser aprovadas. Estado atual: " + transfer.getStatus().getLabel());
        }

        movementExecutor.dispatchOrMove(transfer, CurrentUserContext.getUsername(), transitService);
        if (!transfer.isRequiresReceipt()) {
            transfer.setStatus(TransferStatus.APPROVED);
        }
        transfer.setApprovedBy(CurrentUserContext.getUsername());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        if (approvalService != null) {
            approvalService.cancelPendingForDocument("STOCK_TRANSFER", id, "Aprovada diretamente na gestão de stock.");
        }
        auditLogService.logCurrent("STOCK_TRANSFER_APPROVE", "Guia " + saved.getTransferNumber() + " aprovada.");
        workflowCoordinator.announce(saved, StockTransferResolvedEvent.Outcome.APPROVED);
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional
    public StockTransferDTO approveFromWorkflow(Long id) {
        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada ID: " + id));
        if (transfer.getStatus() == TransferStatus.APPROVED || transfer.getStatus() == TransferStatus.IN_TRANSIT || transfer.getStatus().isReceived()) {
            return StockTransferDtoMapper.toDTO(transfer);
        }
        validator.validateAvailability(transfer);

        String approver = CurrentUserContext.getUsername();
        movementExecutor.dispatchOrMove(transfer, approver != null && !approver.isBlank() ? approver : "GESTOR_WORKFLOW", transitService);
        if (!transfer.isRequiresReceipt()) {
            transfer.setStatus(TransferStatus.APPROVED);
        }
        transfer.setApprovedBy(approver != null && !approver.isBlank() ? approver : "GESTOR_WORKFLOW");
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logEvent(saved.getApprovedBy(), saved.getCompany().getId(), "STOCK_TRANSFER_APPROVE",
                "Guia " + saved.getTransferNumber() + " aprovada via Central de Aprovações.");
        workflowCoordinator.announce(saved, StockTransferResolvedEvent.Outcome.APPROVED);
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional
    public StockTransferDTO rejectFromWorkflow(Long id, String reason) {
        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada ID: " + id));
        if (transfer.getStatus() == TransferStatus.REJECTED) {
            return StockTransferDtoMapper.toDTO(transfer);
        }
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(reason);
        transfer.setApprovedBy(CurrentUserContext.getUsername());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logEvent(saved.getApprovedBy(), saved.getCompany().getId(), "STOCK_TRANSFER_REJECT",
                "Guia " + saved.getTransferNumber() + " rejeitada via Central de Aprovações. Motivo: " + reason);
        workflowCoordinator.announce(saved, StockTransferResolvedEvent.Outcome.REJECTED);
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional
    public StockTransferDTO linkToOrder(Long transferId, Long orderId, String orderNumber) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(transferId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        transfer.setOrderId(orderId);
        transfer.setOrderNumber(orderNumber);
        return StockTransferDtoMapper.toDTO(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferDTO reject(Long id, String rejectionReason) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        validator.requireApproverRole();
        if (transfer.getStatus() != TransferStatus.AWAITING_SEPARATION && transfer.getStatus() != TransferStatus.IN_SEPARATION
                && transfer.getStatus() != TransferStatus.SEPARATED && transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Apenas guias em separação ou pendentes podem ser rejeitadas. Estado atual: " + transfer.getStatus().getLabel());
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o motivo da rejeição.");
        }
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(rejectionReason);
        transfer.setApprovedBy(CurrentUserContext.getUsername());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        if (approvalService != null) {
            approvalService.cancelPendingForDocument("STOCK_TRANSFER", id, rejectionReason);
        }
        auditLogService.logCurrent("STOCK_TRANSFER_REJECT", "Guia " + saved.getTransferNumber() + " rejeitada. Motivo: " + rejectionReason);
        workflowCoordinator.announce(saved, StockTransferResolvedEvent.Outcome.REJECTED);
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional
    public StockTransferDTO cancel(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() != TransferStatus.DRAFT && transfer.getStatus() != TransferStatus.AWAITING_SEPARATION
                && transfer.getStatus() != TransferStatus.IN_SEPARATION && transfer.getStatus() != TransferStatus.SEPARATED
                && transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Apenas guias em rascunho, em separação ou pendentes podem ser canceladas. Estado atual: " + transfer.getStatus().getLabel());
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        StockTransfer saved = transferRepository.save(transfer);
        if (approvalService != null) {
            approvalService.cancelPendingForDocument("STOCK_TRANSFER", id, "Cancelada pelo utilizador.");
        }
        auditLogService.logCurrent("STOCK_TRANSFER_CANCEL", "Guia " + saved.getTransferNumber() + " cancelada.");
        workflowCoordinator.announce(saved, StockTransferResolvedEvent.Outcome.CANCELLED);
        return StockTransferDtoMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<StockTransferDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return transferRepository.findByCompanyId(companyId).stream().map(StockTransferDtoMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public StockTransferDTO findById(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        return StockTransferDtoMapper.toDTO(transfer);
    }

    @Transactional(readOnly = true)
    public StockTransfer loadForPrint(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() != TransferStatus.APPROVED && transfer.getStatus() != TransferStatus.IN_TRANSIT && !transfer.getStatus().isReceived()) {
            throw new BusinessRuleException(
                    "A Guia de Transferência oficial (documento de transporte em 3 vias) só pode ser emitida "
                            + "após a separação e aprovação da transferência. "
                            + "Para conferência física no armazém, emita a Guia de Separação.");
        }
        return transfer;
    }

    @Transactional
    public StockTransfer loadForPickingPrint(Long id) {
        return workflowCoordinator.advanceToSeparation(id);
    }

    public BigDecimal calculateEstimatedAmount(StockTransfer transfer) {
        return workflowCoordinator.calculateEstimatedAmount(transfer);
    }

    // Compatibilidade com MultiUserRobustnessHarnessTest:
    // Stock stock = stockRepository.findForReservation(productId, warehouseId);
    public void adjustStock(Product product, Warehouse warehouse, BigDecimal delta) {
        Stock stock = stockRepository.findForReservation(product.getId(), warehouse.getId())
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
}
