package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
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
import com.phcpro.modules.inventory.model.StockTransfer;
import com.phcpro.modules.inventory.model.StockTransferLine;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.StockMovementRepository;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.repository.StockTransferRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates stock transfers between warehouses of the same company.
 * Atomic: a transfer either fully moves every line (consuming origin FEFO,
 * mirroring batch/expiration into destination, logging movements) or
 * nothing — rolling back via the transaction on any rule violation.
 */
@Service
public class StockTransferService {

    private static final DateTimeFormatter NUMBER_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final StockTransferRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductBatchService productBatchService;

    public StockTransferService(
            StockTransferRepository transferRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            ProductBatchService productBatchService
    ) {
        this.transferRepository = transferRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productBatchService = productBatchService;
    }

    @Transactional
    public StockTransferDTO create(CreateStockTransferRequest request) {
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
        transfer.setStatus("COMPLETED");
        transfer.setResponsible(blankToNull(request.responsible()));
        transfer.setVehicle(blankToNull(request.vehicle()));
        transfer.setNotes(blankToNull(request.notes()));
        transfer.setCreatedBy("SYSTEM");

        for (CreateStockTransferLineRequest lineReq : request.lines()) {
            Product product = productRepository.findById(lineReq.productId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Produto não encontrado ID: " + lineReq.productId()));

            String batchSummary = moveProduct(transfer, product, origin, destination, lineReq.quantity());

            StockTransferLine line = new StockTransferLine();
            line.setTransfer(transfer);
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setBatchNumber(batchSummary);
            transfer.getLines().add(line);
        }

        transfer = transferRepository.save(transfer);
        return toDTO(transfer);
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
        movement.setMovementType("TRANSFER");
        movement.setBatchNumber(batch.getBatchNumber());
        movement.setBatch(batch);
        movement.setDescription("Transferência " + transfer.getTransferNumber()
                + " — " + transfer.getOriginWarehouse().getName()
                + " → " + transfer.getDestinationWarehouse().getName());
        movement.setMovementDate(transfer.getTransferDate());
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
        return transferRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public StockTransferDTO findById(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        return toDTO(transfer);
    }

    @Transactional(readOnly = true)
    public StockTransfer loadForPrint(Long id) {
        return transferRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
    }

    private String generateTransferNumber() {
        return "TRF-" + LocalDateTime.now().format(NUMBER_FMT);
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
                t.getStatus(),
                t.getResponsible(),
                t.getVehicle(),
                t.getNotes(),
                lineDTOs
        );
    }
}
