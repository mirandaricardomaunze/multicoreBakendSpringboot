package mz.multicore.erp.modules.consignment.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.consignment.dto.*;
import mz.multicore.erp.modules.consignment.model.ConsignmentDelivery;
import mz.multicore.erp.modules.consignment.model.ConsignmentLine;
import mz.multicore.erp.modules.consignment.model.ConsignmentStatus;
import mz.multicore.erp.modules.consignment.repository.ConsignmentDeliveryRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import mz.multicore.erp.modules.inventory.service.TransitWarehouseProvider;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Consignment and route sales (SA-C11, {@code docs/CONSIGNACAO_ROTA_SPEC.md}). The bakery leaves bread at a reseller
 * in the morning (guia CG): the goods move to the "Em consignação" warehouse, because they left the shop. At the
 * settlement the good returns come back to the shop, the stale ones are written off and only what was sold is
 * invoiced to the reseller with an ordinary invoice (FT) at the reseller price list, from the consignment warehouse.
 */
@Service
public class ConsignmentService {

    static final String CONSIGNMENT_WAREHOUSE = "Em consignação";
    private static final String MOVEMENT_TYPE = "TRANSFER";

    private final ConsignmentDeliveryRepository deliveryRepository;
    private final ClientLookup clientLookup;
    private final ProductLookup productLookup;
    private final WarehouseLookup warehouseLookup;
    private final StockRepository stockRepository;
    private final InvoiceLookup invoiceLookup;
    private final StockMovementRegistrationService movementService;
    private final StockWasteService wasteService;
    private final TransitWarehouseProvider transitWarehouses;
    private final mz.multicore.erp.modules.inventory.service.InventoryService inventoryService;
    private final ComercialService comercialService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public ConsignmentService(ConsignmentDeliveryRepository deliveryRepository, ClientLookup clientLookup,
                              ProductLookup productLookup, WarehouseLookup warehouseLookup,
                              StockRepository stockRepository, InvoiceLookup invoiceLookup,
                              StockMovementRegistrationService movementService, StockWasteService wasteService,
                              TransitWarehouseProvider transitWarehouses,
                              mz.multicore.erp.modules.inventory.service.InventoryService inventoryService,
                              ComercialService comercialService, DocumentNumberService documentNumberService,
                              AuditLogService auditLogService) {
        this.deliveryRepository = deliveryRepository;
        this.clientLookup = clientLookup;
        this.productLookup = productLookup;
        this.warehouseLookup = warehouseLookup;
        this.stockRepository = stockRepository;
        this.invoiceLookup = invoiceLookup;
        this.movementService = movementService;
        this.wasteService = wasteService;
        this.transitWarehouses = transitWarehouses;
        this.inventoryService = inventoryService;
        this.comercialService = comercialService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    /** Open deliveries (whatever their date) and the ones of the last {@code days} days. */
    @Transactional(readOnly = true)
    public List<ConsignmentDTO> list(Long companyId, int days) {
        CurrentUserContext.requireCompany(companyId);
        return deliveryRepository.findRecentAndOpen(companyId, LocalDate.now().minusDays(Math.max(1, Math.min(days, 120))))
                .stream().map(ConsignmentService::toDto).toList();
    }

    @Transactional
    public ConsignmentDTO deliver(DeliverConsignmentRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("entregar em consignação");
        var existing = deliveryRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());
        Client client = clientLookup.requireForCompany(request.clientId(), request.companyId());
        if (WalkInClientProvider.WALK_IN_TAX_ID.equals(client.getTaxId())) {
            throw new BusinessRuleException("A consignação é para um revendedor identificado, não para o Consumidor Final.");
        }
        Warehouse source = warehouseLookup.requireForCompany(request.warehouseId(), request.companyId());
        Warehouse consignment = consignmentWarehouse(source);
        if (consignment.getId().equals(source.getId())) throw new BusinessRuleException("Escolha o armazém da loja, não o de consignação.");

        ConsignmentDelivery delivery = new ConsignmentDelivery();
        delivery.setCompany(source.getCompany());
        delivery.setNumber(documentNumberService.next(DocumentSeries.CONSIGNMENT));
        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setClient(client);
        delivery.setRoute(request.route() == null || request.route().isBlank() ? null : request.route().trim());
        delivery.setSourceWarehouse(source);
        delivery.setConsignmentWarehouse(consignment);
        delivery.setRequestKey(request.requestKey());
        delivery.setCreatedBy(CurrentUserContext.getUsername());
        String description = "Consignação " + delivery.getNumber() + " - " + client.getName();
        Set<Long> seen = new HashSet<>();
        for (DeliverLineRequest l : request.lines()) {
            if (!seen.add(l.productId())) throw new BusinessRuleException("Cada artigo só aparece uma vez na guia.");
            Product product = productLookup.findForCompany(l.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Artigo não encontrado (ID " + l.productId() + ")."))
                    .requireOrdinarySale();
            if (product.isStockTracked()) {
                BigDecimal stock = stockOf(product.getId(), source.getId());
                if (stock.compareTo(l.quantity()) < 0) {
                    throw new BusinessRuleException("Só há " + DisplayNumbers.qty(stock) + " de " + product.getName() + " em " + source.getName() + ".");
                }
                LocalDate expiry = oldestExpiry(product, source);
                movementService.registerMovement(product, source, l.quantity().negate(), MOVEMENT_TYPE, null, null, description, null);
                movementService.registerMovement(product, consignment, l.quantity(), MOVEMENT_TYPE, delivery.getNumber(), null, description, expiry);
            }
            ConsignmentLine line = new ConsignmentLine();
            line.setDelivery(delivery);
            line.setProduct(product);
            line.setDelivered(l.quantity());
            delivery.getLines().add(line);
        }
        delivery = deliveryRepository.save(delivery);
        auditLogService.logCurrent("CONSIGNMENT_DELIVER", "Consignação " + delivery.getNumber() + " entregue a " + client.getName()
                + " (" + delivery.getLines().size() + " artigos).");
        return toDto(delivery);
    }

    @Transactional
    public ConsignmentDTO settle(Long id, SettleConsignmentRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("acertar consignação");
        ConsignmentDelivery delivery = deliveryRepository.lockByIdAndCompanyId(id, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Guia de consignação não encontrada."));
        if (delivery.getStatus() == ConsignmentStatus.SETTLED) {
            throw new BusinessRuleException("A guia " + delivery.getNumber() + " já foi acertada.");
        }
        Map<Long, SettleLineRequest> byLine = new HashMap<>();
        request.lines().forEach(l -> byLine.put(l.lineId(), l));
        String description = "Acerto da consignação " + delivery.getNumber() + " - " + delivery.getClient().getName();
        List<CreateInvoiceLineRequest> invoiceLines = new ArrayList<>();
        for (ConsignmentLine line : delivery.getLines()) {
            SettleLineRequest s = byLine.getOrDefault(line.getId(), new SettleLineRequest(line.getId(), BigDecimal.ZERO, BigDecimal.ZERO));
            if (s.returned().add(s.damaged()).compareTo(line.getDelivered()) > 0) {
                throw new BusinessRuleException("Voltou mais " + line.getProduct().getName() + " do que foi entregue ("
                        + DisplayNumbers.qty(line.getDelivered()) + ").");
            }
            line.setReturned(s.returned());
            line.setDamaged(s.damaged());
            Product product = line.getProduct();
            if (product.isStockTracked() && s.returned().signum() > 0) {
                LocalDate expiry = oldestExpiry(product, delivery.getConsignmentWarehouse());
                movementService.registerMovement(product, delivery.getConsignmentWarehouse(), s.returned().negate(), MOVEMENT_TYPE,
                        null, null, description, null);
                movementService.registerMovement(product, delivery.getSourceWarehouse(), s.returned(), MOVEMENT_TYPE,
                        delivery.getNumber(), null, description, expiry);
            }
            if (product.isStockTracked() && s.damaged().signum() > 0) {
                wasteService.registerWaste(new CreateStockWasteRequest(request.companyId(), delivery.getConsignmentWarehouse().getId(),
                        product.getId(), null, s.damaged(), WasteReason.EXPIRED, "Sobra estragada da consignação " + delivery.getNumber(),
                        "CG-" + delivery.getId() + "-" + line.getId()));
            }
            if (line.sold().signum() > 0) {
                invoiceLines.add(new CreateInvoiceLineRequest(product.getId(), line.sold(), product.effectiveTaxRate(), BigDecimal.ZERO, null, null));
            }
        }
        if (!invoiceLines.isEmpty()) {
            InvoiceDTO invoice = comercialService.createInvoice(new CreateInvoiceRequest(delivery.getClient().getId(), request.companyId(),
                    delivery.getConsignmentWarehouse().getId(), invoiceLines));
            delivery.setInvoice(invoiceLookup.find(invoice.id()).orElseThrow());
        }
        delivery.setStatus(ConsignmentStatus.SETTLED);
        delivery.setSettledAt(LocalDateTime.now());
        delivery.setSettledBy(CurrentUserContext.getUsername());
        delivery = deliveryRepository.save(delivery);
        auditLogService.logCurrent("CONSIGNMENT_SETTLE", "Consignação " + delivery.getNumber() + " acertada"
                + (delivery.getInvoice() == null ? " sem vendas." : ": fatura " + delivery.getInvoice().getInvoiceNumber() + "."));
        return toDto(delivery);
    }

    /** The company "Em consignação" warehouse: in transit, not for sale at the POS. Created on first use. */
    private Warehouse consignmentWarehouse(Warehouse source) {
        return transitWarehouses.named(source.getCompany(), CONSIGNMENT_WAREHOUSE, "Mercadoria nos revendedores");
    }

    /**
     * Expiry of the lot that leaves first (FEFO), so perishable goods keep their date at the reseller and when they
     * come back, and the Validades screen still sees them. {@code null} for products without expiry.
     */
    private LocalDate oldestExpiry(Product product, Warehouse warehouse) {
        if (!product.isTracksExpiry()) return null;
        return inventoryService.findNextFEFO(product.getId(), warehouse.getId())
                .map(mz.multicore.erp.modules.inventory.dto.ProductBatchDTO::expirationDate).orElse(null);
    }

    private BigDecimal stockOf(Long productId, Long warehouseId) {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId).map(Stock::getQuantity).orElse(BigDecimal.ZERO);
    }

    static ConsignmentDTO toDto(ConsignmentDelivery d) {
        List<ConsignmentLineDTO> lines = d.getLines().stream().map(l -> new ConsignmentLineDTO(l.getId(), l.getProduct().getId(),
                l.getProduct().getName(), l.getDelivered(), l.getReturned(), l.getDamaged(), l.sold())).toList();
        return new ConsignmentDTO(d.getId(), d.getNumber(), d.getDeliveryDate(), d.getClient().getId(), d.getClient().getName(),
                d.getRoute(), d.getSourceWarehouse().getId(), d.getSourceWarehouse().getName(), d.getStatus(),
                d.getInvoice() == null ? null : d.getInvoice().getId(), d.getInvoice() == null ? null : d.getInvoice().getInvoiceNumber(),
                d.getInvoice() == null ? null : d.getInvoice().getTotalAmount(), d.getCreatedBy(), d.getSettledBy(), lines);
    }
}
