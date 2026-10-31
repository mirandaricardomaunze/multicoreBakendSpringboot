package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;

import java.time.LocalDateTime;
import java.util.Objects;

/** Criação e edição optimista dos rascunhos de transferência. */
public class StockTransferDraftService {
    private final StockTransferRepository transfers;
    private final WarehouseRepository warehouses;
    private final CompanyRepository companies;
    private final ProductRepository products;
    private final DocumentNumberService numbers;
    private final AuditLogService audit;
    private final StockTransferValidator validator;

    public StockTransferDraftService(StockTransferRepository transfers, WarehouseRepository warehouses,
                                     CompanyRepository companies, ProductRepository products,
                                     DocumentNumberService numbers, AuditLogService audit,
                                     StockTransferValidator validator) {
        this.transfers=transfers; this.warehouses=warehouses; this.companies=companies;
        this.products=products; this.numbers=numbers; this.audit=audit; this.validator=validator;
    }

    public StockTransferDTO create(CreateStockTransferRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        requireDifferent(request.originWarehouseId(), request.destinationWarehouseId());
        String driver=validator.requiredDriver(request.driverName(), request.responsible());
        String plate=validator.requiredPlate(request.vehiclePlate(), request.vehicle());
        Company company=companies.findById(request.companyId()).orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse origin=warehouse(request.originWarehouseId(), "origem");
        Warehouse destination=warehouse(request.destinationWarehouseId(), "destino");
        requireCompany(origin, destination, company.getId());
        StockTransfer transfer=new StockTransfer();
        transfer.setTransferNumber(numbers.next(DocumentSeries.STOCK_TRANSFER));
        transfer.setTransferDate(LocalDateTime.now()); transfer.setCompany(company);
        populate(transfer, origin, destination, driver, plate, request.responsible(), request.vehicle(),
                request.notes(), request.requiresReceipt(), request.lines(), request.companyId());
        transfer.setStatus(TransferStatus.DRAFT); transfer.setCreatedBy(CurrentUserContext.getUsername());
        validator.validateAvailability(transfer);
        StockTransfer saved=transfers.save(transfer);
        audit.logCurrent("STOCK_TRANSFER_DRAFT_CREATE", "Rascunho da guia " + saved.getTransferNumber() + " criado.");
        return StockTransferDtoMapper.toDTO(saved);
    }

    public StockTransferDTO update(Long id, UpdateStockTransferRequest request) {
        Long companyId=CurrentUserContext.getCurrentCompanyId();
        StockTransfer transfer=transfers.findByIdWithLinesAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        validator.requireDraft(transfer, "actualizada");
        if (!Objects.equals(transfer.getVersion(), request.version())) throw new BusinessRuleException("Esta transferência foi alterada por outro utilizador. Actualize a lista e tente novamente.");
        requireDifferent(request.originWarehouseId(), request.destinationWarehouseId());
        Warehouse origin=warehouse(request.originWarehouseId(), "origem");
        Warehouse destination=warehouse(request.destinationWarehouseId(), "destino");
        requireCompany(origin, destination, companyId);
        String driver=validator.requiredDriver(request.driverName(), request.responsible());
        String plate=validator.requiredPlate(request.vehiclePlate(), request.vehicle());
        transfer.getLines().clear();
        populate(transfer, origin, destination, driver, plate, request.responsible(), request.vehicle(),
                request.notes(), request.requiresReceipt(), request.lines(), companyId);
        validator.validateAvailability(transfer);
        StockTransfer saved=transfers.saveAndFlush(transfer);
        audit.logCurrent("STOCK_TRANSFER_DRAFT_UPDATE", "Rascunho da guia " + saved.getTransferNumber() + " actualizado.");
        return StockTransferDtoMapper.toDTO(saved);
    }

    private void populate(StockTransfer transfer, Warehouse origin, Warehouse destination, String driver,
                          String plate, String responsible, String vehicle, String notes, Boolean requiresReceipt,
                          java.util.List<CreateStockTransferLineRequest> lines, Long companyId) {
        transfer.setOriginWarehouse(origin); transfer.setDestinationWarehouse(destination);
        transfer.setResponsible(textOr(responsible, driver)); transfer.setVehicle(textOr(vehicle, plate));
        transfer.setDriverName(driver); transfer.setVehiclePlate(plate); transfer.setNotes(blankToNull(notes));
        if (requiresReceipt != null) transfer.setRequiresReceipt(requiresReceipt);
        for (CreateStockTransferLineRequest request : lines) {
            Product product=products.findByIdAndCompaniesId(request.productId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + request.productId()));
            StockTransferLine line=new StockTransferLine(); line.setTransfer(transfer);
            line.setProduct(product); line.setQuantity(request.quantity()); transfer.getLines().add(line);
        }
    }
    private Warehouse warehouse(Long id,String role) { return warehouses.findById(id).orElseThrow(() -> new BusinessRuleException("Armazém de " + role + " não encontrado.")); }
    private void requireDifferent(Long origin,Long destination) { if(origin.equals(destination)) throw new BusinessRuleException("O armazém de origem e o armazém de destino devem ser diferentes."); }
    private void requireCompany(Warehouse origin,Warehouse destination,Long companyId) { if(!origin.getCompany().getId().equals(companyId)||!destination.getCompany().getId().equals(companyId)) throw new BusinessRuleException("Os armazéns devem pertencer à mesma empresa da transferência."); }
    private String textOr(String value,String fallback) { return value!=null&&!value.isBlank()?value.trim():fallback; }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
}
