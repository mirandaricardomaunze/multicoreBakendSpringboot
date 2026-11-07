package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.CreateStandaloneDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static mz.multicore.erp.modules.comercial.service.DeliveryGuideQueryService.blankToNull;
import static mz.multicore.erp.modules.comercial.service.DeliveryGuideQueryService.toDTO;

@Service
public class DeliveryGuideCreationService {
    private final DeliveryGuideRepository guideRepository;
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public DeliveryGuideCreationService(DeliveryGuideRepository guideRepository,
                                        OrderRepository orderRepository,
                                        ClientRepository clientRepository,
                                        ProductRepository productRepository,
                                        InventoryService inventoryService,
                                        DocumentNumberService documentNumberService,
                                        AuditLogService auditLogService) {
        this.guideRepository = guideRepository;
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DeliveryGuideDTO createStandalone(CreateStandaloneDeliveryGuideRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        var client = clientRepository.findByIdAndCompaniesId(request.clientId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado na empresa activa."));
        var warehouse = inventoryService.loadWarehouseForActiveCompany(request.warehouseId());

        if (request.responsible() == null || request.responsible().isBlank()) {
            throw new BusinessRuleException("O transportador / responsável é obrigatório para emitir a guia de remessa.");
        }
        if (request.vehicle() == null || request.vehicle().isBlank()) {
            throw new BusinessRuleException("A viatura / matrícula é obrigatória para emitir a guia de remessa.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessRuleException("Adicione pelo menos um produto à guia.");
        }

        DeliveryGuide guide = new DeliveryGuide();
        guide.setGuideNumber(documentNumberService.next(DocumentSeries.DELIVERY_GUIDE));
        guide.setGuideDate(LocalDateTime.now());
        guide.setCompany(warehouse.getCompany());
        guide.setClient(client);
        guide.setWarehouse(warehouse);
        guide.setStatus(DeliveryGuideStatus.PENDING_APPROVAL);
        guide.setResponsible(blankToNull(request.responsible()));
        guide.setVehicle(blankToNull(request.vehicle()));
        guide.setNotes(blankToNull(request.notes()));
        guide.setCreatedBy(CurrentUserContext.getUsername());

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> productIds = new HashSet<>();
        for (var requestedLine : request.lines()) {
            if (requestedLine.quantity() == null || requestedLine.quantity().signum() <= 0) {
                throw new BusinessRuleException("A quantidade de cada produto deve ser positiva.");
            }
            if (!productIds.add(requestedLine.productId())) {
                throw new BusinessRuleException("O mesmo produto não pode aparecer em mais de uma linha da guia.");
            }
            var product = productRepository.findByIdAndCompaniesId(requestedLine.productId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado na empresa activa."));
            BigDecimal unitPrice = product.getUnitPrice() == null ? BigDecimal.ZERO : product.getUnitPrice();
            BigDecimal taxRate = product.effectiveTaxRate();
            var amounts = LineCalculator.compute(unitPrice, requestedLine.quantity(), BigDecimal.ZERO, taxRate);

            DeliveryGuideLine line = new DeliveryGuideLine();
            line.setProduct(product);
            line.setQuantity(requestedLine.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);
            line.setDiscountPercentage(BigDecimal.ZERO);
            line.setLineTotal(amounts.total());
            guide.addLine(line);
            total = total.add(amounts.total());
        }
        guide.setTotalAmount(total);

        DeliveryGuide saved = guideRepository.save(guide);
        auditLogService.logCurrent("DELIVERY_GUIDE_STANDALONE_CREATE",
                "Guia de remessa avulsa " + saved.getGuideNumber() + " criada para " + client.getName() + ".");
        return toDTO(saved);
    }

    @Transactional
    public DeliveryGuideDTO createFromOrder(CreateDeliveryGuideRequest request) {
        // Bloqueada como na faturação: guia e fatura em simultâneo não podem ambas ver "por faturar".
        Order order = orderRepository.findForUpdate(request.orderId(), CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));

        if (OrderKind.orDefault(order.getKind()).usesWarehouseTransfer()) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber()
                    + " é uma reposição interna e não tem cliente a quem entregar. "
                    + "Converta-a em transferência entre armazéns.");
        }
        if (!OrderStatus.is(order.getStatus(), OrderStatus.PENDING)) {
            throw new BusinessRuleException(
                    "Só encomendas aprovadas e ainda não expedidas/faturadas podem gerar guia. "
                            + "Estado actual da encomenda: " + OrderStatusLabel.of(order.getStatus()) + ".");
        }

        if (request.responsible() == null || request.responsible().isBlank()) {
            throw new BusinessRuleException("O transportador / responsável é obrigatório para emitir a guia de remessa.");
        }
        if (request.vehicle() == null || request.vehicle().isBlank()) {
            throw new BusinessRuleException("A viatura / matrícula é obrigatória para emitir a guia de remessa.");
        }

        DeliveryGuide guide = new DeliveryGuide();
        guide.setGuideNumber(documentNumberService.next(DocumentSeries.DELIVERY_GUIDE));
        guide.setGuideDate(LocalDateTime.now());
        guide.setCompany(order.getCompany());
        guide.setOrderId(order.getId());
        guide.setOrderNumber(order.getOrderNumber());
        guide.setClient(order.getClient());
        guide.setWalkInName(order.getWalkInName());
        guide.setWarehouse(order.getWarehouse());
        guide.setTotalAmount(order.getTotalAmount());
        guide.setStatus(DeliveryGuideStatus.PENDING_APPROVAL);
        guide.setResponsible(blankToNull(request.responsible()));
        guide.setVehicle(blankToNull(request.vehicle()));
        guide.setNotes(blankToNull(request.notes()));
        guide.setCreatedBy(CurrentUserContext.getUsername());

        for (OrderLine ol : order.getLines()) {
            DeliveryGuideLine line = new DeliveryGuideLine();
            line.setProduct(ol.getProduct());
            line.setQuantity(ol.getQuantity());
            line.setUnitPrice(ol.getUnitPrice());
            line.setTaxRate(ol.getTaxRate());
            line.setDiscountPercentage(ol.getDiscountPercentage());
            line.setLineTotal(ol.getLineTotal());
            line.setBatchNumber(ol.getBatchNumber());
            line.setSerialNumber(ol.getSerialNumber());
            guide.addLine(line);
        }

        DeliveryGuide saved = guideRepository.save(guide);

        order.setStatus(OrderStatus.GUIDE_PENDING.name());
        order.setDeliveryGuideId(saved.getId());
        orderRepository.save(order);

        auditLogService.logCurrent("DELIVERY_GUIDE_CREATE",
                "Guia de remessa " + saved.getGuideNumber() + " criada a partir da encomenda "
                        + order.getOrderNumber() + ".");
        return toDTO(saved);
    }
}
