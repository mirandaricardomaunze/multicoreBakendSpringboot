package com.phcpro.modules.pos.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.pricing.TaxRates;
import com.phcpro.modules.comercial.model.*;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.CreateCreditNoteRequest;
import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.comercial.service.CreditNoteService;
import com.phcpro.modules.comercial.service.WalkInClientProvider;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.model.TreasuryAccount;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import com.phcpro.modules.comercial.model.SalesChannel;
import com.phcpro.modules.pos.dto.POSCheckoutLineRequest;
import com.phcpro.modules.pos.dto.POSCheckoutRequest;
import com.phcpro.modules.pos.dto.POSReturnRequest;
import com.phcpro.modules.pos.dto.PosPaymentRequest;
import com.phcpro.modules.pos.dto.TillMovementDTO;
import com.phcpro.modules.pos.dto.TillSessionDTO;
import com.phcpro.modules.pos.model.PaymentEntry;
import com.phcpro.modules.pos.model.PaymentMethod;
import com.phcpro.modules.pos.model.TillMovement;
import com.phcpro.modules.pos.model.TillMovementType;
import com.phcpro.modules.pos.model.TillSession;
import com.phcpro.modules.pos.repository.PaymentEntryRepository;
import com.phcpro.modules.pos.repository.TillMovementRepository;
import com.phcpro.modules.pos.repository.TillSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class POSService {

    private final TillSessionRepository tillSessionRepository;
    private final TillMovementRepository tillMovementRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final TreasuryAccountRepository treasuryAccountRepository;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final PaymentEntryRepository paymentEntryRepository;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final CreditNoteService creditNoteService;

    public POSService(
            TillSessionRepository tillSessionRepository,
            TillMovementRepository tillMovementRepository,
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            TreasuryAccountRepository treasuryAccountRepository,
            InventoryService inventoryService,
            FinanceService financeService,
            PaymentEntryRepository paymentEntryRepository,
            WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            CreditNoteService creditNoteService
    ) {
        this.tillSessionRepository = tillSessionRepository;
        this.tillMovementRepository = tillMovementRepository;
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.treasuryAccountRepository = treasuryAccountRepository;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
        this.paymentEntryRepository = paymentEntryRepository;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.creditNoteService = creditNoteService;
    }

    @Transactional(readOnly = true)
    public Optional<TillSession> getActiveSession(String operator, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return tillSessionRepository.findByOperatorAndStatusAndCompanyId(operator, "OPEN", companyId);
    }

    @Transactional(readOnly = true)
    public List<TillSession> getSessionsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return tillSessionRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<TillMovement> getMovementsBySession(Long sessionId) {
        List<TillMovement> movements = tillMovementRepository.findByTillSessionId(sessionId);
        movements.stream().findFirst().ifPresent(m ->
                CurrentUserContext.requireCompany(m.getTillSession().getCompany().getId()));
        return movements;
    }

    @Transactional
    public TillSession openSession(String operator, BigDecimal openingBalance, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Optional<TillSession> active = getActiveSession(operator, companyId);
        if (active.isPresent()) {
            throw new BusinessRuleException("Já possui uma sessão de caixa aberta para este operador.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        TillSession session = new TillSession();
        session.setOperator(operator);
        session.setCompany(company);
        session.setOpeningBalance(openingBalance);
        session.setOpenDate(LocalDateTime.now());
        session.setStatus("OPEN");

        return tillSessionRepository.save(session);
    }

    @Transactional
    public TillSession closeSession(Long sessionId, BigDecimal closingBalanceReal) {
        return closeSession(sessionId, closingBalanceReal, null);
    }

    /**
     * Fecha a sessão de caixa. Modelo Caixa→Tesouraria: a gaveta é apenas física durante a
     * sessão; no fecho, o numerário líquido gerado (vendas + suprimentos − sangrias) é
     * depositado na conta de tesouraria indicada por {@code depositAccountId}. Se for null,
     * a sessão fecha sem gerar o depósito (será lançado manualmente).
     */
    @Transactional
    public TillSession closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Sessão de caixa já se encontra fechada.");
        }

        CurrentUserContext.requireCompany(session.getCompany().getId());
        BigDecimal expected = computeExpectedCash(sessionId, session.getOpeningBalance());

        session.setClosingBalanceReal(closingBalanceReal);
        session.setClosingBalanceExpected(expected);
        session.setCloseDate(LocalDateTime.now());
        session.setStatus("CLOSED");
        session.setDifference(closingBalanceReal.subtract(expected));
        if (session.getDifference().compareTo(BigDecimal.ZERO) != 0) {
            PermissionGuard.requireManagerOrAdmin("fechar caixa com diferença");
        }
        session = tillSessionRepository.save(session);

        // Depósito do numerário líquido da sessão na tesouraria (entrada = DEBIT).
        if (depositAccountId != null) {
            BigDecimal netCash = expected.subtract(session.getOpeningBalance());
            if (netCash.compareTo(BigDecimal.ZERO) > 0) {
                financeService.registerTransaction(depositAccountId, "DEBIT", netCash,
                        "Depósito de fecho de caixa — sessão " + sessionId
                                + " (operador " + session.getOperator() + ")");
            }
        }
        auditLogService.logCurrent("POS_CLOSE_SESSION",
                "Sessão " + sessionId + " fechada por " + session.getOperator()
                        + ". Esperado: " + expected + " MT; contado: " + closingBalanceReal
                        + " MT; diferença: " + session.getDifference() + " MT.");

        return session;
    }

    /** Saldo esperado da gaveta: abertura + vendas + suprimentos − sangrias. */
    private BigDecimal computeExpectedCash(Long sessionId, BigDecimal openingBalance) {
        BigDecimal expected = openingBalance;
        for (TillMovement m : tillMovementRepository.findByTillSessionId(sessionId)) {
            TillMovementType type = m.getMovementType();
            if (type == TillMovementType.SALE || type == TillMovementType.SUPRIMENTO) {
                expected = expected.add(m.getAmount());
            } else if (type == TillMovementType.SANGRIA || type == TillMovementType.REFUND) {
                expected = expected.subtract(m.getAmount());
            }
        }
        return expected;
    }

    @Transactional
    public TillMovement addCashMovement(Long sessionId, String type, BigDecimal amount, String description) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Sessão de caixa está fechada. Impossível realizar movimentos.");
        }

        // Parse do tipo no limite do serviço — o enum elimina o bug de bypass por maiúsculas
        // (ex.: "sangria" saltava a guarda de saldo) e rejeita tipos inválidos.
        CurrentUserContext.requireCompany(session.getCompany().getId());
        TillMovementType movementType;
        try {
            movementType = TillMovementType.valueOf(type == null ? "" : type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de movimento de caixa inválido: " + type);
        }

        if (movementType == TillMovementType.SANGRIA) {
            PermissionGuard.requireManagerOrAdmin("realizar sangria de caixa");
            // Garante que há numerário suficiente na gaveta para a retirada.
            BigDecimal expected = computeExpectedCash(sessionId, session.getOpeningBalance());
            if (expected.compareTo(amount) < 0) {
                throw new BusinessRuleException("Saldo de caixa insuficiente para realizar a sangria. Disponível: " + expected + " MT");
            }
        }

        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(movementType);
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(session.getOperator());
        TillMovement saved = tillMovementRepository.save(movement);
        if (movementType == TillMovementType.SANGRIA || movementType == TillMovementType.SUPRIMENTO
                || movementType == TillMovementType.REFUND) {
            auditLogService.logCurrent("POS_CASH_MOVEMENT",
                    movementType + " na sessão " + sessionId + ": " + amount + " MT. " + description);
        }
        return saved;
    }

    @Transactional
    public Invoice checkout(POSCheckoutRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        TillSession session = getActiveSession(request.operator(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Deverá abrir uma sessão de caixa antes de efetuar vendas no POS."));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));

        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        Client client;
        String walkInLabel = null;
        if (request.clientId() != null) {
            client = clientRepository.findByIdAndCompaniesId(request.clientId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        } else {
            client = walkInClientProvider.getOrCreate();
            if (request.walkInName() != null && !request.walkInName().isBlank()) {
                walkInLabel = request.walkInName().trim();
            }
        }

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setCompany(company);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.PAID); // Immediate payment for POS sales
        invoice.setSalesChannel(SalesChannel.POS);
        // Venda POS é uma fatura real → número fiscal sequencial na série FT.
        invoice.setInvoiceNumber(documentNumberService.next(DocumentSeries.INVOICE));
        // Regista o operador da caixa como autor da venda (aparece no PDF e no histórico).
        invoice.setCreatedBy(request.operator() != null && !request.operator().isBlank()
                ? request.operator() : "SYSTEM");

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (POSCheckoutLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(product.getUnitPrice());
            
            // IVA à taxa-padrão (não depende do NUIT do cliente).
            BigDecimal taxRate = TaxRates.STANDARD_VAT;
            line.setTaxRate(taxRate);

            if (lineReq.discountPercentage() != null && lineReq.discountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                line.setDiscountPercentage(lineReq.discountPercentage());
            }

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    product.getUnitPrice(), lineReq.quantity(), lineReq.discountPercentage(), taxRate);

            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());
            invoice.addLine(line);

            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());

            // Register negative stock movement (exit)
            String clientLabel = walkInLabel != null
                    ? client.getName() + " — " + walkInLabel
                    : client.getName();
            String desc = String.format("Venda POS %s - Cliente %s", invoice.getInvoiceNumber(), clientLabel);
            inventoryService.registerMovement(
                    product,
                    warehouse,
                    lineReq.quantity().negate(),
                    "SALE",
                    lineReq.batchNumber(),
                    lineReq.serialNumber(),
                    desc
            );
        }

        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        BigDecimal totalAmount = subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP);
        invoice.setTotalAmount(totalAmount);

        // Decide payments path: new multi-method list OR legacy single account
        List<PosPaymentRequest> payments = request.payments();
        boolean hasMultiPayments = payments != null && !payments.isEmpty();
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (hasMultiPayments) {
            for (PosPaymentRequest p : payments) {
                totalPaid = totalPaid.add(p.amount());
            }
            totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);
            if (totalPaid.compareTo(totalAmount) > 0) {
                throw new BusinessRuleException(
                        "Soma dos pagamentos (" + totalPaid + ") excede o total da venda (" + totalAmount + ").");
            }
        } else if (request.treasuryAccountId() != null) {
            // Legacy: single full payment via the treasury account
            totalPaid = totalAmount;
        } else {
            throw new BusinessRuleException("Indique pelo menos um método de pagamento.");
        }

        invoice.setAmountPaid(totalPaid);
        invoice.setStatus(deriveStatus(totalPaid, totalAmount));
        invoice = invoiceRepository.save(invoice);

        // Persist PaymentEntry rows + apply each to the right treasury/till
        if (hasMultiPayments) {
            for (PosPaymentRequest p : payments) {
                applyPayment(invoice, p, session, client);
            }
        } else {
            // Legacy path — venda em numerário: entra apenas na gaveta da caixa.
            // O numerário só chega à tesouraria no fecho da sessão (depósito), evitando
            // a dupla contagem que existia ao registar também uma transação de tesouraria.
            registerTillMovement(session, totalAmount, invoice.getInvoiceNumber());
        }

        return invoice;
    }

    /**
     * Devolve produtos vendidos no POS por nota de credito RETURN e regista o reembolso.
     * A nota e aprovada dentro da mesma transaccao para repor stock apenas uma vez.
     */
    @Transactional
    public CreditNoteDTO returnSale(POSReturnRequest request) {
        PermissionGuard.requireManagerOrAdmin("registar devolução no POS");
        CurrentUserContext.requireCompany(request.companyId());

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Não é possível devolver uma fatura anulada.");
        }
        if (invoice.getSalesChannel() != SalesChannel.POS) {
            throw new BusinessRuleException("Use o fluxo comercial para devoluções de faturas que não são POS.");
        }

        CreditNoteDTO created = creditNoteService.create(new CreateCreditNoteRequest(
                invoice.getId(),
                "RETURN",
                request.warehouseId(),
                request.reason(),
                request.lines()
        ));
        CreditNoteDTO approved = creditNoteService.approve(created.id());
        applyRefund(request, approved, invoice);
        auditLogService.logCurrent("POS_RETURN",
                "Devolução POS da fatura " + invoice.getInvoiceNumber()
                        + " via nota de crédito " + approved.noteNumber()
                        + ". Total: " + approved.totalAmount() + " MT. Motivo: " + request.reason());
        return approved;
    }

    private void applyRefund(POSReturnRequest request, CreditNoteDTO note, Invoice invoice) {
        PaymentMethod method = parsePaymentMethod(request.refundMethod());
        BigDecimal amount = note.totalAmount().setScale(2, RoundingMode.HALF_UP);
        String description = "Reembolso POS " + note.noteNumber()
                + " (Fatura " + invoice.getInvoiceNumber() + ")";

        switch (method) {
            case CASH -> refundCash(request.operator(), request.companyId(), amount, description);
            case CARD, BANK_TRANSFER -> {
                if (request.treasuryAccountId() == null) {
                    throw new BusinessRuleException("Conta de tesouraria é obrigatória para reembolso por " + method + ".");
                }
                financeService.registerTransaction(request.treasuryAccountId(), "CREDIT", amount, description);
            }
            case CREDIT -> {
                // A nota de credito fica como credito do cliente; nao ha saida imediata de caixa.
            }
        }
    }

    private void refundCash(String operator, Long companyId, BigDecimal amount, String description) {
        TillSession session = getActiveSession(operator, companyId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Abra uma sessão de caixa antes de devolver dinheiro ao cliente."));
        BigDecimal expected = computeExpectedCash(session.getId(), session.getOpeningBalance());
        if (expected.compareTo(amount) < 0) {
            throw new BusinessRuleException("Saldo de caixa insuficiente para reembolso. Disponível: " + expected + " MT");
        }

        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(TillMovementType.REFUND);
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(operator);
        tillMovementRepository.save(movement);
        auditLogService.logCurrent("POS_REFUND_CASH",
                "Reembolso em numerário na sessão " + session.getId() + ": " + amount + " MT.");
    }

    private PaymentMethod parsePaymentMethod(String method) {
        try {
            return PaymentMethod.valueOf(method == null ? "" : method.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Método de reembolso inválido: " + method);
        }
    }

    private InvoiceStatus deriveStatus(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(total) >= 0)      return InvoiceStatus.PAID;
        if (paid.compareTo(BigDecimal.ZERO) > 0) return InvoiceStatus.PARTIALLY_PAID;
        return InvoiceStatus.APPROVED;  // credit-only — awaiting payment
    }

    private void applyPayment(Invoice invoice, PosPaymentRequest req, TillSession session, Client client) {
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(req.method());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Método de pagamento inválido: " + req.method());
        }

        BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tendered = req.tenderedAmount() == null ? amount : req.tenderedAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = BigDecimal.ZERO;
        if (method == PaymentMethod.CASH && tendered.compareTo(amount) > 0) {
            change = tendered.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        }

        PaymentEntry entry = new PaymentEntry();
        entry.setInvoice(invoice);
        entry.setMethod(method);
        entry.setAmount(amount);
        entry.setTenderedAmount(tendered);
        entry.setChangeGiven(change);
        entry.setReference(req.reference());
        entry.setPaidAt(LocalDateTime.now());
        entry.setCreatedBy("SYSTEM");
        paymentEntryRepository.save(entry);

        // Per-method side effects
        String desc = "Venda POS " + invoice.getInvoiceNumber() + " — " + method + " — " + client.getName();
        switch (method) {
            case CASH -> {
                if (session != null) {
                    // Venda em numerário durante uma sessão → entra só na gaveta.
                    // A tesouraria é alimentada no fecho da sessão (depósito).
                    registerTillMovement(session, amount, invoice.getInvoiceNumber());
                } else if (req.treasuryAccountId() != null) {
                    // Pagamento tardio em numerário, fora de sessão de caixa → entra direto na tesouraria.
                    financeService.registerTransaction(req.treasuryAccountId(), "DEBIT", amount, desc);
                } else {
                    throw new BusinessRuleException(
                            "Pagamento em numerário fora de sessão de caixa requer conta de tesouraria.");
                }
            }
            case CARD, BANK_TRANSFER -> {
                if (req.treasuryAccountId() == null) {
                    throw new BusinessRuleException("Conta de tesouraria é obrigatória para " + method + ".");
                }
                financeService.registerTransaction(req.treasuryAccountId(), "DEBIT", amount, desc);
            }
            case CREDIT -> { /* fiado — sem movimento financeiro até pagamento */ }
        }
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

    /**
     * Registar pagamento posterior de uma venda a crédito (fiado).
     * Atualiza amountPaid + status da fatura e cria um PaymentEntry.
     */
    @Transactional
    public Invoice registerLatePayment(Long invoiceId, PosPaymentRequest req) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Esta fatura está cancelada.");
        }
        BigDecimal outstanding = invoice.getTotalAmount().subtract(
                invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid());
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Esta fatura já está totalmente paga.");
        }
        if (req.amount().compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Pagamento (" + req.amount() + ") excede o saldo em dívida (" + outstanding + ").");
        }

        applyPayment(invoice, req, /*session*/ null, invoice.getClient());
        BigDecimal newPaid = (invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid())
                .add(req.amount()).setScale(2, RoundingMode.HALF_UP);
        invoice.setAmountPaid(newPaid);
        invoice.setStatus(deriveStatus(newPaid, invoice.getTotalAmount()));
        return invoiceRepository.save(invoice);
    }

    public TillSessionDTO toDTO(TillSession s) {
        return new TillSessionDTO(
                s.getId(),
                s.getOperator(),
                s.getCompany() != null ? s.getCompany().getId() : null,
                s.getOpeningBalance(),
                s.getClosingBalanceExpected(),
                s.getClosingBalanceReal(),
                s.getDifference(),
                s.getOpenDate(),
                s.getCloseDate(),
                s.getStatus()
        );
    }

    public TillMovementDTO toDTO(TillMovement m) {
        return new TillMovementDTO(
                m.getId(),
                m.getTillSession() != null ? m.getTillSession().getId() : null,
                m.getMovementType() != null ? m.getMovementType().name() : null,
                m.getAmount(),
                m.getDescription(),
                m.getMovementDate()
        );
    }

}
