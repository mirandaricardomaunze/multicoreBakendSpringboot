package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.validation.TaxIdValidator;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.*;
import com.phcpro.modules.comercial.model.*;
import com.phcpro.modules.comercial.repository.*;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.model.TreasuryAccount;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComercialService {

    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final com.phcpro.modules.comercial.repository.ProductCategoryRepository productCategoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final ApprovalService approvalService;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final ReceiptRepository receiptRepository;
    private final FinanceService financeService;
    private final TreasuryAccountRepository treasuryAccountRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public ComercialService(
            ClientRepository clientRepository,
            ProductRepository productRepository,
            com.phcpro.modules.comercial.repository.ProductCategoryRepository productCategoryRepository,
            InvoiceRepository invoiceRepository,
            @Lazy ApprovalService approvalService,
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository,
            @Lazy InventoryService inventoryService,
            ReceiptRepository receiptRepository,
            @Lazy FinanceService financeService,
            TreasuryAccountRepository treasuryAccountRepository,
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.approvalService = approvalService;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.receiptRepository = receiptRepository;
        this.financeService = financeService;
        this.treasuryAccountRepository = treasuryAccountRepository;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }


    @Transactional
    public ClientDTO createClient(String name, String taxId, String email, String address) {
        TaxIdValidator.validate(taxId);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (clientRepository.findByTaxIdAndCompaniesId(taxId, companyId).isPresent()) {
            throw new BusinessRuleException("Já existe um cliente registado com este NUIT/NIF.");
        }
        Client sharedClient = clientRepository.findByTaxId(taxId).orElse(null);
        if (sharedClient != null) {
            sharedClient.getCompanies().add(companyRepository.getReferenceById(companyId));
            sharedClient = clientRepository.save(sharedClient);
            return new ClientDTO(sharedClient.getId(), sharedClient.getName(), sharedClient.getTaxId(),
                    sharedClient.getEmail(), sharedClient.getAddress());
        }
        Client client = new Client();
        client.setName(name);
        client.setTaxId(taxId);
        client.setEmail(email);
        client.setAddress(address);
        client.setCreatedBy("SYSTEM");
        client.getCompanies().add(companyRepository.getReferenceById(companyId));
        client = clientRepository.save(client);
        return new ClientDTO(client.getId(), client.getName(), client.getTaxId(), client.getEmail(), client.getAddress());
    }

    @Transactional
    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address) {
        Client client = clientRepository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        TaxIdValidator.validate(taxId);
        if (!client.getTaxId().equals(taxId)) {
            clientRepository.findByTaxIdAndCompaniesId(taxId, CurrentUserContext.getCurrentCompanyId()).ifPresent(existing -> {
                throw new BusinessRuleException("Já existe outro cliente registado com este NUIT/NIF.");
            });
        }
        client.setName(name);
        client.setTaxId(taxId);
        client.setEmail(email);
        client.setAddress(address);
        client = clientRepository.save(client);
        return new ClientDTO(client.getId(), client.getName(), client.getTaxId(), client.getEmail(), client.getAddress());
    }

    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        try {
            clientRepository.delete(client);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessRuleException(
                    "Não é possível eliminar este cliente porque já tem documentos associados (faturas, encomendas, etc.).");
        }
    }

    @Transactional
    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        Client client = clientRepository.findByIdAndCompaniesId(request.clientId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));

        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setCompany(company);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.PENDING_APPROVAL);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        boolean hasHighDiscount = false;

        for (CreateInvoiceLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(product.getUnitPrice());
            line.setTaxRate(lineReq.taxRate());

            BigDecimal discountPct = lineReq.discountPercentage();
            if (discountPct == null) {
                discountPct = BigDecimal.ZERO;
            }
            line.setDiscountPercentage(discountPct);
            if (discountPct.compareTo(BigDecimal.valueOf(10)) > 0) {
                hasHighDiscount = true;
            }

            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    product.getUnitPrice(), lineReq.quantity(), discountPct, lineReq.taxRate());

            line.setLineTotal(amounts.total());
            invoice.addLine(line);

            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));

        if (hasHighDiscount) {
            invoice.setStatus(InvoiceStatus.PENDING_DISCOUNT_APPROVAL);
        }

        // Número fiscal sequencial e sem saltos (série FT).
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));
        invoice.setCreatedBy("SYSTEM");

        invoice = invoiceRepository.save(invoice);

        // Submit to Approvals Engine
        String approvalDesc = String.format("Fatura %s para %s - Total: %s MT%s", 
                invoice.getInvoiceNumber(), client.getName(), invoice.getTotalAmount(),
                hasHighDiscount ? " (Aprovação de Desconto Especial >10%)" : "");
        approvalService.submitRequest("INVOICE", invoice.getId(), invoice.getTotalAmount(), approvalDesc);

        return toDTO(invoice);
    }

    @Transactional
    public Receipt createReceipt(Long invoiceId, Long treasuryAccountId, String paymentMethod, BigDecimal amountPaid) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        if (invoice.getStatus() != InvoiceStatus.APPROVED) {
            throw new BusinessRuleException("Apenas faturas no estado APROVADA podem ter recibo. Estado atual: " + invoice.getStatus());
        }

        TreasuryAccount account = treasuryAccountRepository.findByIdAndCompanyId(
                        treasuryAccountId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Conta de tesouraria não encontrada."));

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
        receipt.setCreatedBy("SYSTEM");

        receipt = receiptRepository.save(receipt);

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        // Record financial cash inflow (DEBIT)
        String description = "Recebimento Fatura " + invoice.getInvoiceNumber() + " via " + paymentMethod + " (Recibo " + receiptNum + ")";
        financeService.registerTransaction(treasuryAccountId, "DEBIT", amountPaid, description);

        return receipt;
    }

    @Transactional
    public void cancelReceipt(Long receiptId, String reason) {
        PermissionGuard.requireManagerOrAdmin("anular recibo");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É necessário indicar o motivo da anulação do recibo.");
        }

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));

        CurrentUserContext.requireCompany(receipt.getCompany().getId());
        if ("CANCELLED".equals(receipt.getStatus())) {
            throw new BusinessRuleException("Este recibo já se encontra anulado.");
        }

        receipt.setStatus("CANCELLED");
        receipt.setCancellationReason(reason);
        receiptRepository.save(receipt);

        Invoice invoice = receipt.getInvoice();
        invoice.setStatus(InvoiceStatus.APPROVED); // Revert invoice status to approved so it can be paid or cancelled
        invoiceRepository.save(invoice);

        // Refund cash (CREDIT)
        String description = "Estorno Recibo " + receipt.getReceiptNumber() + " - Motivo: " + reason;
        financeService.registerTransaction(receipt.getTreasuryAccount().getId(), "CREDIT", receipt.getAmountPaid(), description);
        auditLogService.logCurrent("RECEIPT_CANCEL",
                "Recibo " + receipt.getReceiptNumber() + " anulado. Motivo: " + reason);
    }

    @Transactional
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

        // Revert stock movement if the invoice was approved or paid (which deducted stock)
        if (invoice.getStatus() == InvoiceStatus.APPROVED || invoice.getStatus() == InvoiceStatus.PAID) {
            invoice.getLines().forEach(line -> {
                String desc = String.format("Estorno Fatura %s - Motivo: %s", invoice.getInvoiceNumber(), reason);
                inventoryService.registerMovement(
                        line.getProduct(),
                        invoice.getWarehouse(),
                        line.getQuantity(), // Positive quantity to replenish stock
                        "REVERSAL",
                        line.getBatchNumber(),
                        line.getSerialNumber(),
                        desc
                );
            });
        }

        // Cancel associated receipts (if any) and refund cash
        List<Receipt> receipts = receiptRepository.findByCompanyId(CurrentUserContext.getCurrentCompanyId()).stream()
                .filter(r -> r.getInvoice().getId().equals(invoiceId) && !"CANCELLED".equals(r.getStatus()))
                .collect(Collectors.toList());

        for (Receipt r : receipts) {
            r.setStatus("CANCELLED");
            r.setCancellationReason("Fatura anulada: " + reason);
            receiptRepository.save(r);

            // Refund cash (CREDIT)
            String description = "Estorno automático por Fatura Anulada " + invoice.getInvoiceNumber();
            financeService.registerTransaction(r.getTreasuryAccount().getId(), "CREDIT", r.getAmountPaid(), description);
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancellationReason(reason);
        invoiceRepository.save(invoice);
        auditLogService.logCurrent("INVOICE_CANCEL",
                "Fatura " + invoice.getInvoiceNumber() + " anulada. Motivo: " + reason);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findByCompanyId(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Receipt> getAllReceipts() {
        return receiptRepository.findByCompanyId(CurrentUserContext.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository.findByCompanyId(companyId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Histórico de vendas POS (canal {@code SalesChannel.POS}), ordenadas da mais recente para a
     * mais antiga. Inclui {@code createdBy} = operador da caixa que efectuou a venda.
     */
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getPOSSalesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository
                .findByCompanyIdAndSalesChannelOrderByCreatedAtDesc(companyId, SalesChannel.POS)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Faturas com saldo em dívida — fiados ou pagamentos parciais.
     * Inclui status APPROVED (cobrável) ou PARTIALLY_PAID, onde amountPaid < totalAmount.
     */
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.APPROVED
                          || i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .filter(i -> {
                    BigDecimal paid = i.getAmountPaid() == null ? BigDecimal.ZERO : i.getAmountPaid();
                    BigDecimal total = i.getTotalAmount() == null ? BigDecimal.ZERO : i.getTotalAmount();
                    return paid.compareTo(total) < 0;
                })
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Receipt> getReceiptsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return receiptRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients() {
        return clientRepository.findDistinctByCompaniesIdOrderByName(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(c -> new ClientDTO(c.getId(), c.getName(), c.getTaxId(), c.getEmail(), c.getAddress()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findDistinctByCompaniesIdOrderByName(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, null, description);
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
                                     int unitsPerBox, Long categoryId, String saleType, boolean stockTracked,
                                     String description) {
        String cleanReference = normalizeOptional(reference);
        String cleanBarcode = normalizeOptional(barcode);
        ProductSaleType parsedSaleType = parseSaleType(saleType);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (productRepository.findBySkuAndCompaniesId(sku, companyId).isPresent()) {
            throw new BusinessRuleException("Já existe um produto com o SKU indicado.");
        }
        Product sharedProduct = productRepository.findBySku(sku).orElse(null);
        if (sharedProduct != null) {
            sharedProduct.getCompanies().add(companyRepository.getReferenceById(companyId));
            return toDTO(productRepository.save(sharedProduct));
        }
        if (cleanReference != null && productRepository.findByReferenceAndCompaniesId(cleanReference, companyId).isPresent()) {
            throw new BusinessRuleException("Ja existe um produto com a referencia indicada.");
        }
        if (cleanBarcode != null && productRepository.findByBarcodeAndCompaniesId(cleanBarcode, companyId).isPresent()) {
            throw new BusinessRuleException("Ja existe um produto com o codigo de barras indicado.");
        }
        Product product = new Product();
        product.setSku(sku);
        product.setReference(cleanReference);
        product.setBarcode(cleanBarcode);
        product.setName(name);
        product.setUnitPrice(unitPrice);
        product.setPurchasePrice(purchasePrice);
        product.setMinStock(minStock);
        product.setUnitsPerBox(unitsPerBox <= 0 ? 1 : unitsPerBox);
        product.setSaleType(parsedSaleType);
        product.setStockTracked(parsedSaleType != ProductSaleType.SERVICE && stockTracked);
        product.setDescription(description);
        if (categoryId != null) {
            product.setCategory(productCategoryRepository.findByIdAndCompaniesId(categoryId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada.")));
        }
        product.setCreatedBy("SYSTEM");
        product.getCompanies().add(companyRepository.getReferenceById(companyId));
        product = productRepository.save(product);
        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO findProductByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        return productRepository.findByBarcodeAndCompaniesId(barcode.trim(), CurrentUserContext.getCurrentCompanyId())
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> getAllCategories() {
        return productCategoryRepository.findDistinctByCompaniesIdOrderByNameAsc(CurrentUserContext.getCurrentCompanyId()).stream()
                .map(c -> new com.phcpro.modules.comercial.dto.ProductCategoryDTO(
                        c.getId(), c.getCode(), c.getName(), c.getColorHex(), c.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> getActiveCategories() {
        return productCategoryRepository.findDistinctByCompaniesIdAndActiveTrueOrderByNameAsc(
                        CurrentUserContext.getCurrentCompanyId()).stream()
                .map(c -> new com.phcpro.modules.comercial.dto.ProductCategoryDTO(
                        c.getId(), c.getCode(), c.getName(), c.getColorHex(), c.isActive()))
                .toList();
    }

    public ProductDTO toDTO(Product p) {
        return new ProductDTO(
                p.getId(),
                p.getSku(),
                p.getReference(),
                p.getBarcode(),
                p.getName(),
                p.getUnitPrice(),
                p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO,
                p.getMinStock() != null ? p.getMinStock() : BigDecimal.ZERO,
                p.getUnitsPerBox() <= 0 ? 1 : p.getUnitsPerBox(),
                p.getSaleType() != null ? p.getSaleType().name() : ProductSaleType.UNIT.name(),
                p.isStockTracked(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getDescription()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductSaleType parseSaleType(String value) {
        if (value == null || value.isBlank()) {
            return ProductSaleType.UNIT;
        }
        try {
            return ProductSaleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de venda inválido. Use UNIT, BOX, WEIGHT ou SERVICE.");
        }
    }

    public InvoiceDTO toDTO(Invoice invoice) {
        List<InvoiceLineDTO> lines = invoice.getLines().stream().map(line -> new InvoiceLineDTO(
                line.getId(),
                line.getProduct().getId(),
                line.getProduct().getName(),
                line.getProduct().getSku(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getTaxRate(),
                line.getLineTotal(),
                line.getDiscountPercentage(),
                line.getBatchNumber(),
                line.getSerialNumber()
        )).collect(Collectors.toList());

        return new InvoiceDTO(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getClient().getId(),
                invoice.getClient().getName(),
                invoice.getClient().getTaxId(),
                invoice.getTotalBeforeTax(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO,
                invoice.getStatus(),
                invoice.getRejectionReason(),
                lines,
                invoice.getCreatedAt() != null ? invoice.getCreatedAt() : LocalDateTime.now(),
                invoice.getCreatedBy()
        );
    }

    @Transactional
    public OrderDTO createOrder(CreateInvoiceRequest request) {
        return createOrder(new CreateOrderRequest(
                request.clientId(), null,
                request.companyId(), request.warehouseId(), request.lines()));
    }

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        Client client;
        if (request.clientId() != null) {
            client = clientRepository.findByIdAndCompaniesId(request.clientId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        } else {
            client = walkInClientProvider.getOrCreate();
        }
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));

        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Order order = new Order();
        order.setClient(client);
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setStatus("PENDING");
        if (request.walkInName() != null && !request.walkInName().isBlank()) {
            order.setWalkInName(request.walkInName().trim());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateInvoiceLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            OrderLine line = new OrderLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(product.getUnitPrice());
            line.setTaxRate(lineReq.taxRate());

            BigDecimal discountPct = lineReq.discountPercentage();
            if (discountPct == null) {
                discountPct = BigDecimal.ZERO;
            }
            line.setDiscountPercentage(discountPct);

            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    product.getUnitPrice(), lineReq.quantity(), discountPct, lineReq.taxRate());

            line.setLineTotal(amounts.total());
            order.addLine(line);

            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        order.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        order.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));

        // Número sequencial da encomenda (série EC, independente da série de faturas).
        order.setOrderNumber(documentNumberService.next(DocumentSeries.ORDER));
        order.setCreatedBy("SYSTEM");

        order = orderRepository.save(order);

        return toDTO(order);
    }

    @Transactional
    public InvoiceDTO billOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));

        CurrentUserContext.requireCompany(order.getCompany().getId());
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Apenas encomendas no estado PENDENTE podem ser faturadas.");
        }

        Invoice invoice = new Invoice();
        invoice.setClient(order.getClient());
        invoice.setCompany(order.getCompany());
        invoice.setWarehouse(order.getWarehouse());
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setSalesChannel(SalesChannel.ORDER);
        invoice.setTotalBeforeTax(order.getTotalBeforeTax());
        invoice.setTaxAmount(order.getTaxAmount());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setCreatedBy("SYSTEM");

        // A fatura tem o seu próprio número fiscal gapless (série FT), independente
        // do número da encomenda — exigência da AT (a série FT não pode ter saltos).
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));

        for (OrderLine orderLine : order.getLines()) {
            InvoiceLine invoiceLine = new InvoiceLine();
            invoiceLine.setProduct(orderLine.getProduct());
            invoiceLine.setQuantity(orderLine.getQuantity());
            invoiceLine.setUnitPrice(orderLine.getUnitPrice());
            invoiceLine.setTaxRate(orderLine.getTaxRate());
            invoiceLine.setDiscountPercentage(orderLine.getDiscountPercentage());
            invoiceLine.setLineTotal(orderLine.getLineTotal());
            invoiceLine.setBatchNumber(orderLine.getBatchNumber());
            invoiceLine.setSerialNumber(orderLine.getSerialNumber());
            invoice.addLine(invoiceLine);

            // Deduct stock for each line in the warehouse
            String desc = String.format("Saída Fatura %s (Encomenda %s) - Cliente %s", 
                    invoice.getInvoiceNumber(), order.getOrderNumber(), order.getClient().getName());
            inventoryService.registerMovement(
                    orderLine.getProduct(),
                    order.getWarehouse(),
                        orderLine.getQuantity().negate(),
                    "SALE",
                    orderLine.getBatchNumber(),
                    orderLine.getSerialNumber(),
                    desc
            );
        }

        invoice = invoiceRepository.save(invoice);

        order.setStatus("BILLED");
        order.setInvoiceId(invoice.getId());
        orderRepository.save(order);

        return toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return orderRepository.findByCompanyId(companyId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Devolve as encomendas PENDENTES (status PENDING e ainda sem invoiceId) — as únicas que
     * podem ser convertidas em fatura. Usado pelo diálogo "Faturar Encomenda" no tab das Faturas
     * para impedir dupla faturação a montante (a validação final está em {@link #billOrder}).
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getPendingOrdersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return orderRepository
                .findByCompanyIdAndStatusAndInvoiceIdIsNull(companyId, "PENDING")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public OrderDTO toDTO(Order order) {
        List<OrderLineDTO> lines = order.getLines().stream().map(line -> new OrderLineDTO(
                line.getId(),
                line.getProduct().getId(),
                line.getProduct().getName(),
                line.getProduct().getSku(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getTaxRate(),
                line.getLineTotal(),
                line.getDiscountPercentage(),
                line.getBatchNumber(),
                line.getSerialNumber()
        )).collect(Collectors.toList());

        return new OrderDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getClient().getTaxId(),
                order.getWalkInName(),
                order.getTotalBeforeTax(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getInvoiceId(),
                lines,
                order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now(),
                order.getPrintedAt(),
                order.getPrintCount(),
                order.getLastPrintedBy()
        );
    }

    /**
     * Devolve uma encomenda pelo ID com todas as linhas carregadas. Usado pelo painel
     * de gestão para o ecrã de detalhes.
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        // Inicializa as linhas antes de sair da transacção
        CurrentUserContext.requireCompany(order.getCompany().getId());
        order.getLines().size();
        return toDTO(order);
    }

    /**
     * Marca a encomenda como impressa: incrementa contador, regista timestamp e operador.
     * Idempotente em sentido lato — pode ser chamado várias vezes; cada chamada é
     * uma nova impressão registada.
     */
    @Transactional
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
