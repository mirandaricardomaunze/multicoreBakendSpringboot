package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.events.ReceiptCancelledEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.*;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import mz.multicore.erp.modules.pricing.service.PriceResolutionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ComercialInvoiceLifecycleService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final ReceiptRepository receiptRepository;
    private final FinanceService financeService;
    private final ApprovalService approvalService;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final ReceivablesService receivablesService;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessUnitRepository businessUnitRepository;
    private final SaleStockIssuer saleStockIssuer;
    private final PriceResolutionService priceResolver;

    public ComercialInvoiceLifecycleService(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            ProductRepository productRepository,
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository,
            @Lazy InventoryService inventoryService,
            ReceiptRepository receiptRepository,
            @Lazy FinanceService financeService,
            @Lazy ApprovalService approvalService,
            WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            ReceivablesService receivablesService,
            ApplicationEventPublisher eventPublisher,
            @Autowired(required = false) BusinessUnitRepository businessUnitRepository, SaleStockIssuer saleStockIssuer,
            @Autowired(required = false) PriceResolutionService priceResolver) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.receiptRepository = receiptRepository;
        this.financeService = financeService;
        this.approvalService = approvalService;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.receivablesService = receivablesService;
        this.eventPublisher = eventPublisher;
        this.businessUnitRepository = businessUnitRepository;
        this.saleStockIssuer = saleStockIssuer;
        this.priceResolver = priceResolver;
    }

    public void publishSale(Invoice invoice, BigDecimal amountPaidNow, boolean cashPayment) {
        BigDecimal cost = invoice.getLines().stream().map(InvoiceLine::lineCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        eventPublisher.publishEvent(new SaleRegisteredEvent(
                invoice.getCompany().getId(), invoice.getId(), invoice.getInvoiceNumber(),
                invoice.issueDate() == null ? LocalDate.now() : invoice.issueDate(),
                invoice.getTotalBeforeTax(), invoice.getTaxAmount(), invoice.getTotalAmount(),
                cost, amountPaidNow, cashPayment));
    }

    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("emitir fatura");
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
        BusinessUnit restrictedBu = null;
        if (request.businessUnitId() != null && businessUnitRepository != null) {
            restrictedBu = businessUnitRepository.findByIdAndCompanyId(request.businessUnitId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Linha de negócio restrita não encontrada."));
        }

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        if (request.customerName() != null && !request.customerName().isBlank()) {
            invoice.setCustomerName(request.customerName().trim());
        }
        invoice.setCompany(company);
        invoice.setWarehouse(warehouse);
        if (restrictedBu != null) invoice.setBusinessUnit(restrictedBu);
        invoice.assignDueDate(LocalDate.now(), request.dueDate());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        boolean hasHighDiscount = false;
        var pricing = PriceResolutionService.selectionFor(priceResolver, request.companyId(), client);

        for (CreateInvoiceLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));
            if (restrictedBu != null) {
                var prodBu = product.getBusinessUnit();
                if (prodBu == null || !restrictedBu.getId().equals(prodBu.getId())) {
                    String prodBuName = prodBu != null ? prodBu.getName() : "Sem linha";
                    throw new BusinessRuleException("A fatura está restrita à linha de negócio '" + restrictedBu.getName()
                            + "' e não aceita o produto '" + product.getName()
                            + "' (pertencente a: " + prodBuName + "). Desmarque o filtro de negócio para emitir produtos mistos.");
                }
            }
            BigDecimal unitPrice = PriceResolutionService.unitPrice(priceResolver, pricing, product, lineReq.quantity(), lineReq.unitPrice());
            BigDecimal taxRate = product.effectiveTaxRate();
            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);
            BigDecimal discountPct = lineReq.discountPercentage() == null ? BigDecimal.ZERO : lineReq.discountPercentage();
            if (discountPct.signum() < 0 || discountPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessRuleException("O desconto da linha deve estar entre 0 e 100 por cento.");
            }
            line.setDiscountPercentage(discountPct);
            if (discountPct.compareTo(BigDecimal.valueOf(10)) > 0) hasHighDiscount = true;
            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());
            line.setUnitCost(product.getPurchasePrice());

            LineCalculator.LineAmounts amounts = LineCalculator.compute(unitPrice, lineReq.quantity(), discountPct, taxRate);
            line.setLineTotal(amounts.total());
            invoice.addLine(line);
            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));

        receivablesService.assertCreditAvailable(client, invoice.getTotalAmount());
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));
        invoice.setCreatedBy(CurrentUserContext.getUsername());

        if (hasHighDiscount) {
            invoice.setStatus(InvoiceStatus.PENDING_DISCOUNT_APPROVAL);
            invoice = invoiceRepository.save(invoice);
            String approvalDesc = String.format("Fatura %s para %s - Total: %s MT (Aprovação de Desconto Especial >10%%)",
                    invoice.getInvoiceNumber(), client.getName(), invoice.getTotalAmount());
            approvalService.submitRequest("INVOICE", invoice.getId(), invoice.getTotalAmount(), approvalDesc, "MANAGER", false);
        } else {
            invoice.setStatus(InvoiceStatus.APPROVED);
            invoice = invoiceRepository.save(invoice);
            deductStockForInvoice(invoice);
            auditLogService.logCurrent("INVOICE_ISSUE", "Fatura " + invoice.getInvoiceNumber() + " emitida e aprovada para "
                    + client.getName() + ". Total: " + invoice.getTotalAmount() + " MT.");
            publishSale(invoice, BigDecimal.ZERO, false);
        }
        return toDTO(invoice);
    }

    /** Também usado pela aprovação de desconto especial: a mesma saída, com a mesma verificação de reservas. */
    void deductStockForInvoice(Invoice invoice) {
        saleStockIssuer.issue(invoice, invoice.getWarehouse(),
                String.format("Saída Fatura %s - Cliente %s", invoice.getInvoiceNumber(), invoice.getClient().getName()));
    }

    public void cancelInvoice(Long invoiceId, String reason) {
        PermissionGuard.requireManagerOrAdmin("anular fatura");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É necessário indicar o motivo da anulação.");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Esta fatura já se encontra anulada.");
        }
        boolean realisedSale = invoice.getStatus().isRealisedSale();
        if (realisedSale && invoice.getSalesChannel() == SalesChannel.POS) {
            throw new BusinessRuleException("A venda de balcão " + invoice.getInvoiceNumber()
                    + " não se anula aqui: faça a devolução no POS (F8), que repõe a caixa e o stock.");
        }
        if (invoice.getNoteAdjustment() != null && invoice.getNoteAdjustment().signum() != 0) {
            throw new BusinessRuleException("A fatura " + invoice.getInvoiceNumber()
                    + " tem notas de crédito ou de débito aprovadas e não pode ser anulada.");
        }
        // Numa fatura de guia, a mercadoria continua entregue: a anulação não repõe stock.
        if (realisedSale && invoice.getDeliveryGuideId() == null) {
            invoice.getLines().forEach(line -> {
                String desc = String.format("Estorno Fatura %s - Motivo: %s", invoice.getInvoiceNumber(), reason);
                inventoryService.registerMovement(line.getProduct(), invoice.getWarehouse(), line.getQuantity(),
                        "REVERSAL", line.getBatchNumber(), line.getSerialNumber(), desc);
            });
        }
        List<Receipt> receipts = receiptRepository.findByCompanyId(CurrentUserContext.getCurrentCompanyId()).stream()
                .filter(r -> r.getInvoice().getId().equals(invoiceId) && !"CANCELLED".equals(r.getStatus()))
                .collect(Collectors.toList());
        for (Receipt r : receipts) {
            r.setStatus("CANCELLED");
            r.setCancellationReason("Fatura anulada: " + reason);
            receiptRepository.save(r);
            String description = "Estorno automático por Fatura Anulada " + invoice.getInvoiceNumber();
            financeService.registerTransaction(r.getTreasuryAccount().getId(), "CREDIT", r.getAmountPaid(), description);
            eventPublisher.publishEvent(new ReceiptCancelledEvent(
                    invoice.getCompany().getId(), r.getId(), r.getReceiptNumber(), LocalDate.now()));
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancellationReason(reason);
        invoiceRepository.save(invoice);
        if (realisedSale) {
            eventPublisher.publishEvent(new InvoiceCancelledEvent(
                    invoice.getCompany().getId(), invoice.getId(), invoice.getInvoiceNumber(), LocalDate.now()));
        }
        auditLogService.logCurrent("INVOICE_CANCEL", "Fatura " + invoice.getInvoiceNumber() + " anulada. Motivo: " + reason);
    }

    public InvoiceDTO createInvoiceFromQuotation(Quotation quotation) {
        CurrentUserContext.requireCompany(quotation.getCompany().getId());
        Client client = quotation.getClient();
        if (client != null) receivablesService.assertCreditAvailable(client, quotation.getTotalAmount());

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setCompany(quotation.getCompany());
        invoice.setWarehouse(quotation.getWarehouse());
        invoice.setCustomerName(quotation.clientLabel());
        invoice.assignDueDate(LocalDate.now(), null);
        invoice.setSalesChannel(SalesChannel.MANUAL);
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setTotalBeforeTax(quotation.getTotalBeforeTax());
        invoice.setTaxAmount(quotation.getTaxAmount());
        invoice.setTotalAmount(quotation.getTotalAmount());
        invoice.setCreatedBy(CurrentUserContext.getUsername() != null ? CurrentUserContext.getUsername() : "SYSTEM");
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));

        for (QuotationLine quotedLine : quotation.getLines()) {
            InvoiceLine invoiceLine = new InvoiceLine();
            invoiceLine.setProduct(quotedLine.getProduct());
            invoiceLine.setQuantity(quotedLine.getQuantity());
            invoiceLine.setUnitPrice(quotedLine.getUnitPrice());
            invoiceLine.setTaxRate(quotedLine.getTaxRate());
            invoiceLine.setDiscountPercentage(quotedLine.getDiscountPercentage());
            invoiceLine.setLineTotal(quotedLine.getLineTotal());
            invoiceLine.setUnitCost(quotedLine.getProduct().getPurchasePrice());
            invoice.addLine(invoiceLine);
        }
        saleStockIssuer.issue(invoice, quotation.getWarehouse(), String.format("Saída Fatura %s (Cotação %s) - Cliente %s",
                invoice.getInvoiceNumber(), quotation.getQuotationNumber(), quotation.clientLabel()));
        invoice = invoiceRepository.save(invoice);
        publishSale(invoice, BigDecimal.ZERO, false);
        return toDTO(invoice);
    }

    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findByCompanyId(CurrentUserContext.getCurrentCompanyId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<Receipt> getAllReceipts() {
        return receiptRepository.findByCompanyId(CurrentUserContext.getCurrentCompanyId());
    }

    public PageResponse<InvoiceDTO> getInvoicePage(Long companyId, Integer page, Integer size) {
        CurrentUserContext.requireCompany(companyId);
        return PageResponseMapper.from(
                invoiceRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, PageQuery.of(page, size)),
                this::toDTO);
    }

    public PageResponse<InvoiceDTO> getPOSSalesPage(Long companyId, Integer page, Integer size, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
        return PageResponseMapper.from(
                invoiceRepository.findSalesPage(companyId, SalesChannel.POS, fromDateTime, toDateTime, PageQuery.of(page, size)),
                this::toDTO);
    }

    public POSSalesSummaryDTO getPOSSalesSummary(Long companyId, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
        long count = invoiceRepository.countSales(companyId, SalesChannel.POS, fromDateTime, toDateTime);
        BigDecimal total = invoiceRepository.sumSalesTotal(companyId, SalesChannel.POS, fromDateTime, toDateTime);
        if (from == null || to == null) {
            return new POSSalesSummaryDTO(count, total, null, null, null, null);
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        LocalDateTime previousFromDateTime = previousFrom.atStartOfDay();
        LocalDateTime previousToDateTime = previousTo.plusDays(1).atStartOfDay().minusNanos(1);
        long previousCount = invoiceRepository.countSales(companyId, SalesChannel.POS, previousFromDateTime, previousToDateTime);
        BigDecimal previousTotal = invoiceRepository.sumSalesTotal(companyId, SalesChannel.POS, previousFromDateTime, previousToDateTime);
        return new POSSalesSummaryDTO(count, total, previousCount, previousTotal,
                percentVariation(BigDecimal.valueOf(count), BigDecimal.valueOf(previousCount)),
                percentVariation(total, previousTotal));
    }

    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository.findByCompanyId(companyId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InvoiceDTO> searchInvoices(String query) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> matches(i.getInvoiceNumber(), query) || (i.getClient() != null && matches(i.getClient().getName(), query)))
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<InvoiceDTO> getPOSSalesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository.findByCompanyIdAndSalesChannelOrderByCreatedAtDesc(companyId, SalesChannel.POS)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> i.getStatus().isCollectable() && i.outstandingAmount().signum() > 0)
                .map(this::toDTO)
                .sorted(Comparator.comparingInt(InvoiceDTO::daysOverdue).reversed())
                .collect(Collectors.toList());
    }

    public List<ReceiptDTO> getReceiptsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return receiptRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public ReceiptDTO toDTO(Receipt r) {
        Long buId = r.getInvoice() != null && r.getInvoice().getBusinessUnit() != null ? r.getInvoice().getBusinessUnit().getId() : null;
        String buName = r.getInvoice() != null && r.getInvoice().getBusinessUnit() != null ? r.getInvoice().getBusinessUnit().getName() : null;
        return new ReceiptDTO(
                r.getId(), r.getReceiptNumber(),
                r.getInvoice() != null ? r.getInvoice().getInvoiceNumber() : null,
                r.getInvoice() != null && r.getInvoice().getClient() != null ? r.getInvoice().getClient().getName() : null,
                r.getAmountPaid(), r.getPaymentMethod(), r.getStatus(), r.getReceiptDate(), buId, buName);
    }

    public InvoiceDTO toDTO(Invoice invoice) {
        List<InvoiceLineDTO> lines = invoice.getLines().stream().map(line -> new InvoiceLineDTO(
                line.getId(), line.getProduct().getId(), line.getProduct().getName(), line.getProduct().getSku(),
                line.getQuantity(), line.getUnitPrice(), line.getTaxRate(), line.getLineTotal(),
                line.getDiscountPercentage(), line.getBatchNumber(), line.getSerialNumber()
        )).collect(Collectors.toList());
        return new InvoiceDTO(
                invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getClient().getId(), invoice.getClient().getName(), invoice.getClient().getTaxId(),
                invoice.getTotalBeforeTax(), invoice.getTaxAmount(), invoice.getTotalAmount(),
                invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO,
                invoice.getStatus(), invoice.getRejectionReason(), lines,
                invoice.getCreatedAt() != null ? invoice.getCreatedAt() : LocalDateTime.now(),
                invoice.getCreatedBy(), invoice.effectiveDueDate(),
                invoice.daysOverdue(LocalDate.now()), invoice.agingBucket(LocalDate.now()),
                invoice.getBusinessUnit() != null ? invoice.getBusinessUnit().getId() : null,
                invoice.getBusinessUnit() != null ? invoice.getBusinessUnit().getName() : null,
                invoice.getCustomerName(), invoice.getNoteAdjustment());
    }

    static boolean matches(String haystack, String needle) {
        if (needle == null || needle.isBlank()) return true;
        if (haystack == null) return false;
        return haystack.toLowerCase(Locale.ROOT).contains(needle.trim().toLowerCase(Locale.ROOT));
    }

    private static BigDecimal percentVariation(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;
        if (safePrevious.signum() == 0) return safeCurrent.signum() == 0 ? BigDecimal.ZERO : null;
        return safeCurrent.subtract(safePrevious).multiply(BigDecimal.valueOf(100)).divide(safePrevious, 1, RoundingMode.HALF_UP);
    }
}
