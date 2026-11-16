package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stock em trânsito: transferência em 2 passos (despacho na origem, recepção no destino).
 * Ver {@code docs/STOCK_IN_TRANSIT_SPEC.md} e
 * {@code docs/STOCK_TRANSFER_TRANSIT_SERVICE_DECOMPOSITION_SPEC.md}.
 *
 * <p>Invariante: {@code saída da origem = entrada no destino + em trânsito}. O despacho só tira da
 * origem; a recepção só põe no destino. Colaborador separado de {@link StockTransferService}, que
 * apenas o invoca na aprovação de guias com {@code requiresReceipt}. Esta classe é uma fachada que
 * delega em {@link StockTransferDispatchService} e {@link StockTransferReceiptService}.
 */
@Service
public class StockTransferTransitService {

    private final StockTransferRepository transferRepository;
    private final StockTransferDispatchService dispatchService;
    private final StockTransferReceiptService receiptService;

    @Autowired
    public StockTransferTransitService(
            StockTransferRepository transferRepository,
            StockTransferDispatchService dispatchService,
            StockTransferReceiptService receiptService) {
        this.transferRepository = transferRepository;
        this.dispatchService = dispatchService;
        this.receiptService = receiptService;
    }

    /** Construtor retrocompatível (testes legados de trânsito e divergência). */
    public StockTransferTransitService(
            StockTransferRepository transferRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            ProductBatchService productBatchService,
            AuditLogService auditLogService) {
        this(transferRepository, new StockTransitLedger(stockRepository, stockMovementRepository),
                productBatchService, auditLogService);
    }

    private StockTransferTransitService(StockTransferRepository transferRepository, StockTransitLedger ledger,
                                        ProductBatchService productBatchService, AuditLogService auditLogService) {
        this(transferRepository,
                new StockTransferDispatchService(productBatchService, ledger, auditLogService),
                new StockTransferReceiptService(transferRepository, productBatchService, ledger, auditLogService, null));
    }

    /**
     * Retira o stock da origem (FEFO) e põe a guia {@code IN_TRANSIT}. Não toca no destino.
     * Chamado pela aprovação; a validação de papel e de estado é feita por quem chama.
     */
    @Transactional
    public void dispatch(StockTransfer transfer, String dispatcher) {
        dispatchService.dispatch(transfer, dispatcher);
    }

    /** O armazém de destino confirma a recepção integral: só agora o stock entra no destino. */
    @Transactional
    public StockTransferDTO receive(Long id, String notes) {
        return receive(id, notes, List.of());
    }

    /** Recepção conferida no destino; linhas não indicadas são recebidas na íntegra. */
    @Transactional
    public StockTransferDTO receive(Long id, String notes, List<ReceiveStockTransferRequest.ReceiveLine> receiptLines) {
        return receive(id, notes, receiptLines, null);
    }

    @Transactional
    public StockTransferDTO receive(Long id, String notes,
                                    List<ReceiveStockTransferRequest.ReceiveLine> receiptLines,
                                    String idempotencyKey) {
        return receiptService.receive(id, notes, receiptLines, idempotencyKey);
    }

    @Transactional(readOnly = true)
    public List<StockTransferDTO> findInTransit(Long companyId, Long destinationWarehouseId) {
        CurrentUserContext.requireCompany(companyId);
        return transferRepository.findByCompanyIdAndStatus(companyId, TransferStatus.IN_TRANSIT).stream()
                .filter(t -> destinationWarehouseId == null
                        || destinationWarehouseId.equals(t.getDestinationWarehouse().getId()))
                .map(StockTransferDtoMapper::toDTO)
                .toList();
    }
}
