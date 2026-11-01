package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.*;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ComercialOrderLifecycleService {

    private static final Set<String> CANCELLABLE_STATUSES = Set.of(
            "PENDING_APPROVAL", "PENDING", "AWAITING_SEPARATION", "IN_SEPARATION", "SEPARATED");

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final InvoiceRepository invoiceRepository;
    private final ApprovalService approvalService;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final ReceivablesService receivablesService;
    private final OrderRiskEvaluator orderRiskEvaluator;
    private final ComercialClientAndCreditService clientAndCreditService;
    private final ComercialInvoiceLifecycleService invoiceLifecycleService;

    public ComercialOrderLifecycleService(
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            ClientRepository clientRepository,
            ProductRepository productRepository,
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository,
            @Lazy InventoryService inventoryService,
            InvoiceRepository invoiceRepository,
            @Lazy ApprovalService approvalService,
            WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            ReceivablesService receivablesService,
            OrderRiskEvaluator orderRiskEvaluator,
            ComercialClientAndCreditService clientAndCreditService,
            ComercialInvoiceLifecycleService invoiceLifecycleService) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.invoiceRepository = invoiceRepository;
        this.approvalService = approvalService;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.receivablesService = receivablesService;
        this.orderRiskEvaluator = orderRiskEvaluator != null ? orderRiskEvaluator : new OrderRiskEvaluator(invoiceRepository);
        this.clientAndCreditService = clientAndCreditService;
        this.invoiceLifecycleService = invoiceLifecycleService;
    }

    public OrderDTO createOrder(CreateOrderRequest request, OrderKind kind) {
        PermissionGuard.requireSeller("registar encomenda de cliente");
        CurrentUserContext.requireCompany(request.companyId());
        Client client = request.clientId() != null
                ? clientRepository.findByIdAndCompaniesId(request.clientId(), request.companyId())
                        .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."))
                : walkInClientProvider.getOrCreate();
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Warehouse destination = resolveDestination(request.companyId(), request.destinationWarehouseId(), kind, warehouse);
        List<OrderLine> lines = buildOrderLines(request.companyId(), request.lines());
        clientAndCreditService.validatePaymentTermsForClient(client, request.paymentTerms(), request.companyId());
        return placeOrder(company, client, warehouse, destination, request.walkInName(), lines, kind, request.agreedTerms());
    }

    public OrderDTO updateOrder(Long orderId, UpdateOrderRequest request) {
        PermissionGuard.requireSeller("actualizar encomenda");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        Long companyId = order.getCompany().getId();
        CurrentUserContext.requireCompany(companyId);
        requireEditableOrder(order);
        if (request.version() == null || request.version() != order.getVersion()) {
            throw new BusinessRuleException("A encomenda foi alterada noutro posto. Actualize a lista e tente novamente.");
        }
        Client client = request.clientId() == null
                ? walkInClientProvider.getOrCreate()
                : clientRepository.findByIdAndCompaniesId(request.clientId(), companyId)
                        .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!companyId.equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        OrderKind kind = OrderKind.orDefault(order.getKind());
        Warehouse destination = resolveDestination(companyId, request.destinationWarehouseId(), kind, warehouse);
        List<OrderLine> replacement = buildOrderLines(companyId, request.lines());
        if (order.isReservationActive()) {
            revalidateReservation(order, warehouse, replacement);
            replacement.forEach(line -> line.setReservedQuantity(line.getQuantity()));
        }
        order.setClient(client);
        order.setWalkInName(cleanWalkInName(request.walkInName()));
        order.setWarehouse(warehouse);
        order.setDestinationWarehouse(destination);
        String paymentTerms = request.paymentTerms() != null ? request.paymentTerms() : order.getPaymentTerms();
        clientAndCreditService.validatePaymentTermsForClient(client, paymentTerms, companyId);
        order.setPaymentTerms(paymentTerms);
        order.setNotes(request.notes());
        order.getLines().clear();
        replacement.forEach(order::addLine);
        recalculateOrderTotals(order);

        if (kind.requiresApproval()) {
            approvalService.cancelPendingForDocument("ORDER", order.getId(), "Substituído por uma nova versão da encomenda.");
            order.setStatus("PENDING_APPROVAL");
        }
        order = orderRepository.saveAndFlush(order);
        if (kind.requiresApproval()) submitForApproval(order);
        auditLogService.logCurrent("ORDER_UPDATE",
                "Encomenda " + order.getOrderNumber() + " actualizada com " + order.getLines().size() + " linha(s).");
        return toDTO(order);
    }

    private void requireEditableOrder(Order order) {
        String status = order.getStatus() == null ? "" : order.getStatus().toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING_APPROVAL", "PENDING", "AWAITING_SEPARATION").contains(status)) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " está em \""
                    + OrderStatusLabel.of(order.getStatus()) + "\" e já não pode ser alterada.");
        }
    }

    private List<OrderLine> buildOrderLines(Long companyId, List<CreateInvoiceLineRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException("A encomenda deve conter pelo menos uma linha.");
        }
        List<OrderLine> lines = new ArrayList<>();
        for (CreateInvoiceLineRequest request : requests) {
            if (request.quantity() == null || request.quantity().signum() <= 0) {
                throw new BusinessRuleException("A quantidade da linha deve ser positiva.");
            }
            BigDecimal discount = request.discountPercentage() == null ? BigDecimal.ZERO : request.discountPercentage();
            if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessRuleException("O desconto da linha deve estar entre 0 e 100 por cento.");
            }
            Product product = productRepository.findByIdAndCompaniesId(request.productId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + request.productId()));
            BigDecimal unitPrice = product.effectiveUnitPrice(request.quantity());
            BigDecimal taxRate = product.effectiveTaxRate();
            LineCalculator.LineAmounts amounts = LineCalculator.compute(unitPrice, request.quantity(), discount, taxRate);
            OrderLine line = new OrderLine();
            line.setProduct(product);
            line.setQuantity(request.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);
            line.setDiscountPercentage(discount);
            line.setBatchNumber(request.batchNumber());
            line.setSerialNumber(request.serialNumber());
            line.setLineTotal(amounts.total());
            lines.add(line);
        }
        return lines;
    }

    private void recalculateOrderTotals(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (OrderLine line : order.getLines()) {
            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    line.getUnitPrice(), line.getQuantity(), line.getDiscountPercentage(), line.getTaxRate());
            line.setLineTotal(amounts.total());
            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }
        order.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        order.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));
    }

    private String cleanWalkInName(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void revalidateReservation(Order order, Warehouse targetWarehouse, List<OrderLine> replacement) {
        Long previousWarehouseId = order.getWarehouse().getId();
        Map<Long, BigDecimal> previousByProduct = new HashMap<>();
        if (previousWarehouseId.equals(targetWarehouse.getId())) {
            for (OrderLine line : order.getLines()) {
                previousByProduct.merge(line.getProduct().getId(), line.getReservedQuantity(), BigDecimal::add);
            }
        }
        Map<Long, BigDecimal> requestedByProduct = new HashMap<>();
        Map<Long, Product> products = new HashMap<>();
        for (OrderLine line : replacement) {
            Long productId = line.getProduct().getId();
            requestedByProduct.merge(productId, line.getQuantity(), BigDecimal::add);
            products.put(productId, line.getProduct());
        }
        for (var entry : requestedByProduct.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal physical = inventoryService.lockAndGetPhysicalQuantity(productId, targetWarehouse.getId());
            BigDecimal active = orderLineRepository.sumActiveReservations(productId, targetWarehouse.getId());
            BigDecimal available = physical.subtract(active).add(previousByProduct.getOrDefault(productId, BigDecimal.ZERO));
            if (available.compareTo(entry.getValue()) < 0) {
                throw new BusinessRuleException("Stock disponível insuficiente para " + products.get(productId).getName() + ".");
            }
        }
    }

    public OrderDTO placeOrder(Company company, Client client, Warehouse warehouse, Warehouse destination,
                               String walkInName, List<OrderLine> lines, OrderKind kind, OrderTerms terms) {
        OrderTerms agreed = OrderTerms.orNone(terms);
        Order order = new Order();
        order.setClient(client);
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setDestinationWarehouse(destination);
        order.setKind(kind);
        order.setQuotationId(agreed.quotationId());
        order.setQuotationNumber(agreed.quotationNumber());
        order.setPaymentTerms(agreed.paymentTerms());
        order.setDeliveryTerms(agreed.deliveryTerms());
        order.setNotes(agreed.notes());
        order.assignExpectedDelivery(LocalDate.now(), agreed.deliveryDays());
        order.setStatus(kind.requiresApproval() ? "PENDING_APPROVAL" : "PENDING");
        if (walkInName != null && !walkInName.isBlank()) order.setWalkInName(walkInName.trim());

        lines.forEach(order::addLine);
        recalculateOrderTotals(order);
        order.setOrderNumber(documentNumberService.next(DocumentSeries.ORDER));
        order.setCreatedBy(CurrentUserContext.getUsername());
        order = orderRepository.save(order);

        if (kind.requiresApproval()) submitForApproval(order);
        return toDTO(order);
    }

    /** Desconto acima de 10% exige sempre gerente, como na fatura directa, seja qual for o valor. */
    private void submitForApproval(Order order) {
        OrderRiskAssessment assessment = orderRiskEvaluator.evaluate(
                order, order.getCompany() != null ? order.getCompany().getId() : CurrentUserContext.getCurrentCompanyId());
        boolean highDiscount = order.getLines().stream().anyMatch(line -> line.getDiscountPercentage() != null
                && line.getDiscountPercentage().compareTo(BigDecimal.TEN) > 0);
        if (highDiscount) {
            approvalService.submitRequest("ORDER", order.getId(), order.getTotalAmount(),
                    assessment.description() + " (Desconto especial >10%)", "MANAGER", false);
        } else {
            approvalService.submitRequest("ORDER", order.getId(), order.getTotalAmount(), assessment.description());
        }
    }

    public InvoiceDTO billOrder(Long orderId) {
        Long currentCompanyId = CurrentUserContext.requireCurrentCompanyId();
        Order order = orderRepository.findForUpdate(orderId, currentCompanyId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());

        if (order.getInvoiceId() != null || "BILLED".equalsIgnoreCase(order.getStatus()) || "INVOICED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " já foi faturada anteriormente"
                    + (order.getInvoiceId() != null ? " (Fatura ID: " + order.getInvoiceId() + ")" : "")
                    + ". Não é permitido faturar a mesma encomenda novamente.");
        }
        if (!OrderKind.orDefault(order.getKind()).isBillable()) {
            throw new BusinessRuleException("Não é possível facturar a encomenda " + order.getOrderNumber()
                    + ": é uma reposição interna, mercadoria que muda de armazém dentro da empresa. "
                    + "Não há cliente a quem cobrar — o que a cumpre é a transferência entre armazéns.");
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"SEPARATED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Apenas encomendas no estado PENDENTE podem ser faturadas.");
        }
        List<OrderLine> billableLines = order.getLines().stream().filter(line -> billedQuantity(order, line).signum() > 0).toList();
        if (billableLines.isEmpty()) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " não tem artigos separados para faturar.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (OrderLine line : billableLines) {
            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    line.getUnitPrice(), billedQuantity(order, line), line.getDiscountPercentage(), line.getTaxRate());
            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }
        BigDecimal billedTotal = subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP);
        receivablesService.assertCreditAvailable(order.getClient(), billedTotal);

        Invoice invoice = new Invoice();
        invoice.setClient(order.getClient());
        if (order.getWalkInName() != null && !order.getWalkInName().isBlank()) {
            invoice.setCustomerName(order.getWalkInName().trim());
        }
        invoice.setCompany(order.getCompany());
        invoice.setWarehouse(order.getWarehouse());
        LocalDate explicitDueDate = clientAndCreditService.resolveDueDateFromTerms(order.getPaymentTerms(), LocalDate.now(), order.getClient());
        invoice.assignDueDate(LocalDate.now(), explicitDueDate);
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setSalesChannel(SalesChannel.ORDER);
        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(billedTotal);
        invoice.setCreatedBy(CurrentUserContext.getUsername() != null ? CurrentUserContext.getUsername() : "SYSTEM");
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));

        for (OrderLine orderLine : billableLines) {
            BigDecimal quantity = billedQuantity(order, orderLine);
            InvoiceLine invoiceLine = new InvoiceLine();
            invoiceLine.setProduct(orderLine.getProduct());
            invoiceLine.setQuantity(quantity);
            invoiceLine.setUnitPrice(orderLine.getUnitPrice());
            invoiceLine.setTaxRate(orderLine.getTaxRate());
            invoiceLine.setDiscountPercentage(orderLine.getDiscountPercentage());
            invoiceLine.setLineTotal(LineCalculator.compute(orderLine.getUnitPrice(), quantity,
                    orderLine.getDiscountPercentage(), orderLine.getTaxRate()).total());
            invoiceLine.setBatchNumber(orderLine.getBatchNumber());
            invoiceLine.setSerialNumber(orderLine.getSerialNumber());
            invoiceLine.setUnitCost(orderLine.getProduct().getPurchasePrice());
            invoice.addLine(invoiceLine);

            String desc = String.format("Saída Fatura %s (Encomenda %s) - Cliente %s",
                    invoice.getInvoiceNumber(), order.getOrderNumber(), order.getClient().getName());
            inventoryService.registerMovement(orderLine.getProduct(), order.getWarehouse(),
                    quantity.negate(), "SALE", orderLine.getBatchNumber(), orderLine.getSerialNumber(), desc);
        }

        invoice = invoiceRepository.save(invoice);
        invoiceLifecycleService.publishSale(invoice, BigDecimal.ZERO, false);
        order.setStatus("BILLED");
        order.setInvoiceId(invoice.getId());
        order.setReservationActive(false);
        orderRepository.save(order);
        return invoiceLifecycleService.toDTO(invoice);
    }

    /** Depois da conferência, só se fatura o que o armazém separou mesmo. */
    private static BigDecimal billedQuantity(Order order, OrderLine line) {
        return "SEPARATED".equalsIgnoreCase(order.getStatus()) ? line.getSeparatedQuantity() : line.getQuantity();
    }

    public List<OrderDTO> getOrdersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return orderRepository.findByCompanyId(companyId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> searchCancellableOrders(String query) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return orderRepository.findByCompanyId(companyId).stream()
                .filter(ComercialOrderLifecycleService::isCancellable)
                .filter(o -> ComercialInvoiceLifecycleService.matches(o.getOrderNumber(), query)
                        || (o.getClient() != null && ComercialInvoiceLifecycleService.matches(o.getClient().getName(), query)))
                .map(this::toDTO).collect(Collectors.toList());
    }

    public void cancelOrder(Long orderId, String reason) {
        PermissionGuard.requireManagerOrAdmin("cancelar encomenda");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É necessário indicar o motivo do cancelamento.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        if ("BILLED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Encomenda já faturada — anule a fatura associada.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Esta encomenda já se encontra cancelada.");
        }
        if (!isCancellable(order)) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " está em \""
                    + OrderStatusLabel.of(order.getStatus()) + "\" e já não pode ser cancelada: "
                    + "trate primeiro a guia ou a transferência associada.");
        }
        approvalService.cancelPendingForDocument("ORDER", order.getId(), reason);
        order.setStatus("CANCELLED");
        if (order.isReservationActive()) {
            order.setReservationActive(false);
            order.getLines().forEach(line -> line.setReservedQuantity(BigDecimal.ZERO));
        }
        orderRepository.save(order);
        auditLogService.logCurrent("ORDER_CANCEL", "Encomenda " + order.getOrderNumber() + " cancelada. Motivo: " + reason);
    }

    /** Depois de haver guia, transferência ou fatura, cancelar deixaria o documento ligado sem origem. */
    private static boolean isCancellable(Order order) {
        String status = order.getStatus() == null ? "" : order.getStatus().toUpperCase(Locale.ROOT);
        return CANCELLABLE_STATUSES.contains(status);
    }

    public OrderDTO approveOrder(Long orderId, String comments) {
        PermissionGuard.requireManagerOrAdmin("aprovar encomenda");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        if (!"PENDING_APPROVAL".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Apenas encomendas no estado \"Pendente de Aprovação\" podem ser aprovadas. Estado actual: "
                    + OrderStatusLabel.of(order.getStatus()) + ".");
        }
        List<ApprovalRequestDTO> pendings = approvalService.getPendingRequests();
        Long reqId = pendings.stream()
                .filter(r -> "ORDER".equalsIgnoreCase(r.documentType()) && orderId.equals(r.documentId()))
                .map(ApprovalRequestDTO::id).findFirst().orElse(null);
        if (reqId != null) {
            approvalService.approveRequest(reqId, comments != null && !comments.isBlank() ? comments : "Aprovado via Central Comercial.");
        } else {
            order.setStatus("PENDING");
            order = orderRepository.save(order);
        }
        auditLogService.logCurrent("ORDER_APPROVE",
                "Encomenda " + order.getOrderNumber() + " aprovada por " + CurrentUserContext.getUsername() + ".");
        return toDTO(orderRepository.findById(orderId).orElse(order));
    }

    public OrderDTO rejectOrder(Long orderId, String reason) {
        PermissionGuard.requireManagerOrAdmin("rejeitar encomenda");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o motivo da rejeição da encomenda.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        if (!"PENDING_APPROVAL".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Apenas encomendas no estado \"Pendente de Aprovação\" podem ser rejeitadas. Estado actual: "
                    + OrderStatusLabel.of(order.getStatus()) + ".");
        }
        List<ApprovalRequestDTO> pendings = approvalService.getPendingRequests();
        Long reqId = pendings.stream()
                .filter(r -> "ORDER".equalsIgnoreCase(r.documentType()) && orderId.equals(r.documentId()))
                .map(ApprovalRequestDTO::id).findFirst().orElse(null);
        if (reqId != null) {
            approvalService.rejectRequest(reqId, reason);
        } else {
            order.setStatus("CANCELLED");
            order = orderRepository.save(order);
        }
        auditLogService.logCurrent("ORDER_REJECT",
                "Encomenda " + order.getOrderNumber() + " rejeitada por " + CurrentUserContext.getUsername() + ". Motivo: " + reason);
        return toDTO(orderRepository.findById(orderId).orElse(order));
    }

    public List<OrderDTO> getPendingOrdersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return orderRepository.findByCompanyIdAndStatusAndInvoiceIdIsNull(companyId, "PENDING")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> searchPendingOrders(String query) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return orderRepository.findByCompanyIdAndStatusAndInvoiceIdIsNull(companyId, "PENDING").stream()
                .filter(o -> ComercialInvoiceLifecycleService.matches(o.getOrderNumber(), query)
                        || (o.getClient() != null && ComercialInvoiceLifecycleService.matches(o.getClient().getName(), query)))
                .map(this::toDTO).collect(Collectors.toList());
    }

    private Warehouse resolveDestination(Long companyId, Long destinationWarehouseId, OrderKind kind, Warehouse origin) {
        if (!kind.requiresDestinationWarehouse()) return null;
        if (destinationWarehouseId == null) {
            throw new BusinessRuleException("Indique o armazém de destino: uma reposição interna tem de dizer para que loja vai a mercadoria.");
        }
        if (destinationWarehouseId.equals(origin.getId())) {
            throw new BusinessRuleException("O armazém de destino tem de ser diferente do de origem.");
        }
        Warehouse destination = warehouseRepository.findById(destinationWarehouseId)
                .orElseThrow(() -> new BusinessRuleException("Armazém de destino não encontrado."));
        if (!companyId.equals(destination.getCompany().getId())) {
            throw new BusinessRuleException("O armazém de destino não pertence à empresa ativa.");
        }
        return destination;
    }

    public Order createReplenishmentRecord(StockTransferDTO transfer) {
        CurrentUserContext.requireCompany(transfer.companyId());
        Company company = companyRepository.findById(transfer.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse origin = warehouseRepository.findById(transfer.originWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de origem não encontrado."));
        Warehouse destination = warehouseRepository.findById(transfer.destinationWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de destino não encontrado."));

        Order order = new Order();
        order.setClient(walkInClientProvider.getOrCreate());
        order.setCompany(company);
        order.setWarehouse(origin);
        order.setDestinationWarehouse(destination);
        order.setKind(OrderKind.INTERNAL_REPLENISHMENT);
        order.setStatus("TRANSFERRED");
        order.setStockTransferId(transfer.id());
        order.setTransferNumber(transfer.transferNumber());
        order.setOrderNumber(documentNumberService.next(DocumentSeries.ORDER));
        order.setCreatedBy(CurrentUserContext.getUsername());

        for (var transferLine : transfer.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(transferLine.productId(), transfer.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + transferLine.productId()));
            BigDecimal unitPrice = product.effectiveUnitPrice(transferLine.quantity());
            BigDecimal taxRate = product.effectiveTaxRate();
            OrderLine line = new OrderLine();
            line.setProduct(product);
            line.setQuantity(transferLine.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);
            line.setDiscountPercentage(BigDecimal.ZERO);
            line.setBatchNumber(transferLine.batchNumber());
            order.addLine(line);
        }
        recalculateOrderTotals(order);
        return orderRepository.save(order);
    }

    public OrderDTO toDTO(Order order) {
        var logistics = LogisticsLoadCalculator.calculate(order.getLines().stream()
                .map(line -> new LogisticsLoadCalculator.Input(line.getQuantity(), line.getProduct().getGrossUnitWeightKg())).toList());
        List<OrderLineDTO> lines = IntStream.range(0, order.getLines().size()).mapToObj(index -> {
            OrderLine line = order.getLines().get(index);
            var share = logistics.get(index);
            return new OrderLineDTO(
                    line.getId(), line.getProduct().getId(), line.getProduct().getName(), line.getProduct().getSku(),
                    line.getQuantity(), line.getUnitPrice(), line.getTaxRate(), line.getLineTotal(),
                    line.getDiscountPercentage(), line.getBatchNumber(), line.getSerialNumber(),
                    Math.max(1, line.getProduct().getUnitsPerBox()),
                    line.getProduct().getNetUnitWeightKg(), line.getProduct().getGrossUnitWeightKg(),
                    share.lineWeightKg(), share.quantityPercentage(), share.weightPercentage());
        }).toList();
        return new OrderDTO(
                order.getId(), order.getOrderNumber(),
                order.getClient().getId(), order.getClient().getName(), order.getClient().getTaxId(),
                order.getWalkInName(), order.getTotalBeforeTax(), order.getTaxAmount(), order.getTotalAmount(),
                order.getStatus(), order.getInvoiceId(), lines,
                order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now(),
                order.getPrintedAt(), order.getPrintCount(), order.getLastPrintedBy(),
                OrderKind.orDefault(order.getKind()), OrderKind.orDefault(order.getKind()).label(),
                OrderStatusLabel.of(order.getStatus()),
                order.getQuotationId(), order.getQuotationNumber(),
                order.getPaymentTerms(), order.getDeliveryTerms(), order.getExpectedDeliveryDate(),
                order.isDeliveryOverdue(LocalDate.now()),
                order.getWarehouse() == null ? null : order.getWarehouse().getId(),
                order.getWarehouse() == null ? null : order.getWarehouse().getName(),
                order.getDestinationWarehouse() == null ? null : order.getDestinationWarehouse().getId(),
                order.getDestinationWarehouse() == null ? null : order.getDestinationWarehouse().getName(),
                order.getStockTransferId(), order.getTransferNumber(), order.getVersion(),
                order.getBusinessUnit() == null ? null : order.getBusinessUnit().getId(),
                order.getBusinessUnit() == null ? null : order.getBusinessUnit().getName(),
                order.getNotes());
    }

    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        order.getLines().size();
        return toDTO(order);
    }

    public OrderDTO markOrderPrinted(Long orderId, String operator) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        order.setPrintCount(order.getPrintCount() + 1);
        order.setPrintedAt(LocalDateTime.now());
        order.setLastPrintedBy(operator == null || operator.isBlank() ? "SYSTEM" : operator);
        return toDTO(orderRepository.save(order));
    }
}
