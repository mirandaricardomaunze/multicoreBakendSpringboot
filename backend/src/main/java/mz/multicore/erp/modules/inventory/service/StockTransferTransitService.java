package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.StockTransferLineBatch;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stock em trânsito: transferência em 2 passos (despacho na origem, recepção no destino).
 * Ver {@code docs/STOCK_IN_TRANSIT_SPEC.md}.
 *
 * <p>Invariante: {@code saída da origem = entrada no destino + em trânsito}. O despacho só tira da
 * origem; a recepção só põe no destino. Colaborador separado de {@link StockTransferService}, que
 * apenas o invoca na aprovação de guias com {@code requiresReceipt}.
 */
@Service
public class StockTransferTransitService {

    private final StockTransferRepository transferRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductBatchService productBatchService;
    private final AuditLogService auditLogService;

    public StockTransferTransitService(
            StockTransferRepository transferRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            ProductBatchService productBatchService,
            AuditLogService auditLogService) {
        this.transferRepository = transferRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productBatchService = productBatchService;
        this.auditLogService = auditLogService;
    }

    /**
     * Retira o stock da origem (FEFO) e põe a guia {@code IN_TRANSIT}. Não toca no destino.
     * Chamado pela aprovação; a validação de papel e de estado é feita por quem chama.
     */
    @Transactional
    public void dispatch(StockTransfer transfer, String dispatcher) {
        Warehouse origin = transfer.getOriginWarehouse();
        for (StockTransferLine line : transfer.getLines()) {
            Product product = line.getProduct();
            BigDecimal quantity = line.getQuantity();

            productBatchService.materialiseLegacyIfNeeded(product, origin,
                    stockRepository.findByProductIdAndWarehouseId(product.getId(), origin.getId())
                            .map(Stock::getQuantity)
                            .orElse(null));
            List<ProductBatchService.BatchConsumption> debits =
                    productBatchService.consumeFEFO(product, origin, quantity);

            line.getTransitBatches().clear();
            StringBuilder summary = new StringBuilder();
            for (ProductBatchService.BatchConsumption d : debits) {
                ProductBatch source = d.batch();
                StockTransferLineBatch parcel = new StockTransferLineBatch();
                parcel.setLine(line);
                parcel.setBatchNumber(source.getBatchNumber());
                parcel.setExpirationDate(source.getExpirationDate());
                parcel.setQuantity(d.quantity());
                line.getTransitBatches().add(parcel);

                saveMovement(product, origin, d.quantity().negate(), source, transfer, "saída");
                if (summary.length() > 0) summary.append(", ");
                summary.append(source.getBatchNumber());
            }
            adjustStock(product, origin, quantity.negate());
            line.setBatchNumber(summary.toString());
        }

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setDispatchedAt(LocalDateTime.now());
        transfer.setDispatchedBy(dispatcher);
        auditLogService.logCurrent("STOCK_TRANSFER_DISPATCH",
                "Guia " + transfer.getTransferNumber() + " despachada: stock em trânsito de '"
                        + origin.getName() + "' para '" + transfer.getDestinationWarehouse().getName() + "'.");
    }

    /** O armazém de destino confirma a recepção integral: só agora o stock entra no destino. */
    @Transactional
    public StockTransferDTO receive(Long id, String notes) {
        return receive(id, notes, List.of());
    }

    /**
     * Recepção conferida no destino. Linhas não indicadas são recebidas na íntegra. Para cada linha,
     * o recebido entra no destino consumindo as parcelas pela ordem do despacho (FEFO); a diferença
     * volta ao armazém de origem com o mesmo lote e validade. Ver
     * {@code docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md}.
     */
    @Transactional
    public StockTransferDTO receive(Long id, String notes, List<ReceiveStockTransferRequest.ReceiveLine> receiptLines) {
        PermissionGuard.requireManagerOrAdmin("confirmar a recepção de guias de transferência");
        StockTransfer transfer = transferRepository
                .findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BusinessRuleException(
                    "Só guias em trânsito podem ter a recepção confirmada. Estado atual: "
                            + transfer.getStatus().getLabel());
        }

        Map<Long, ReceiveStockTransferRequest.ReceiveLine> declared = indexReceiptLines(transfer, receiptLines);
        Warehouse destination = transfer.getDestinationWarehouse();
        Warehouse origin = transfer.getOriginWarehouse();
        boolean anyDiscrepancy = false;
        StringBuilder discrepancySummary = new StringBuilder();

        for (StockTransferLine line : transfer.getLines()) {
            BigDecimal sent = line.getTransitBatches().stream()
                    .map(StockTransferLineBatch::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ReceiveStockTransferRequest.ReceiveLine req = line.getId() == null ? null : declared.get(line.getId());
            BigDecimal received = req == null ? sent : req.receivedQuantity();
            validateReceivedQuantity(line, sent, received, req);

            BigDecimal remaining = received;
            BigDecimal enteredDestination = BigDecimal.ZERO;
            BigDecimal returnedOrigin = BigDecimal.ZERO;
            for (StockTransferLineBatch parcel : line.getTransitBatches()) {
                BigDecimal toDestination = parcel.getQuantity().min(remaining);
                BigDecimal toOrigin = parcel.getQuantity().subtract(toDestination);
                remaining = remaining.subtract(toDestination);
                parcel.setReceivedQuantity(toDestination);

                if (toDestination.signum() > 0) {
                    ProductBatch target = productBatchService.addToBatch(line.getProduct(), destination,
                            parcel.getBatchNumber(), parcel.getExpirationDate(), toDestination);
                    saveMovement(line.getProduct(), destination, toDestination, target, transfer, "entrada");
                    enteredDestination = enteredDestination.add(toDestination);
                }
                if (toOrigin.signum() > 0) {
                    ProductBatch back = productBatchService.addToBatch(line.getProduct(), origin,
                            parcel.getBatchNumber(), parcel.getExpirationDate(), toOrigin);
                    saveMovement(line.getProduct(), origin, toOrigin, back, transfer, "devolução à origem por divergência");
                    returnedOrigin = returnedOrigin.add(toOrigin);
                }
            }
            if (enteredDestination.signum() > 0) {
                adjustStock(line.getProduct(), destination, enteredDestination);
            }
            if (returnedOrigin.signum() > 0) {
                adjustStock(line.getProduct(), origin, returnedOrigin);
                anyDiscrepancy = true;
                if (discrepancySummary.length() > 0) discrepancySummary.append("; ");
                discrepancySummary.append(line.getProduct().getName()).append(": ")
                        .append(returnedOrigin.stripTrailingZeros().toPlainString()).append(" devolvido(s) — ")
                        .append(req.discrepancyReason().trim());
            }
            line.setReceivedQuantity(enteredDestination);
            line.setReturnedQuantity(returnedOrigin);
            line.setDiscrepancyReason(returnedOrigin.signum() > 0 ? req.discrepancyReason().trim() : null);
        }

        transfer.setStatus(anyDiscrepancy ? TransferStatus.RECEIVED_WITH_DISCREPANCY : TransferStatus.RECEIVED);
        transfer.setReceivedAt(LocalDateTime.now());
        transfer.setReceivedBy(CurrentUserContext.getUsername());
        transfer.setReceiptNotes(notes == null || notes.isBlank() ? null : notes.trim());
        StockTransfer saved = transferRepository.save(transfer);
        if (anyDiscrepancy) {
            auditLogService.logCurrent("STOCK_TRANSFER_RECEIVE_DISCREPANCY",
                    "Recepção da guia " + saved.getTransferNumber() + " em '" + destination.getName()
                            + "' com divergência; diferença devolvida a '" + origin.getName() + "': "
                            + discrepancySummary);
        } else {
            auditLogService.logCurrent("STOCK_TRANSFER_RECEIVE",
                    "Recepção da guia " + saved.getTransferNumber() + " confirmada em '" + destination.getName() + "'.");
        }
        return StockTransferDtoMapper.toDTO(saved);
    }

    private Map<Long, ReceiveStockTransferRequest.ReceiveLine> indexReceiptLines(
            StockTransfer transfer, List<ReceiveStockTransferRequest.ReceiveLine> receiptLines) {
        Map<Long, ReceiveStockTransferRequest.ReceiveLine> declared = new HashMap<>();
        if (receiptLines == null || receiptLines.isEmpty()) {
            return declared;
        }
        Set<Long> lineIds = new HashSet<>();
        for (StockTransferLine line : transfer.getLines()) {
            if (line.getId() != null) lineIds.add(line.getId());
        }
        for (ReceiveStockTransferRequest.ReceiveLine r : receiptLines) {
            if (r == null || r.lineId() == null || !lineIds.contains(r.lineId())) {
                throw new BusinessRuleException("A linha indicada na recepção não pertence à guia "
                        + transfer.getTransferNumber() + ".");
            }
            if (declared.put(r.lineId(), r) != null) {
                throw new BusinessRuleException("A mesma linha foi indicada mais de uma vez na recepção.");
            }
        }
        return declared;
    }

    private static void validateReceivedQuantity(StockTransferLine line, BigDecimal sent, BigDecimal received,
                                                 ReceiveStockTransferRequest.ReceiveLine req) {
        String product = line.getProduct() != null ? line.getProduct().getName() : "artigo";
        if (received == null || received.signum() < 0) {
            throw new BusinessRuleException("A quantidade recebida de '" + product + "' não pode ser negativa.");
        }
        if (received.compareTo(sent) > 0) {
            throw new BusinessRuleException("A quantidade recebida de '" + product + "' ("
                    + received.stripTrailingZeros().toPlainString() + ") excede a enviada ("
                    + sent.stripTrailingZeros().toPlainString() + "). Excedentes registam-se por entrada de stock.");
        }
        if (received.compareTo(sent) < 0
                && (req == null || req.discrepancyReason() == null || req.discrepancyReason().isBlank())) {
            throw new BusinessRuleException("Indique o motivo da divergência em '" + product
                    + "': a diferença será devolvida ao armazém de origem.");
        }
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

    private void saveMovement(Product product, Warehouse warehouse, BigDecimal qty,
                              ProductBatch batch, StockTransfer transfer, String leg) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(qty);
        movement.setMovementType(StockMovementType.TRANSFER);
        movement.setBatchNumber(batch.getBatchNumber());
        movement.setBatch(batch);
        movement.setDescription("Transferência " + transfer.getTransferNumber()
                + " — " + transfer.getOriginWarehouse().getName()
                + " → " + transfer.getDestinationWarehouse().getName() + " (" + leg + ")");
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(CurrentUserContext.getUsername());
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
}
