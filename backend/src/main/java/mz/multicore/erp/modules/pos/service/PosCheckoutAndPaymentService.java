package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.service.*;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.pos.dto.*;
import mz.multicore.erp.modules.pos.model.*;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PosCheckoutAndPaymentService {

    private final InvoiceLookup invoiceLookup;
    private final InvoiceCommandService invoiceCommands;
    private final ClientLookup clientLookup;
    private final ProductLookup productLookup;
    private final WarehouseLookup warehouseLookup;
    private final CompanyLookup companyLookup;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final PaymentEntryRepository paymentEntryRepository;
    private final TillMovementRepository tillMovementRepository;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final ReceivablesService receivablesService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderCommandService orderCommands;
    private final PosSessionAndShiftService sessionAndShiftService;
    private final PosReturnAndVoucherService returnAndVoucherService;
    private final PosPriceGuard priceGuard;
    private final mz.multicore.erp.modules.comercial.service.SaleStockIssuer saleStockIssuer; // respeita reservas; nulo só no construtor legado

    public PosCheckoutAndPaymentService(
            InvoiceLookup invoiceLookup, InvoiceCommandService invoiceCommands, ClientLookup clientLookup,
            ProductLookup productLookup, WarehouseLookup warehouseLookup,
            CompanyLookup companyLookup, InventoryService inventoryService,
            FinanceService financeService, PaymentEntryRepository paymentEntryRepository,
            TillMovementRepository tillMovementRepository, WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            ReceivablesService receivablesService, ApplicationEventPublisher eventPublisher,
            OrderCommandService orderCommands, PosSessionAndShiftService sessionAndShiftService,
            PosReturnAndVoucherService returnAndVoucherService, PosPriceGuard priceGuard,
            mz.multicore.erp.modules.comercial.service.SaleStockIssuer saleStockIssuer) {
        this.priceGuard = priceGuard;
        this.saleStockIssuer = saleStockIssuer;
        this.invoiceLookup = invoiceLookup;
        this.invoiceCommands = invoiceCommands;
        this.clientLookup = clientLookup;
        this.productLookup = productLookup;
        this.warehouseLookup = warehouseLookup;
        this.companyLookup = companyLookup;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
        this.paymentEntryRepository = paymentEntryRepository;
        this.tillMovementRepository = tillMovementRepository;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.receivablesService = receivablesService;
        this.eventPublisher = eventPublisher;
        this.orderCommands = orderCommands;
        this.sessionAndShiftService = sessionAndShiftService;
        this.returnAndVoucherService = returnAndVoucherService;
    }

    public Invoice checkout(POSCheckoutRequest request) {
        PermissionGuard.requireSeller("efectuar venda no POS");
        CurrentUserContext.requireCompany(request.companyId());
        sessionAndShiftService.requireOwnOperatorForSeller(request.operator());

        String requestFingerprint = posRequestFingerprint(request);
        if (request.contingencyReference() != null && !request.contingencyReference().isBlank()) {
            Optional<Invoice> existing = invoiceLookup.findByContingencyReference(
                    request.companyId(), request.contingencyReference().trim());
            if (existing.isPresent()) {
                Invoice previous = existing.get();
                if (previous.getPosRequestFingerprint() != null && !previous.getPosRequestFingerprint().equals(requestFingerprint)) {
                    throw new BusinessRuleException("A chave de idempotência desta venda já foi usada com dados diferentes. Actualize a venda e tente novamente com uma nova chave.");
                }
                return previous;
            }
        }

        if (priceGuard != null) priceGuard.requireAuthorized(request);
        TillSession session = sessionAndShiftService.getActiveSession(request.operator(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Deverá abrir uma sessão de caixa antes de efetuar vendas no POS."));
        Warehouse warehouse = warehouseLookup.require(request.warehouseId());
        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Company company = companyLookup.require(request.companyId());

        Client client;
        String walkInLabel = null;
        if (request.clientId() != null) {
            client = clientLookup.requireForCompany(request.clientId(), request.companyId());
        } else {
            if (request.walkInName() == null || request.walkInName().trim().isBlank()) {
                throw new BusinessRuleException("É obrigatório indicar o nome do cliente para efetuar a venda no POS.");
            }
            client = walkInClientProvider.getOrCreate();
            walkInLabel = request.walkInName().trim();
        }

        Invoice invoice = new Invoice();
        if (request.contingencyReference() != null && !request.contingencyReference().isBlank()) {
            invoice.setContingencyReference(request.contingencyReference().trim());
            invoice.setPosRequestFingerprint(requestFingerprint);
        }
        invoice.setClient(client);
        String resolvedCustomerName = walkInLabel != null ? walkInLabel : client.getName();
        if (resolvedCustomerName == null || resolvedCustomerName.trim().isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o nome do cliente para efetuar a venda no POS.");
        }
        invoice.setCustomerName(resolvedCustomerName.trim());
        invoice.setCompany(company);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.assignDueDate(LocalDate.now(), null);
        invoice.setSalesChannel(SalesChannel.POS);
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));
        invoice.setCreatedBy(request.operator() != null && !request.operator().isBlank() ? request.operator() : "SYSTEM");

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (POSCheckoutLineRequest lineReq : request.lines()) {
            Product product = productLookup.findForCompany(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId())).requireOrdinarySale();
            BigDecimal unitPrice = (lineReq.unitPrice() != null && lineReq.unitPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? lineReq.unitPrice() : PosPriceGuard.defaultPrice(priceGuard, request, product, lineReq.quantity());
            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);
            BigDecimal taxRate = product.effectiveTaxRate();
            line.setTaxRate(taxRate);
            if (lineReq.discountPercentage() != null && lineReq.discountPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessRuleException("O desconto da linha não pode exceder 100 por cento.");
            }
            if (lineReq.discountPercentage() != null && lineReq.discountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                line.setDiscountPercentage(lineReq.discountPercentage());
            }
            LineCalculator.LineAmounts amounts = LineCalculator.compute(unitPrice, lineReq.quantity(), lineReq.discountPercentage(), taxRate);
            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());
            line.setUnitCost(product.getPurchasePrice());
            invoice.addLine(line);
            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        BigDecimal totalAmount = subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP);
        invoice.setTotalAmount(totalAmount);

        List<PosPaymentRequest> payments = request.payments();
        boolean hasMultiPayments = payments != null && !payments.isEmpty();
        BigDecimal totalPaid = BigDecimal.ZERO;
        PosPaymentSplit split = hasMultiPayments ? PosPaymentSplit.of(payments) : null;
        if (hasMultiPayments) {
            totalPaid = split.settled(); // a CREDIT line stays owed: it never counts as paid
            if (split.declared().compareTo(totalAmount) > 0) {
                throw new BusinessRuleException("Soma dos pagamentos (" + DisplayNumbers.money(split.declared()) + " MT) excede o total da venda (" + DisplayNumbers.money(totalAmount) + " MT).");
            }
        } else if (request.treasuryAccountId() != null) {
            totalPaid = totalAmount;
        } else {
            throw new BusinessRuleException("Indique pelo menos um método de pagamento.");
        }

        receivablesService.assertCreditAvailable(client, totalAmount.subtract(totalPaid));
        deductStockForSale(invoice, warehouse, client, walkInLabel);
        invoice.setAmountPaid(totalPaid);
        invoice.setStatus(invoice.deriveStatusFromPayments());
        final Invoice savedInvoice = invoiceCommands.save(invoice);

        if (request.quotationId() != null && orderCommands != null) {
            orderCommands.markQuotationConvertedByPos(request.quotationId(), request.companyId(), savedInvoice)
                    .ifPresent(quotation -> auditLogService.logCurrent("QUOTATION_CONVERT_POS", "Cotação "
                            + quotation.getQuotationNumber() + " convertida no POS através da fatura "
                            + savedInvoice.getInvoiceNumber() + ". Total: " + savedInvoice.getTotalAmount() + " MT."));
        }

        if (hasMultiPayments) {
            for (PosPaymentRequest p : payments) applyPayment(savedInvoice, p, session, client);
        } else {
            registerTillMovement(session, totalAmount, savedInvoice.getInvoiceNumber());
        }

        BigDecimal moneyNow = split == null ? totalPaid : split.money(), cashNow = split == null ? totalPaid : split.cash();
        BigDecimal costOfGoods = savedInvoice.getLines().stream().map(InvoiceLine::lineCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        eventPublisher.publishEvent(new SaleRegisteredEvent(
                savedInvoice.getCompany().getId(), savedInvoice.getId(), savedInvoice.getInvoiceNumber(),
                LocalDate.now(), savedInvoice.getTotalBeforeTax(), savedInvoice.getTaxAmount(),
                savedInvoice.getTotalAmount(), costOfGoods, moneyNow, cashNow.signum() > 0, cashNow));
        return savedInvoice;
    }

    public Invoice registerLatePayment(Long invoiceId, PosPaymentRequest req) {
        PermissionGuard.requireSeller("registar pagamento posterior");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Invoice invoice = invoiceCommands.lockForCompany(invoiceId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        String key = req.idempotencyKey();
        String fingerprint = key == null ? null : paymentRequestFingerprint(req);
        if (key != null) {
            Optional<PaymentEntry> previous = paymentEntryRepository.findByInvoiceIdAndIdempotencyKey(invoiceId, key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException("A chave de idempotência deste pagamento já foi usada com dados diferentes.");
                }
                return invoice;
            }
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Esta fatura está cancelada.");
        }
        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid());
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Esta fatura já está totalmente paga.");
        }
        if (req.amount().compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Pagamento (" + DisplayNumbers.money(req.amount()) + " MT) excede o saldo em dívida (" + DisplayNumbers.money(outstanding) + " MT).");
        }
        applyPayment(invoice, req, null, invoice.getClient());
        BigDecimal newPaid = (invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid())
                .add(req.amount()).setScale(2, RoundingMode.HALF_UP);
        invoice.setAmountPaid(newPaid);
        invoice.setStatus(invoice.deriveStatusFromPayments());
        return invoiceCommands.save(invoice);
    }

    private void applyPayment(Invoice invoice, PosPaymentRequest req, TillSession session, Client client) {
        PaymentMethod method = PosReturnAndVoucherService.parsePaymentMethod(req.method());
        BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tendered = req.tenderedAmount() == null ? amount : req.tenderedAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = (method == PaymentMethod.CASH && tendered.compareTo(amount) > 0)
                ? tendered.subtract(amount).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        PaymentEntry entry = new PaymentEntry();
        entry.setInvoice(invoice);
        entry.setMethod(method);
        entry.setAmount(amount);
        entry.setTenderedAmount(tendered);
        entry.setChangeGiven(change);
        entry.setReference(req.reference());
        entry.setIdempotencyKey(req.idempotencyKey());
        entry.setRequestFingerprint(req.idempotencyKey() == null ? null : paymentRequestFingerprint(req));
        entry.setPaidAt(LocalDateTime.now());
        entry.setCreatedBy("SYSTEM");
        paymentEntryRepository.save(entry);

        String desc = "Venda POS " + invoice.getInvoiceNumber() + " — " + method + " — " + client.getName();
        switch (method) {
            case CASH -> {
                if (session != null) registerTillMovement(session, amount, invoice.getInvoiceNumber());
                else if (req.treasuryAccountId() != null) financeService.registerTransaction(req.treasuryAccountId(), "DEBIT", amount, desc);
                else throw new BusinessRuleException("Pagamento em numerário fora de sessão de caixa requer conta de tesouraria.");
            }
            case CARD, BANK_TRANSFER, MPESA, EMOLA -> {
                if (req.treasuryAccountId() == null) throw new BusinessRuleException("Conta de tesouraria é obrigatória para " + method + ".");
                financeService.registerTransaction(req.treasuryAccountId(), "DEBIT", amount, desc);
            }
            case CREDIT -> {}
            case STORE_CREDIT -> returnAndVoucherService.redeemVoucherForPayment(req.reference(), amount, invoice);
        }
    }

    private void deductStockForSale(Invoice invoice, Warehouse warehouse, Client client, String walkInLabel) {
        String clientLabel = walkInLabel != null ? client.getName() + " — " + walkInLabel : client.getName();
        String desc = String.format("Venda POS %s - Cliente %s", invoice.getInvoiceNumber(), clientLabel);
        if (saleStockIssuer != null) saleStockIssuer.issue(invoice, warehouse, desc);
        else invoice.getLines().forEach(line -> inventoryService.registerMovement(line.getProduct(), warehouse,
                line.getQuantity().negate(), "SALE", line.getBatchNumber(), line.getSerialNumber(), desc));
    }

    private void registerTillMovement(TillSession session, BigDecimal amount, String invoiceNumber) {
        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(TillMovementType.SALE);
        movement.setAmount(amount);
        movement.setDescription("Venda POS " + invoiceNumber);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(session.getOperator());
        tillMovementRepository.save(movement);
    }

    private static String posRequestFingerprint(POSCheckoutRequest request) {
        StringBuilder canonical = new StringBuilder(512);
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.operator());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.companyId());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.clientId());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.walkInName());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.warehouseId());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.treasuryAccountId());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, request.quotationId());
        if (request.lines() != null) {
            request.lines().forEach(l -> {
                PosReturnAndVoucherService.appendFingerprintPart(canonical, l.productId());
                PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(l.quantity()));
                PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(l.discountPercentage()));
                PosReturnAndVoucherService.appendFingerprintPart(canonical, l.batchNumber());
                PosReturnAndVoucherService.appendFingerprintPart(canonical, l.serialNumber());
                PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(l.unitPrice()));
            });
        }
        canonical.append("|payments|");
        if (request.payments() != null) {
            request.payments().forEach(p -> {
                PosReturnAndVoucherService.appendFingerprintPart(canonical, p.method());
                PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(p.amount()));
                PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(p.tenderedAmount()));
                PosReturnAndVoucherService.appendFingerprintPart(canonical, p.reference());
                PosReturnAndVoucherService.appendFingerprintPart(canonical, p.treasuryAccountId());
            });
        }
        return PosReturnAndVoucherService.sha256Hex(canonical.toString());
    }

    private static String paymentRequestFingerprint(PosPaymentRequest req) {
        StringBuilder canonical = new StringBuilder(160);
        PosReturnAndVoucherService.appendFingerprintPart(canonical, req.method());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(req.amount()));
        PosReturnAndVoucherService.appendFingerprintPart(canonical, PosReturnAndVoucherService.canonicalDecimal(req.tenderedAmount()));
        PosReturnAndVoucherService.appendFingerprintPart(canonical, req.reference());
        PosReturnAndVoucherService.appendFingerprintPart(canonical, req.treasuryAccountId());
        return PosReturnAndVoucherService.sha256Hex(canonical.toString());
    }
}
