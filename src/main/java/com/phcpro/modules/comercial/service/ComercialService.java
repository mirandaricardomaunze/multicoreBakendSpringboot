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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final com.phcpro.modules.fiscal.repository.TaxRateRepository taxRateRepository;

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
            AuditLogService auditLogService,
            com.phcpro.modules.fiscal.repository.TaxRateRepository taxRateRepository
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
        this.taxRateRepository = taxRateRepository;
    }


    @Transactional
    public ClientDTO createClient(SaveClientRequest request) {
        String taxId = request.taxId();
        TaxIdValidator.validate(taxId);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (clientRepository.findByTaxIdAndCompaniesId(taxId, companyId).isPresent()) {
            throw new BusinessRuleException("Já existe um cliente registado com este NUIT/NIF.");
        }
        Client sharedClient = clientRepository.findByTaxId(taxId).orElse(null);
        if (sharedClient != null) {
            sharedClient.getCompanies().add(companyRepository.getReferenceById(companyId));
            sharedClient = clientRepository.save(sharedClient);
            return toDTO(sharedClient);
        }
        Client client = new Client();
        client.setName(request.name());
        client.setTaxId(taxId);
        client.setEmail(request.email());
        client.setAddress(request.address());
        client.setPaymentTermsDays(request.effectivePaymentTermsDays());
        client.setCreatedBy("SYSTEM");
        client.getCompanies().add(companyRepository.getReferenceById(companyId));
        client = clientRepository.save(client);
        return toDTO(client);
    }

    @Transactional
    public ClientDTO updateClient(Long id, SaveClientRequest request) {
        Client client = clientRepository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        String taxId = request.taxId();
        TaxIdValidator.validate(taxId);
        if (!client.getTaxId().equals(taxId)) {
            clientRepository.findByTaxIdAndCompaniesId(taxId, CurrentUserContext.getCurrentCompanyId()).ifPresent(existing -> {
                throw new BusinessRuleException("Já existe outro cliente registado com este NUIT/NIF.");
            });
        }
        client.setName(request.name());
        client.setTaxId(taxId);
        client.setEmail(request.email());
        client.setAddress(request.address());
        // Alterar o prazo só afecta faturas FUTURAS: o vencimento das já emitidas está gravado
        // no documento, e uma dívida não muda de data por o acordo ter mudado hoje.
        client.setPaymentTermsDays(request.effectivePaymentTermsDays());
        client = clientRepository.save(client);
        return toDTO(client);
    }

    /** Conversão única de cliente para DTO — evita cinco cópias da mesma lista de campos. */
    private ClientDTO toDTO(Client client) {
        return new ClientDTO(client.getId(), client.getName(), client.getTaxId(),
                client.getEmail(), client.getAddress(), client.effectivePaymentTermsDays());
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
        // Emitir fatura é operação directa de quem tem perfil autorizado (atribuído pelo admin) —
        // não passa pela Engine de Aprovações. Só o desconto sensível (>10%) é que exige aprovação.
        PermissionGuard.requireManagerOrAdmin("emitir fatura");
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
        // Vencimento: o do pedido, ou o prazo acordado com o cliente. A regra vive no domínio
        // (Invoice.assignDueDate) — as três portas que emitem fatura chamam a mesma.
        invoice.assignDueDate(LocalDate.now(), request.dueDate());
        // Estado definido no fim: APPROVED (directa) ou PENDING_DISCOUNT_APPROVAL (desconto >10%).

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        boolean hasHighDiscount = false;

        for (CreateInvoiceLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            // Preço efectivo: aplica grosso quando a quantidade atinge a mínima de grosso.
            BigDecimal unitPrice = product.effectiveUnitPrice(lineReq.quantity());

            // IVA: a taxa é do ARTIGO, nunca a que o cliente HTTP envia — senão o mesmo produto
            // saía isento no POS e a 16% na fatura. Ver docs/IVA_TAXA_CANONICA_SPEC.md.
            BigDecimal taxRate = product.effectiveTaxRate();

            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);

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
                    unitPrice, lineReq.quantity(), discountPct, taxRate);

            line.setLineTotal(amounts.total());
            invoice.addLine(line);

            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));

        // Número fiscal sequencial e sem saltos (série FT).
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));
        invoice.setCreatedBy(CurrentUserContext.getUsername());

        if (hasHighDiscount) {
            // Desconto sensível (>10%) continua a exigir aprovação do gerente. O stock só baixa
            // na aprovação (InvoiceApprovalCallback.onApproved).
            invoice.setStatus(InvoiceStatus.PENDING_DISCOUNT_APPROVAL);
            invoice = invoiceRepository.save(invoice);
            String approvalDesc = String.format(
                    "Fatura %s para %s - Total: %s MT (Aprovação de Desconto Especial >10%%)",
                    invoice.getInvoiceNumber(), client.getName(), invoice.getTotalAmount());
            approvalService.submitRequest("INVOICE", invoice.getId(), invoice.getTotalAmount(), approvalDesc);
        } else {
            // Faturação directa: emite já APROVADA e baixa o stock no acto (como POS/encomenda).
            invoice.setStatus(InvoiceStatus.APPROVED);
            invoice = invoiceRepository.save(invoice);
            deductStockForInvoice(invoice);
            auditLogService.logCurrent("INVOICE_ISSUE",
                    "Fatura " + invoice.getInvoiceNumber() + " emitida e aprovada para "
                            + client.getName() + ". Total: " + invoice.getTotalAmount() + " MT.");
        }

        return toDTO(invoice);
    }

    /** Baixa de stock (saída SALE) por cada linha da fatura, no armazém da fatura. */
    private void deductStockForInvoice(Invoice invoice) {
        invoice.getLines().forEach(line -> {
            String desc = String.format("Saída Fatura %s - Cliente %s",
                    invoice.getInvoiceNumber(), invoice.getClient().getName());
            inventoryService.registerMovement(
                    line.getProduct(),
                    invoice.getWarehouse(),
                    line.getQuantity().negate(),
                    "SALE",
                    line.getBatchNumber(),
                    line.getSerialNumber(),
                    desc
            );
        });
    }

    /**
     * Emite recibo de recebimento. Devolve o <b>DTO</b>, não a entidade: a conversão tem de
     * acontecer dentro da transacção, senão o proxy lazy do cliente rebenta na fronteira HTTP
     * ({@code open-in-view=false}) — o recibo ficava gravado e o utilizador via um erro.
     */
    @Transactional
    public ReceiptDTO createReceipt(Long invoiceId, Long treasuryAccountId, String paymentMethod, BigDecimal amountPaid) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        if (!invoice.getStatus().isCollectable()) {
            throw new BusinessRuleException("Apenas faturas por cobrar podem ter recibo. Estado atual: " + invoice.getStatus());
        }
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor do recibo tem de ser positivo.");
        }
        // O recibo nunca pode exceder o que falta receber — senão entrava dinheiro a mais na
        // tesouraria e a fatura ficava com um "pago" superior ao total.
        BigDecimal outstanding = invoice.outstandingAmount();
        if (amountPaid.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Valor do recibo (" + amountPaid
                    + ") excede o saldo em dívida (" + outstanding + ").");
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

        // Acumula o recebido e deriva o estado pela regra única: um recibo parcial deixa a
        // fatura PARTIALLY_PAID (o saldo continua a aparecer nas contas correntes), não PAID.
        invoice.setAmountPaid(invoice.getAmountPaid().add(amountPaid).setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(invoice.deriveStatusFromPayments());
        invoiceRepository.save(invoice);

        // Record financial cash inflow (DEBIT)
        String description = "Recebimento Fatura " + invoice.getInvoiceNumber() + " via " + paymentMethod + " (Recibo " + receiptNum + ")";
        financeService.registerTransaction(treasuryAccountId, "DEBIT", amountPaid, description);

        return toDTO(receipt);
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

        // Devolve só o valor deste recibo. Com vários recibos na mesma fatura, anular um não
        // pode apagar os outros — o estado volta a derivar do que continua pago.
        Invoice invoice = receipt.getInvoice();
        BigDecimal remaining = invoice.getAmountPaid().subtract(receipt.getAmountPaid());
        invoice.setAmountPaid(remaining.compareTo(BigDecimal.ZERO) > 0
                ? remaining.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        invoice.setStatus(invoice.deriveStatusFromPayments());
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
     * Localiza faturas da empresa activa por nº de fatura OU nome de cliente (pesquisa parcial,
     * sem distinção de maiúsculas). Usado pelos diálogos de Nota de Crédito/Débito para encontrar
     * o documento de origem ao estilo PHC. {@code query} vazio devolve todas as faturas.
     */
    @Transactional(readOnly = true)
    public List<InvoiceDTO> searchInvoices(String query) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> matches(i.getInvoiceNumber(), query)
                        || (i.getClient() != null && matches(i.getClient().getName(), query)))
                .map(this::toDTO)
                .collect(Collectors.toList());
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
     * Faturas com saldo em dívida — fiados ou pagamentos parciais, das mais atrasadas para as
     * mais recentes (é por aí que se começa a cobrar).
     *
     * <p>Usa os predicados canónicos ({@code isCollectable} + {@code outstandingAmount}) em vez de
     * repetir a comparação de estados e de valores, que era a terceira cópia da mesma regra.
     */
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> i.getStatus().isCollectable())
                .filter(i -> i.outstandingAmount().signum() > 0)
                .map(this::toDTO)
                .sorted(Comparator.comparingInt(InvoiceDTO::daysOverdue).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiptDTO> getReceiptsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        // Converter aqui dentro, pela mesma razão de createReceipt.
        return receiptRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    /** Recibo → DTO (achata fatura/cliente) para a fronteira HTTP. */
    public com.phcpro.modules.comercial.dto.ReceiptDTO toDTO(Receipt r) {
        return new com.phcpro.modules.comercial.dto.ReceiptDTO(
                r.getId(),
                r.getReceiptNumber(),
                r.getInvoice() != null ? r.getInvoice().getInvoiceNumber() : null,
                r.getInvoice() != null && r.getInvoice().getClient() != null
                        ? r.getInvoice().getClient().getName() : null,
                r.getAmountPaid(),
                r.getPaymentMethod(),
                r.getStatus(),
                r.getReceiptDate());
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients() {
        return clientRepository.findDistinctByCompaniesIdOrderByName(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findDistinctByCompaniesIdOrderByName(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Catálogo vendável do POS: exclui os produtos esgotados. Um produto aparece se não controla
     * stock (ex.: serviços) ou se tem stock disponível num armazém de venda. Ver
     * {@link InventoryService#getInStockProductIdsForSale(Long)}.
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getSellableProducts() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        java.util.Set<Long> inStock = inventoryService.getInStockProductIdsForSale(companyId);
        return productRepository.findDistinctByCompaniesIdOrderByName(companyId)
                .stream()
                .map(this::toDTO)
                .filter(p -> !p.stockTracked() || inStock.contains(p.id()))
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
        product.setWholesalePrice(wholesalePrice);
        product.setWholesaleMinQty(wholesaleMinQty);
        product.setSaleType(parsedSaleType);
        product.setStockTracked(parsedSaleType != ProductSaleType.SERVICE && stockTracked);
        product.setDescription(description);
        if (categoryId != null) {
            product.setCategory(productCategoryRepository.findByIdAndCompaniesId(categoryId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada.")));
        }
        if (taxRateId != null) {
            product.setTaxRate(taxRateRepository.findByIdAndCompaniesId(taxRateId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Taxa de IVA não encontrada.")));
        }
        product.setCreatedBy("SYSTEM");
        product.getCompanies().add(companyRepository.getReferenceById(companyId));
        product = productRepository.save(product);
        return toDTO(product);
    }

    /**
     * Actualiza os dados de um produto da empresa activa. O SKU é imutável (identidade do artigo);
     * referência e código de barras revalidam unicidade excluindo o próprio. Não toca no stock — só
     * no cadastro. `unitsPerBox` afecta apenas a conversão para caixas no inventário (o stock
     * continua em unidades).
     */
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
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Product product = productRepository.findByIdAndCompaniesId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        String cleanReference = normalizeOptional(reference);
        String cleanBarcode = normalizeOptional(barcode);
        if (cleanReference != null) {
            productRepository.findByReferenceAndCompaniesId(cleanReference, companyId)
                    .filter(p -> !p.getId().equals(id))
                    .ifPresent(p -> { throw new BusinessRuleException("Ja existe outro produto com a referencia indicada."); });
        }
        if (cleanBarcode != null) {
            productRepository.findByBarcodeAndCompaniesId(cleanBarcode, companyId)
                    .filter(p -> !p.getId().equals(id))
                    .ifPresent(p -> { throw new BusinessRuleException("Ja existe outro produto com o codigo de barras indicado."); });
        }
        ProductSaleType parsedSaleType = parseSaleType(saleType);
        product.setReference(cleanReference);
        product.setBarcode(cleanBarcode);
        product.setName(name);
        product.setUnitPrice(unitPrice);
        product.setPurchasePrice(purchasePrice);
        product.setMinStock(minStock);
        product.setUnitsPerBox(unitsPerBox <= 0 ? 1 : unitsPerBox);
        product.setWholesalePrice(wholesalePrice);
        product.setWholesaleMinQty(wholesaleMinQty);
        product.setSaleType(parsedSaleType);
        product.setStockTracked(parsedSaleType != ProductSaleType.SERVICE && stockTracked);
        product.setDescription(normalizeOptional(description));
        if (categoryId != null) {
            product.setCategory(productCategoryRepository.findByIdAndCompaniesId(categoryId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada.")));
        } else {
            product.setCategory(null);
        }
        if (taxRateId != null) {
            product.setTaxRate(taxRateRepository.findByIdAndCompaniesId(taxRateId, companyId)
                    .orElseThrow(() -> new BusinessRuleException("Taxa de IVA não encontrada.")));
        } else {
            product.setTaxRate(null);
        }
        product = productRepository.save(product);
        auditLogService.logCurrent("PRODUCT_UPDATE",
                "Produto " + product.getSku() + " (" + product.getName() + ") actualizado.");
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

    /** Taxas de IVA activas da empresa (apenas tipos IVA_*), para escolha no cadastro de produto. */
    @Transactional(readOnly = true)
    public List<com.phcpro.modules.fiscal.dto.TaxRateDTO> getActiveVatRates() {
        return taxRateRepository.findDistinctByCompaniesIdAndActiveTrueOrderByTypeAscRateDesc(
                        CurrentUserContext.getCurrentCompanyId()).stream()
                .filter(t -> t.getType() != null && t.getType().name().startsWith("IVA"))
                .map(t -> new com.phcpro.modules.fiscal.dto.TaxRateDTO(
                        t.getId(), t.getCode(), t.getName(), t.getType().name(),
                        t.getRate(), t.getLegalBasis(), t.isActive()))
                .toList();
    }

    public ProductDTO toDTO(Product p) {
        // IVA dinâmico: usa a taxa configurada no produto; sem taxa explícita aplica-se a padrão.
        boolean hasRate = p.getTaxRate() != null;
        BigDecimal effectiveRate = hasRate ? p.getTaxRate().getRate()
                : com.phcpro.architecture.pricing.TaxRates.STANDARD_VAT;
        Long taxRateId = hasRate ? p.getTaxRate().getId() : null;
        String taxRateLabel = hasRate ? p.getTaxRate().getName() : "IVA Normal (16%)";
        return new ProductDTO(
                p.getId(),
                p.getSku(),
                p.getReference(),
                p.getBarcode(),
                p.getName(),
                p.getUnitPrice(),
                p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO,
                p.getMinStock() != null ? p.getMinStock() : BigDecimal.ZERO,
                p.getWholesalePrice(),
                p.getWholesaleMinQty(),
                p.getUnitsPerBox() <= 0 ? 1 : p.getUnitsPerBox(),
                p.getSaleType() != null ? p.getSaleType().name() : ProductSaleType.UNIT.name(),
                p.isStockTracked(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                taxRateId,
                effectiveRate,
                taxRateLabel,
                p.getDescription(),
                p.getImageData()
        );
    }

    /** Guarda/actualiza a imagem (thumbnail) de um produto da empresa actual. */
    @Transactional
    public void updateProductImage(Long productId, byte[] imageData) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Product product = productRepository.findById(productId)
                .filter(p -> p.belongsToCompany(companyId))
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        product.setImageData(imageData);
        productRepository.save(product);
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
                invoice.getCreatedBy(),
                invoice.effectiveDueDate(),
                invoice.daysOverdue(LocalDate.now()),
                invoice.agingBucket(LocalDate.now())
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
        // Encomenda passa pela Engine de Aprovações antes de ser faturável (ver OrderApprovalCallback).
        order.setStatus("PENDING_APPROVAL");
        if (request.walkInName() != null && !request.walkInName().isBlank()) {
            order.setWalkInName(request.walkInName().trim());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateInvoiceLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            BigDecimal unitPrice = product.effectiveUnitPrice(lineReq.quantity());

            // Mesma regra da fatura: a taxa vem do artigo (a encomenda é faturada mais tarde e a
            // fatura herda esta linha, por isso divergir aqui contaminava também a fatura).
            BigDecimal taxRate = product.effectiveTaxRate();

            OrderLine line = new OrderLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);

            BigDecimal discountPct = lineReq.discountPercentage();
            if (discountPct == null) {
                discountPct = BigDecimal.ZERO;
            }
            line.setDiscountPercentage(discountPct);

            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    unitPrice, lineReq.quantity(), discountPct, taxRate);

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
        order.setCreatedBy(CurrentUserContext.getUsername());

        order = orderRepository.save(order);

        // Submete à Engine de Aprovações. O encaminhamento por valor (auto ≤50 / MANAGER / ADMIN)
        // e a promoção a PENDING (faturável) acontecem em OrderApprovalCallback.
        String approvalDesc = String.format("Encomenda %s para %s - Total: %s MT",
                order.getOrderNumber(), order.getClient().getName(), order.getTotalAmount());
        approvalService.submitRequest("ORDER", order.getId(), order.getTotalAmount(), approvalDesc);

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
        // A encomenda facturada segue o prazo do cliente, tal como uma fatura emitida à mão.
        invoice.assignDueDate(LocalDate.now(), null);
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
     * Encomendas que ainda podem ser canceladas (não faturadas nem já canceladas), localizadas por
     * nº ou cliente (pesquisa parcial). Cobre tanto PENDING_APPROVAL como PENDING. Usado pelo diálogo
     * "Cancelar Encomenda" ao estilo PHC. {@code query} vazio devolve todas as canceláveis.
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> searchCancellableOrders(String query) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return orderRepository.findByCompanyId(companyId).stream()
                .filter(o -> !"BILLED".equalsIgnoreCase(o.getStatus())
                        && !"CANCELLED".equalsIgnoreCase(o.getStatus()))
                .filter(o -> matches(o.getOrderNumber(), query)
                        || (o.getClient() != null && matches(o.getClient().getName(), query)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cancela uma encomenda ainda não faturada. Regra de negócio (mirror de {@code cancelInvoice}):
     * exige perfil MANAGER/ADMIN e motivo; uma encomenda já faturada não se cancela (anula-se a
     * fatura); fecha também o pedido de aprovação aberto, se existir; audita.
     */
    @Transactional
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

        // Fecha o pedido de aprovação ainda aberto (encomenda em PENDING_APPROVAL).
        approvalService.cancelPendingForDocument("ORDER", order.getId(), reason);

        order.setStatus("CANCELLED");
        orderRepository.save(order);
        auditLogService.logCurrent("ORDER_CANCEL",
                "Encomenda " + order.getOrderNumber() + " cancelada. Motivo: " + reason);
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

    /**
     * Localiza encomendas PENDENTES (não faturadas) da empresa activa por nº de encomenda OU nome
     * de cliente (pesquisa parcial, sem distinção de maiúsculas). Usado pelo diálogo "Faturar
     * Encomenda" ao estilo PHC. {@code query} vazio devolve todas as pendentes.
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> searchPendingOrders(String query) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return orderRepository.findByCompanyIdAndStatusAndInvoiceIdIsNull(companyId, "PENDING").stream()
                .filter(o -> matches(o.getOrderNumber(), query)
                        || (o.getClient() != null && matches(o.getClient().getName(), query)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Substring case-insensitive, null-safe. {@code needle} vazio/nulo → corresponde sempre. */
    private boolean matches(String haystack, String needle) {
        if (needle == null || needle.isBlank()) return true;
        if (haystack == null) return false;
        return haystack.toLowerCase(java.util.Locale.ROOT)
                .contains(needle.trim().toLowerCase(java.util.Locale.ROOT));
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
