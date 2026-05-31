package com.phcpro.modules.pos.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.modules.comercial.model.*;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.model.TreasuryAccount;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.pos.dto.POSCheckoutLineRequest;
import com.phcpro.modules.pos.dto.POSCheckoutRequest;
import com.phcpro.modules.pos.dto.PosPaymentRequest;
import com.phcpro.modules.pos.dto.TillMovementDTO;
import com.phcpro.modules.pos.dto.TillSessionDTO;
import com.phcpro.modules.pos.model.PaymentEntry;
import com.phcpro.modules.pos.model.PaymentMethod;
import com.phcpro.modules.pos.model.TillMovement;
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
            PaymentEntryRepository paymentEntryRepository
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
    }

    @Transactional(readOnly = true)
    public Optional<TillSession> getActiveSession(String operator, Long companyId) {
        return tillSessionRepository.findByOperatorAndStatusAndCompanyId(operator, "OPEN", companyId);
    }

    @Transactional(readOnly = true)
    public List<TillSession> getSessionsByCompany(Long companyId) {
        return tillSessionRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<TillMovement> getMovementsBySession(Long sessionId) {
        return tillMovementRepository.findByTillSessionId(sessionId);
    }

    @Transactional
    public TillSession openSession(String operator, BigDecimal openingBalance, Long companyId) {
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
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Sessão de caixa já se encontra fechada.");
        }

        // Expected balance calculation
        BigDecimal expected = session.getOpeningBalance();
        List<TillMovement> movements = tillMovementRepository.findByTillSessionId(sessionId);
        for (TillMovement m : movements) {
            if ("SALE".equals(m.getMovementType()) || "SUPRIMENTO".equals(m.getMovementType())) {
                expected = expected.add(m.getAmount());
            } else if ("SANGRIA".equals(m.getMovementType())) {
                expected = expected.subtract(m.getAmount());
            }
        }

        session.setClosingBalanceReal(closingBalanceReal);
        session.setClosingBalanceExpected(expected);
        session.setCloseDate(LocalDateTime.now());
        session.setStatus("CLOSED");
        session.setDifference(closingBalanceReal.subtract(expected));

        return tillSessionRepository.save(session);
    }

    @Transactional
    public TillMovement addCashMovement(Long sessionId, String type, BigDecimal amount, String description) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Sessão de caixa está fechada. Impossível realizar movimentos.");
        }

        if ("SANGRIA".equals(type)) {
            // Check cash drawer availability
            BigDecimal expected = session.getOpeningBalance();
            List<TillMovement> movements = tillMovementRepository.findByTillSessionId(sessionId);
            for (TillMovement m : movements) {
                if ("SALE".equals(m.getMovementType()) || "SUPRIMENTO".equals(m.getMovementType())) {
                    expected = expected.add(m.getAmount());
                } else if ("SANGRIA".equals(m.getMovementType())) {
                    expected = expected.subtract(m.getAmount());
                }
            }

            if (expected.compareTo(amount) < 0) {
                throw new BusinessRuleException("Saldo de caixa insuficiente para realizar a sangria. Disponível: " + expected + " MT");
            }
        }

        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(type.toUpperCase());
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());

        return tillMovementRepository.save(movement);
    }

    @Transactional
    public Invoice checkout(POSCheckoutRequest request) {
        TillSession session = getActiveSession(request.operator(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Deverá abrir uma sessão de caixa antes de efetuar vendas no POS."));

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setCompany(company);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.PAID); // Immediate payment for POS sales
        invoice.setInvoiceNumber("POS-" + System.currentTimeMillis());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (POSCheckoutLineRequest lineReq : request.lines()) {
            Product product = productRepository.findById(lineReq.productId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(product.getUnitPrice());
            
            // Tax calculation
            BigDecimal taxRate = client.getTaxId().startsWith("5") ? new BigDecimal("0.23") : new BigDecimal("0.17");
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
            String desc = String.format("Venda POS %s - Cliente %s", invoice.getInvoiceNumber(), client.getName());
            inventoryService.registerMovement(
                    product,
                    warehouse,
                    BigDecimal.valueOf(lineReq.quantity()).negate(),
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
            // Legacy path — register one CASH-equivalent entry via the supplied account
            financeService.registerTransaction(request.treasuryAccountId(), "DEBIT", totalAmount,
                    "Venda POS " + invoice.getInvoiceNumber() + " - " + client.getName());
            registerTillMovement(session, totalAmount, invoice.getInvoiceNumber());
        }

        return invoice;
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
                registerTillMovement(session, amount, invoice.getInvoiceNumber());
                if (req.treasuryAccountId() != null) {
                    financeService.registerTransaction(req.treasuryAccountId(), "DEBIT", amount, desc);
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
        movement.setMovementType("SALE");
        movement.setAmount(amount);
        movement.setDescription("Venda POS " + invoiceNumber);
        movement.setMovementDate(LocalDateTime.now());
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
                m.getMovementType(),
                m.getAmount(),
                m.getDescription(),
                m.getMovementDate()
        );
    }
}
