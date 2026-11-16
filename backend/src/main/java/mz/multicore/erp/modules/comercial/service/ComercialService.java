package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.architecture.events.ReceiptCancelledEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.*;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.service.TreasuryAccountLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.service.BusinessUnitLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComercialService {

    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final TreasuryAccountLookup treasuryAccountLookup;
    private final OrderRepository orderRepository;
    private final DocumentNumberService documentNumberService;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ComercialClientAndCreditService clientAndCreditService;
    private final ComercialProductCatalogService productCatalogService;
    private final ComercialInvoiceLifecycleService invoiceLifecycleService;
    private final ComercialOrderLifecycleService orderLifecycleService;

    public ComercialService(
            ClientRepository clientRepository,
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository,
            InvoiceRepository invoiceRepository,
            @Lazy ApprovalService approvalService,
            CompanyLookup companyLookup,
            WarehouseLookup warehouseLookup,
            @Lazy InventoryService inventoryService,
            ReceiptRepository receiptRepository,
            @Lazy FinanceService financeService,
            TreasuryAccountLookup treasuryAccountLookup,
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            TaxRateLookup taxRateLookup,
            ReceivablesService receivablesService,
            ApplicationEventPublisher eventPublisher) {
        this(clientRepository, productRepository, productCategoryRepository, invoiceRepository,
                approvalService, companyLookup, warehouseLookup, inventoryService,
                receiptRepository, financeService, treasuryAccountLookup, orderRepository,
                orderLineRepository, walkInClientProvider, documentNumberService, auditLogService,
                taxRateLookup, receivablesService, eventPublisher, new OrderRiskEvaluator(invoiceRepository), null);
    }

    public ComercialService(
            ClientRepository clientRepository,
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository,
            InvoiceRepository invoiceRepository,
            @Lazy ApprovalService approvalService,
            CompanyLookup companyLookup,
            WarehouseLookup warehouseLookup,
            @Lazy InventoryService inventoryService,
            ReceiptRepository receiptRepository,
            @Lazy FinanceService financeService,
            TreasuryAccountLookup treasuryAccountLookup,
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            TaxRateLookup taxRateLookup,
            ReceivablesService receivablesService,
            ApplicationEventPublisher eventPublisher,
            OrderRiskEvaluator orderRiskEvaluator,
            BusinessUnitLookup businessUnitLookup) {
        this(
                invoiceRepository, receiptRepository, treasuryAccountLookup, orderRepository,
                documentNumberService, financeService, auditLogService, eventPublisher,
                new ComercialClientAndCreditService(clientRepository, companyLookup, invoiceRepository, auditLogService),
                new ComercialProductCatalogService(productRepository, productCategoryRepository, companyLookup,
                        taxRateLookup, businessUnitLookup, inventoryService, auditLogService),
                new ComercialInvoiceLifecycleService(invoiceRepository, clientRepository, productRepository,
                        companyLookup, warehouseLookup, inventoryService, receiptRepository, financeService,
                        approvalService, walkInClientProvider, documentNumberService, auditLogService,
                        receivablesService, eventPublisher, businessUnitLookup, new SaleStockIssuer(inventoryService, orderLineRepository), null),
                new ComercialOrderLifecycleService(orderRepository, orderLineRepository, clientRepository,
                        productRepository, companyLookup, warehouseLookup, inventoryService, invoiceRepository,
                        approvalService, walkInClientProvider, documentNumberService, auditLogService,
                        receivablesService, orderRiskEvaluator,
                        new ComercialClientAndCreditService(clientRepository, companyLookup, invoiceRepository, auditLogService),
                        new ComercialInvoiceLifecycleService(invoiceRepository, clientRepository, productRepository,
                                companyLookup, warehouseLookup, inventoryService, receiptRepository, financeService,
                                approvalService, walkInClientProvider, documentNumberService, auditLogService,
                                receivablesService, eventPublisher, businessUnitLookup, new SaleStockIssuer(inventoryService, orderLineRepository), null), null)
        );
    }

    @Autowired
    public ComercialService(
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            TreasuryAccountLookup treasuryAccountLookup,
            OrderRepository orderRepository,
            DocumentNumberService documentNumberService,
            @Lazy FinanceService financeService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher,
            ComercialClientAndCreditService clientAndCreditService,
            ComercialProductCatalogService productCatalogService,
            ComercialInvoiceLifecycleService invoiceLifecycleService,
            ComercialOrderLifecycleService orderLifecycleService) {
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.treasuryAccountLookup = treasuryAccountLookup;
        this.orderRepository = orderRepository;
        this.documentNumberService = documentNumberService;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.clientAndCreditService = clientAndCreditService;
        this.productCatalogService = productCatalogService;
        this.invoiceLifecycleService = invoiceLifecycleService;
        this.orderLifecycleService = orderLifecycleService;
    }

    @Transactional
    public ClientDTO createClient(SaveClientRequest request) { return clientAndCreditService.createClient(request); }

    @Transactional
    public ClientDTO updateClient(Long id, SaveClientRequest request) { return clientAndCreditService.updateClient(id, request); }

    @Transactional
    public void deleteClient(Long id) { clientAndCreditService.deleteClient(id); }

    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients() { return clientAndCreditService.getAllClients(); }

    @Transactional(readOnly = true)
    public ClientCreditEligibilityDTO getClientCreditEligibility(Long clientId) {
        return clientAndCreditService.getClientCreditEligibility(clientId);
    }

    public ClientCreditEligibilityDTO evaluateClientCreditEligibility(Client client, Long companyId) {
        return clientAndCreditService.evaluateClientCreditEligibility(client, companyId);
    }

    public void validatePaymentTermsForClient(Client client, String paymentTerms, Long companyId) {
        clientAndCreditService.validatePaymentTermsForClient(client, paymentTerms, companyId);
    }

    public static boolean isPromptPayment(String terms) { return ComercialClientAndCreditService.isPromptPayment(terms); }

    public LocalDate resolveDueDateFromTerms(String paymentTerms, LocalDate issueDate, Client client) {
        return clientAndCreditService.resolveDueDateFromTerms(paymentTerms, issueDate, client);
    }

    @Transactional
    public InvoiceDTO createInvoice(CreateInvoiceRequest request) { return invoiceLifecycleService.createInvoice(request); }

    @Transactional
    public ReceiptDTO createReceipt(Long invoiceId, Long treasuryAccountId, String paymentMethod, BigDecimal amountPaid) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Invoice invoice = invoiceRepository.lockByIdAndCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        if (!invoice.getStatus().isCollectable()) {
            throw new BusinessRuleException("Apenas faturas por cobrar podem ter recibo. Estado atual: " + invoice.getStatus());
        }
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor do recibo tem de ser positivo.");
        }
        BigDecimal outstanding = invoice.outstandingAmount();
        if (amountPaid.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Valor do recibo (" + DisplayNumbers.money(amountPaid)
                    + " MT) excede o saldo em dívida (" + DisplayNumbers.money(outstanding) + " MT).");
        }

        TreasuryAccount account = treasuryAccountLookup.requireForCompany(treasuryAccountId, companyId);

        Receipt receipt = new Receipt();
        receipt.setInvoice(invoice);
        receipt.setCompany(invoice.getCompany());
        receipt.setTreasuryAccount(account);
        receipt.setAmountPaid(amountPaid);
        receipt.setPaymentMethod(paymentMethod);
        receipt.setReceiptDate(LocalDateTime.now());
        receipt.setStatus("COMPLETED");

        String receiptNum = documentNumberService.next(DocumentSeries.RECEIPT);
        receipt.setReceiptNumber(receiptNum);
        receipt.setCreatedBy(CurrentUserContext.getUsername() != null ? CurrentUserContext.getUsername() : "SYSTEM");
        receipt = receiptRepository.save(receipt);

        invoice.setAmountPaid(invoice.getAmountPaid().add(amountPaid).setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(invoice.deriveStatusFromPayments());
        invoiceRepository.save(invoice);

        String description = "Recebimento Fatura " + invoice.getInvoiceNumber() + " via " + paymentMethod + " (Recibo " + receiptNum + ")";
        financeService.registerTransaction(treasuryAccountId, "DEBIT", amountPaid, description);

        eventPublisher.publishEvent(new PaymentReceivedEvent(
                invoice.getCompany().getId(), receipt.getId(), receiptNum,
                LocalDate.now(), amountPaid, "CASH".equalsIgnoreCase(paymentMethod) || "DINHEIRO".equalsIgnoreCase(paymentMethod)));

        return toDTO(receipt);
    }

    @Transactional
    public void cancelReceipt(Long receiptId, String reason) {
        PermissionGuard.requireManagerOrAdmin("anular recibo");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É necessário indicar o motivo da anulação do recibo.");
        }
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Receipt receipt = receiptRepository.lockByIdAndCompanyId(receiptId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if ("CANCELLED".equals(receipt.getStatus())) {
            throw new BusinessRuleException("Este recibo já se encontra anulado.");
        }
        receipt.setStatus("CANCELLED");
        receipt.setCancellationReason(reason);
        receiptRepository.save(receipt);

        Invoice invoice = invoiceRepository.lockByIdAndCompanyId(receipt.getInvoice().getId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Fatura do recibo não encontrada."));
        if (AdvancePayments.isOffsetPayment(receipt.getPaymentMethod()) || receipt.getAmountPaid().compareTo(invoice.getAmountPaid()) > 0)
            throw new BusinessRuleException("Este recibo está ligado a um adiantamento já aplicado e não pode ser anulado.");
        BigDecimal remaining = invoice.getAmountPaid().subtract(receipt.getAmountPaid());
        invoice.setAmountPaid(remaining.compareTo(BigDecimal.ZERO) > 0
                ? remaining.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        invoice.setStatus(invoice.deriveStatusFromPayments());
        invoiceRepository.save(invoice);

        String description = "Estorno Recibo " + receipt.getReceiptNumber() + " - Motivo: " + reason;
        financeService.registerTransaction(receipt.getTreasuryAccount().getId(), "CREDIT", receipt.getAmountPaid(), description);
        eventPublisher.publishEvent(new ReceiptCancelledEvent(
                invoice.getCompany().getId(), receipt.getId(), receipt.getReceiptNumber(), LocalDate.now()));
        auditLogService.logCurrent("RECEIPT_CANCEL",
                "Recibo " + receipt.getReceiptNumber() + " anulado. Motivo: " + reason);
    }

    @Transactional
    public void cancelInvoice(Long invoiceId, String reason) { invoiceLifecycleService.cancelInvoice(invoiceId, reason); }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getAllInvoices() { return invoiceLifecycleService.getAllInvoices(); }

    @Transactional(readOnly = true)
    public List<Receipt> getAllReceipts() { return invoiceLifecycleService.getAllReceipts(); }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDTO> getInvoicePage(Long companyId, Integer page, Integer size) {
        return invoiceLifecycleService.getInvoicePage(companyId, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDTO> getPOSSalesPage(Long companyId, Integer page, Integer size, LocalDate from, LocalDate to) {
        return invoiceLifecycleService.getPOSSalesPage(companyId, page, size, from, to);
    }

    @Transactional(readOnly = true)
    public POSSalesSummaryDTO getPOSSalesSummary(Long companyId, LocalDate from, LocalDate to) {
        return invoiceLifecycleService.getPOSSalesSummary(companyId, from, to);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) { return invoiceLifecycleService.getInvoicesByCompany(companyId); }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> searchInvoices(String query) { return invoiceLifecycleService.searchInvoices(query); }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getPOSSalesByCompany(Long companyId) { return invoiceLifecycleService.getPOSSalesByCompany(companyId); }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) {
        return invoiceLifecycleService.getOutstandingInvoicesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<ReceiptDTO> getReceiptsByCompany(Long companyId) { return invoiceLifecycleService.getReceiptsByCompany(companyId); }

    public ReceiptDTO toDTO(Receipt r) { return invoiceLifecycleService.toDTO(r); }

    public InvoiceDTO toDTO(Invoice invoice) { return invoiceLifecycleService.toDTO(invoice); }

    @Transactional
    public InvoiceDTO createInvoiceFromQuotation(Quotation quotation) {
        return invoiceLifecycleService.createInvoiceFromQuotation(quotation);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() { return productCatalogService.getAllProducts(); }

    @Transactional(readOnly = true)
    public List<ProductDTO> getSellableProducts() { return productCatalogService.getSellableProducts(); }

    @Transactional(readOnly = true)
    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(
            String query, boolean availableOnly, Long warehouseId, Integer page, Integer size) {
        return productCatalogService.getPOSCatalogPage(query, availableOnly, warehouseId, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(
            String query, boolean availableOnly, Integer page, Integer size) {
        return productCatalogService.getPOSCatalogPage(query, availableOnly, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDTO> getProductsPage(String query, Long categoryId, Integer page, Integer size) {
        return productCatalogService.getProductsPage(query, categoryId, page, size);
    }

    @Transactional(readOnly = true)
    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode, Long warehouseId) {
        return productCatalogService.findPOSCatalogItemByBarcode(barcode, warehouseId);
    }

    @Transactional(readOnly = true)
    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode) {
        return productCatalogService.findPOSCatalogItemByBarcode(barcode);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String name, BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, String description) {
        return createProduct(sku, null, null, name, unitPrice, purchasePrice, minStock, 1, description);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name, BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, String description) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock, 1, description);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, String description) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock, unitsPerBox, null, description);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String description) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, ProductSaleType.UNIT.name(), true, description);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked, String description) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, saleType, stockTracked, null, description, null, null);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     Long taxRateId, String description) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description, null, null);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     Long taxRateId, String description,
                                     BigDecimal wholesalePrice, BigDecimal wholesaleMinQty) {
        return productCatalogService.createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description, wholesalePrice, wholesaleMinQty);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox,
            Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock, unitsPerBox,
                packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId,
                description, wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, null);
    }

    @Transactional
    public ProductDTO createProduct(String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox,
            Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg, Long businessUnitId) {
        return productCatalogService.createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId,
                description, wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, businessUnitId);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     Long taxRateId, String description) {
        return updateProduct(id, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description, null, null);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, String reference, String barcode, String name,
                                     BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     Long taxRateId, String description,
                                     BigDecimal wholesalePrice, BigDecimal wholesaleMinQty) {
        return productCatalogService.updateProduct(id, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description, wholesalePrice, wholesaleMinQty);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox,
            Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg) {
        return updateProduct(id, reference, barcode, name, unitPrice, purchasePrice, minStock, unitsPerBox,
                packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId,
                description, wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, null);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox,
            Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg, Long businessUnitId) {
        return productCatalogService.updateProduct(id, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId,
                description, wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, businessUnitId);
    }

    @Transactional
    public ProductDTO createProduct(CreateProductRequest r) { return productCatalogService.createProduct(r); }
    @Transactional
    public ProductDTO updateProduct(Long id, CreateProductRequest r) { return productCatalogService.updateProduct(id, r); }

    @Transactional(readOnly = true)
    public ProductDTO findProductByBarcode(String barcode) { return productCatalogService.findProductByBarcode(barcode); }

    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getAllCategories() { return productCatalogService.getAllCategories(); }

    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getActiveCategories() { return productCatalogService.getActiveCategories(); }

    @Transactional(readOnly = true)
    public List<TaxRateDTO> getActiveVatRates() { return productCatalogService.getActiveVatRates(); }

    public ProductDTO toDTO(Product p) { return productCatalogService.toDTO(p); }

    @Transactional
    public void updateProductImage(Long productId, byte[] imageData) { productCatalogService.updateProductImage(productId, imageData); }

    @Transactional
    public OrderDTO createOrder(CreateInvoiceRequest request) {
        return createOrder(new CreateOrderRequest(
                request.clientId(), request.customerName(), request.companyId(), request.warehouseId(), request.lines()));
    }

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) { return createOrder(request, request.effectiveKind()); }

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, OrderKind kind) { return orderLifecycleService.createOrder(request, kind); }

    @Transactional
    public OrderDTO updateOrder(Long orderId, UpdateOrderRequest request) { return orderLifecycleService.updateOrder(orderId, request); }

    @Transactional
    public OrderDTO placeOrder(Company company, Client client, Warehouse warehouse, String walkInName,
                               List<OrderLine> lines, OrderKind kind, OrderTerms terms) {
        return orderLifecycleService.placeOrder(company, client, warehouse, null, walkInName, lines, kind, terms);
    }

    @Transactional
    public InvoiceDTO billOrder(Long orderId) { return orderLifecycleService.billOrder(orderId); }

    @Transactional
    public InvoiceDTO billSeparatedOrder(Long orderId) { return orderLifecycleService.billSeparatedOrder(orderId); }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCompany(Long companyId) { return orderLifecycleService.getOrdersByCompany(companyId); }

    @Transactional(readOnly = true)
    public PageResponse<OrderDTO> getOrdersPageByCompany(Long companyId, Integer page, Integer size) {
        CurrentUserContext.requireCompany(companyId);
        return PageResponseMapper.from(
                orderRepository.findPageByCompanyId(companyId, PageQuery.of(page, size)),
                this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> searchCancellableOrders(String query) { return orderLifecycleService.searchCancellableOrders(query); }

    @Transactional
    public void cancelOrder(Long orderId, String reason) { orderLifecycleService.cancelOrder(orderId, reason); }

    @Transactional
    public OrderDTO approveOrder(Long orderId, String comments) { return orderLifecycleService.approveOrder(orderId, comments); }

    @Transactional
    public OrderDTO rejectOrder(Long orderId, String reason) { return orderLifecycleService.rejectOrder(orderId, reason); }

    @Transactional(readOnly = true)
    public List<OrderDTO> getPendingOrdersByCompany(Long companyId) { return orderLifecycleService.getPendingOrdersByCompany(companyId); }

    @Transactional(readOnly = true)
    public List<OrderDTO> searchPendingOrders(String query) { return orderLifecycleService.searchPendingOrders(query); }

    @Transactional
    public Order createReplenishmentRecord(StockTransferDTO transfer) { return orderLifecycleService.createReplenishmentRecord(transfer); }

    public OrderDTO toDTO(Order order) { return orderLifecycleService.toDTO(order); }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) { return orderLifecycleService.getOrderById(orderId); }

    @Transactional
    public OrderDTO markOrderPrinted(Long orderId, String operator) { return orderLifecycleService.markOrderPrinted(orderId, operator); }
}
