package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.ReceivablesService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.dto.TillMovementDTO;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.model.ShiftReconciliation;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest;
import mz.multicore.erp.modules.pos.model.PaymentMethod;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillMovementType;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.pos.repository.ShiftReconciliationRepository;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.pos.dto.POSReturnResultDTO;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;
import mz.multicore.erp.modules.pos.model.StoreVoucher;
import mz.multicore.erp.modules.pos.model.StoreVoucherStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ReceivablesService receivablesService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final StoreVoucherRepository storeVoucherRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final QuotationRepository quotationRepository;
    private final ShiftReconciliationRepository shiftReconciliationRepository;

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
            CreditNoteService creditNoteService,
            ReceivablesService receivablesService,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            StoreVoucherRepository storeVoucherRepository,
            CreditNoteRepository creditNoteRepository,
            QuotationRepository quotationRepository
    ) {
        this(tillSessionRepository, tillMovementRepository, invoiceRepository, clientRepository,
                productRepository, warehouseRepository, companyRepository, treasuryAccountRepository,
                inventoryService, financeService, paymentEntryRepository, walkInClientProvider,
                documentNumberService, auditLogService, creditNoteService, receivablesService,
                eventPublisher, storeVoucherRepository, creditNoteRepository, quotationRepository, null);
    }

    @Autowired
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
            CreditNoteService creditNoteService,
            ReceivablesService receivablesService,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            StoreVoucherRepository storeVoucherRepository,
            CreditNoteRepository creditNoteRepository,
            QuotationRepository quotationRepository,
            ShiftReconciliationRepository shiftReconciliationRepository
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
        this.receivablesService = receivablesService;
        this.eventPublisher = eventPublisher;
        this.storeVoucherRepository = storeVoucherRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.quotationRepository = quotationRepository;
        this.shiftReconciliationRepository = shiftReconciliationRepository;
    }

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
            CreditNoteService creditNoteService,
            ReceivablesService receivablesService,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            StoreVoucherRepository storeVoucherRepository,
            CreditNoteRepository creditNoteRepository
    ) {
        this(tillSessionRepository, tillMovementRepository, invoiceRepository, clientRepository,
                productRepository, warehouseRepository, companyRepository, treasuryAccountRepository,
                inventoryService, financeService, paymentEntryRepository, walkInClientProvider,
                documentNumberService, auditLogService, creditNoteService, receivablesService,
                eventPublisher, storeVoucherRepository, creditNoteRepository, null, null);
    }

    /**
     * Procura a sessão OPEN do operador. Após passagem de turno, o operador actual está
     * em {@code current_operator}; por isso verifica-se primeiro esse campo e só depois
     * o campo {@code operator} (abertura original). Garante que o operador B, que recebeu
     * a caixa por handover, encontra a sessão sem a ter aberto.
     */
    @Transactional(readOnly = true)
    public Optional<TillSession> getActiveSession(String operator, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return tillSessionRepository.findActiveSessionForOperator(operator, "OPEN", companyId);
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
        return closeSession(sessionId, closingBalanceReal, null, null, null);
    }

    @Transactional
    public TillSession closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId) {
        return closeSession(sessionId, closingBalanceReal, depositAccountId, null, null);
    }

    /**
     * Fecha a sessão de caixa. Modelo Caixa→Tesouraria: a gaveta é apenas física durante a
     * sessão; no fecho, o numerário líquido gerado (vendas + suprimentos − sangrias) é
     * depositado na conta de tesouraria indicada por {@code depositAccountId}. Se for null,
     * a sessão fecha sem gerar o depósito (será lançado manualmente).
     */
    @Transactional
    public TillSession closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId, String notes, String cashBreakdownJson) {
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
        session.setClosingNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        session.setCashBreakdownJson(cashBreakdownJson != null && !cashBreakdownJson.isBlank() ? cashBreakdownJson.trim() : null);

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
                        + " MT; diferença: " + session.getDifference() + " MT."
                        + (session.getClosingNotes() != null ? " Justificação: " + session.getClosingNotes() : ""));

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

    /**
     * Dados do fecho de caixa (Z): reconciliação da gaveta (abertura + vendas em numerário +
     * suprimentos − sangrias − devoluções = esperado) vs contado, e a diferença. Leitura pura,
     * tenant-scoped — funciona antes e depois do fecho (permite pré-visualizar). Ver
     * {@code docs/FECHO_CAIXA_Z_SPEC.md}.
     */
    @Transactional(readOnly = true)
    public PosZReportDTO buildZReport(Long sessionId) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));
        CurrentUserContext.requireCompany(session.getCompany().getId());

        BigDecimal opening = session.getOpeningBalance() == null ? BigDecimal.ZERO : session.getOpeningBalance();
        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal suprimentos = BigDecimal.ZERO;
        BigDecimal sangrias = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        int saleCount = 0;
        int refundsCount = 0;
        for (TillMovement m : tillMovementRepository.findByTillSessionId(sessionId)) {
            switch (m.getMovementType()) {
                case SALE -> { cashSales = cashSales.add(m.getAmount()); saleCount++; }
                case SUPRIMENTO -> suprimentos = suprimentos.add(m.getAmount());
                case SANGRIA -> sangrias = sangrias.add(m.getAmount());
                case REFUND -> { refunds = refunds.add(m.getAmount()); refundsCount++; }
            }
        }

        // Vendas por outros meios de pagamento durante o período da sessão
        LocalDateTime openDate = session.getOpenDate();
        LocalDateTime closeDate = session.getCloseDate() != null ? session.getCloseDate() : LocalDateTime.now();

        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal mpesaSales = BigDecimal.ZERO;
        BigDecimal chequeSales = BigDecimal.ZERO;
        BigDecimal creditSales = BigDecimal.ZERO;

        List<PaymentEntry> entries = paymentEntryRepository.findByInvoiceCompanyIdAndPaidAtBetween(
                session.getCompany().getId(), openDate, closeDate);

        for (PaymentEntry entry : entries) {
            if (entry.getInvoice() != null && SalesChannel.POS == entry.getInvoice().getSalesChannel()
                    && (session.getOperator() == null || session.getOperator().equalsIgnoreCase(entry.getInvoice().getCreatedBy()))) {
                if (entry.getMethod() != null) {
                    switch (entry.getMethod()) {
                        case CARD -> cardSales = cardSales.add(entry.getAmount());
                        case MPESA, EMOLA -> mpesaSales = mpesaSales.add(entry.getAmount());
                        case BANK_TRANSFER -> chequeSales = chequeSales.add(entry.getAmount());
                        case CREDIT -> creditSales = creditSales.add(entry.getAmount());
                        default -> {}
                    }
                }
            }
        }

        BigDecimal totalSales = cashSales.add(cardSales).add(mpesaSales).add(chequeSales).add(creditSales);
        BigDecimal expected = opening.add(cashSales).add(suprimentos).subtract(sangrias).subtract(refunds);
        BigDecimal counted = session.getClosingBalanceReal();
        BigDecimal difference = counted == null ? null : counted.subtract(expected);

        List<ShiftReconciliationDTO> recons = shiftReconciliationRepository != null
                ? shiftReconciliationRepository.findByTillSessionIdOrderByReconciledAtAsc(sessionId)
                        .stream().map(this::toDTO).toList()
                : java.util.Collections.emptyList();

        return new PosZReportDTO(
                session.getId(),
                session.getOperator(),
                session.getCurrentOperator(),
                session.getOpenDate(),
                session.getCloseDate(),
                session.getStatus(),
                opening,
                cashSales,
                cardSales,
                mpesaSales,
                chequeSales,
                creditSales,
                totalSales,
                suprimentos,
                sangrias,
                refunds,
                expected,
                counted,
                difference,
                saleCount,
                refundsCount,
                session.getClosingNotes(),
                session.getCashBreakdownJson(),
                recons
        );
    }

    @Transactional(readOnly = true)
    public List<mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO> getSessionsHistory(
            Long companyId, LocalDateTime from, LocalDateTime to) {
        CurrentUserContext.requireCompany(companyId);
        List<TillSession> sessions;
        if (from != null && to != null) {
            sessions = tillSessionRepository.findByCompanyIdAndOpenDateBetweenOrderByOpenDateDesc(companyId, from, to);
        } else {
            sessions = tillSessionRepository.findByCompanyIdOrderByOpenDateDesc(companyId);
        }
        return sessions.stream().map(s -> {
            PosZReportDTO z = buildZReport(s.getId());
            return new mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO(
                    s.getId(),
                    s.getOperator(),
                    s.getOpenDate(),
                    s.getCloseDate(),
                    s.getStatus(),
                    s.getOpeningBalance(),
                    z.expectedCash(),
                    s.getClosingBalanceReal(),
                    s.getDifference(),
                    z.totalSales(),
                    z.saleCount()
            );
        }).toList();
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

        // Idempotência para Modo de Contingência (Offline-First):
        // Se a venda com esta referência de contingência já foi sincronizada anteriormente,
        // devolve o documento emitido sem duplicar cobrança ou saída de stock.
        if (request.contingencyReference() != null && !request.contingencyReference().isBlank()) {
            java.util.Optional<Invoice> existing = invoiceRepository.findByCompanyIdAndContingencyReference(
                    request.companyId(), request.contingencyReference().trim());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

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
            if (request.walkInName() == null || request.walkInName().trim().isBlank()) {
                throw new BusinessRuleException("É obrigatório indicar o nome do cliente para efetuar a venda no POS.");
            }
            client = walkInClientProvider.getOrCreate();
            walkInLabel = request.walkInName().trim();
        }

        Invoice invoice = new Invoice();
        if (request.contingencyReference() != null && !request.contingencyReference().isBlank()) {
            invoice.setContingencyReference(request.contingencyReference().trim());
        }
        invoice.setClient(client);
        // Nome a imprimir no recibo: o rótulo walk-in escrito pelo operador, ou o nome do
        // cliente registado quando não há rótulo livre.
        String resolvedCustomerName = walkInLabel != null ? walkInLabel : client.getName();
        if (resolvedCustomerName == null || resolvedCustomerName.trim().isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o nome do cliente para efetuar a venda no POS.");
        }
        invoice.setCustomerName(resolvedCustomerName.trim());
        invoice.setCompany(company);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.PAID); // Immediate payment for POS sales
        // Vencimento: pronto pagamento por omissão, prazo do cliente quando a venda fica a fiado.
        // Mesma regra do domínio que a faturação usa — não uma segunda cópia aqui.
        invoice.assignDueDate(java.time.LocalDate.now(), null);
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

            // Preço efectivo: se a linha traz preço cotado/personalizado (>0), honra esse valor;
            // caso contrário, aplica tabela/grosso quando a quantidade atinge a mínima de grosso do produto.
            BigDecimal unitPrice = (lineReq.unitPrice() != null && lineReq.unitPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? lineReq.unitPrice()
                    : product.effectiveUnitPrice(lineReq.quantity());

            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);

            // IVA dinâmico: taxa do artigo (padrão quando não tem). Regra partilhada com a fatura e
            // a encomenda em Product.effectiveTaxRate — não duplicar aqui.
            BigDecimal taxRate = product.effectiveTaxRate();
            line.setTaxRate(taxRate);

            if (lineReq.discountPercentage() != null && lineReq.discountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                line.setDiscountPercentage(lineReq.discountPercentage());
            }

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    unitPrice, lineReq.quantity(), lineReq.discountPercentage(), taxRate);

            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
            line.setSerialNumber(lineReq.serialNumber());
            // Fotografia do custo no acto da venda — a margem de hoje não pode mudar quando o
            // preço de compra mudar. Ver docs/MARGEM_CUSTO_HISTORICO_SPEC.md.
            line.setUnitCost(product.getPurchasePrice());
            invoice.addLine(line);

            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
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

        // Fiado no balcão: o que não é pago agora vira dívida e tem de caber no limite de
        // crédito do cliente. Venda paga na totalidade não consome crédito nenhum.
        //
        // A verificação vem ANTES da saída de stock: uma venda recusada não pode deixar
        // mercadoria já descontada do armazém. (A transacção reverteria, mas depender do
        // rollback para não fazer estragos é frágil — a ordem é que tem de estar certa.)
        receivablesService.assertCreditAvailable(client, totalAmount.subtract(totalPaid));

        deductStockForSale(invoice, warehouse, client, walkInLabel);

        invoice.setAmountPaid(totalPaid);
        invoice.setStatus(invoice.deriveStatusFromPayments());
        final Invoice savedInvoice = invoiceRepository.save(invoice);

        // Se a venda provém de uma cotação/pró-forma, marca a cotação como convertida
        if (request.quotationId() != null && quotationRepository != null) {
            quotationRepository.findByIdAndCompanyId(request.quotationId(), request.companyId())
                    .ifPresent(quotation -> {
                        if (quotation.getStatus().isOpen()) {
                            quotation.setStatus(mz.multicore.erp.modules.comercial.model.QuotationStatus.CONVERTED);
                            quotation.setInvoiceId(savedInvoice.getId());
                            quotation.setInvoiceNumber(savedInvoice.getInvoiceNumber());
                            if (quotation.getDecidedAt() == null) {
                                quotation.setDecidedAt(LocalDateTime.now());
                                quotation.setDecidedBy(savedInvoice.getCreatedBy());
                            }
                            quotationRepository.save(quotation);
                            auditLogService.logCurrent("QUOTATION_CONVERT_POS",
                                    "Cotação " + quotation.getQuotationNumber() + " convertida no POS através da fatura "
                                            + savedInvoice.getInvoiceNumber() + ". Total: " + savedInvoice.getTotalAmount() + " MT.");
                        }
                    });
        }

        // Persist PaymentEntry rows + apply each to the right treasury/till
        if (hasMultiPayments) {
            for (PosPaymentRequest p : payments) {
                applyPayment(savedInvoice, p, session, client);
            }
        } else {
            // Legacy path — venda em numerário: entra apenas na gaveta da caixa.
            // O numerário só chega à tesouraria no fecho da sessão (depósito), evitando
            // a dupla contagem que existia ao registar também uma transação de tesouraria.
            registerTillMovement(session, totalAmount, savedInvoice.getInvoiceNumber());
        }

        // Contabilidade: a venda de balcão é uma fatura real e lança como tal. Numerário
        // (gaveta ou método CASH) entra em Caixa; o resto em Banco. Ver CONTABILIDADE_SPEC §5.
        boolean cashPayment = !hasMultiPayments || payments.stream()
                .anyMatch(p -> "CASH".equalsIgnoreCase(p.method()));
        BigDecimal costOfGoods = savedInvoice.getLines().stream()
                .map(InvoiceLine::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.SaleRegisteredEvent(
                savedInvoice.getCompany().getId(),
                savedInvoice.getId(),
                savedInvoice.getInvoiceNumber(),
                java.time.LocalDate.now(),
                savedInvoice.getTotalBeforeTax(),
                savedInvoice.getTaxAmount(),
                savedInvoice.getTotalAmount(),
                costOfGoods,
                totalPaid,
                cashPayment));

        return savedInvoice;
    }

    /**
     * Devolve produtos vendidos no POS por nota de credito RETURN e regista o reembolso.
     * A nota e aprovada dentro da mesma transaccao para repor stock apenas uma vez.
     */
    @Transactional
    public POSReturnResultDTO returnSale(POSReturnRequest request) {
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
        StoreVoucher voucher = applyRefund(request, approved, invoice);
        auditLogService.logCurrent("POS_RETURN",
                "Devolução POS da fatura " + invoice.getInvoiceNumber()
                        + " via nota de crédito " + approved.noteNumber()
                        + ". Total: " + approved.totalAmount() + " MT. Motivo: " + request.reason()
                        + (voucher != null ? " (Vale emitido: " + voucher.getCode() + ")" : ""));
        return new POSReturnResultDTO(approved, voucher != null ? toDTO(voucher) : null);
    }

    private StoreVoucher applyRefund(POSReturnRequest request, CreditNoteDTO note, Invoice invoice) {
        PaymentMethod method = parsePaymentMethod(request.refundMethod());
        BigDecimal amount = note.totalAmount().setScale(2, RoundingMode.HALF_UP);
        String description = "Reembolso POS " + note.noteNumber()
                + " (Fatura " + invoice.getInvoiceNumber() + ")";

        switch (method) {
            case CASH -> {
                refundCash(request.operator(), request.companyId(), amount, description);
                return null;
            }
            case CARD, BANK_TRANSFER, MPESA, EMOLA -> {
                if (request.treasuryAccountId() == null) {
                    throw new BusinessRuleException("Conta de tesouraria é obrigatória para reembolso por " + method + ".");
                }
                financeService.registerTransaction(request.treasuryAccountId(), "CREDIT", amount, description);
                return null;
            }
            case CREDIT -> {
                // A nota de credito fica como credito do cliente; nao ha saida imediata de caixa.
                return null;
            }
            case STORE_CREDIT -> {
                return createStoreVoucherForReturn(invoice, note, amount, request.operator());
            }
        }
        return null;
    }

    private StoreVoucher createStoreVoucherForReturn(Invoice invoice, CreditNoteDTO note, BigDecimal amount, String operator) {
        String code = generateUniqueVoucherCode();
        StoreVoucher voucher = new StoreVoucher();
        voucher.setCode(code);
        voucher.setInitialAmount(amount);
        voucher.setRemainingAmount(amount);
        voucher.setCompany(invoice.getCompany());
        voucher.setClient(invoice.getClient());
        String clientName = invoice.getCustomerName();
        if (clientName == null || clientName.isBlank()) {
            clientName = invoice.getClient() != null ? invoice.getClient().getName() : "Cliente Geral";
        }
        voucher.setClientName(clientName);
        CreditNote cn = creditNoteRepository.findById(note.id()).orElse(null);
        voucher.setCreditNote(cn);
        voucher.setIssuedAt(LocalDateTime.now());
        voucher.setExpiresAt(java.time.LocalDate.now().plusDays(90));
        voucher.setStatus(StoreVoucherStatus.ACTIVE);
        voucher.setCreatedBy(operator != null ? operator : "SYSTEM");
        voucher = storeVoucherRepository.save(voucher);

        auditLogService.logCurrent("STORE_VOUCHER_ISSUED",
                "Emitido vale de compras " + code + " no valor de " + amount + " MT para " + clientName
                        + " associado à nota de crédito " + note.noteNumber());
        return voucher;
    }

    private String generateUniqueVoucherCode() {
        java.security.SecureRandom rng = new java.security.SecureRandom();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("VALE-");
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(rng.nextInt(chars.length())));
            }
            sb.append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(rng.nextInt(chars.length())));
            }
            code = sb.toString();
            attempts++;
            if (attempts > 50) {
                code = "VALE-" + System.currentTimeMillis();
                break;
            }
        } while (storeVoucherRepository.existsByCode(code));
        return code;
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
        if (method == null || method.isBlank()) {
            throw new BusinessRuleException("Método de pagamento inválido: null");
        }
        String normalized = method.trim().toUpperCase();
        if ("VALE".equals(normalized) || "VALE_COMPRAS".equals(normalized) || "VALE DE COMPRAS".equals(normalized)
                || "STORE_CREDIT".equals(normalized) || "STORE CREDIT".equals(normalized)) {
            return PaymentMethod.STORE_CREDIT;
        }
        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Método de reembolso inválido: " + method);
        }
    }

    private void applyPayment(Invoice invoice, PosPaymentRequest req, TillSession session, Client client) {
        PaymentMethod method = parsePaymentMethod(req.method());

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
            case CARD, BANK_TRANSFER, MPESA, EMOLA -> {
                if (req.treasuryAccountId() == null) {
                    throw new BusinessRuleException("Conta de tesouraria é obrigatória para " + method + ".");
                }
                financeService.registerTransaction(req.treasuryAccountId(), "DEBIT", amount, desc);
            }
            case CREDIT -> { /* fiado — sem movimento financeiro até pagamento */ }
            case STORE_CREDIT -> {
                redeemVoucherForPayment(req.reference(), amount, invoice);
            }
        }
    }

    private void redeemVoucherForPayment(String voucherCode, BigDecimal amount, Invoice invoice) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new BusinessRuleException("Código do vale de compras é obrigatório.");
        }
        StoreVoucher voucher = storeVoucherRepository.findByCodeAndCompanyId(voucherCode.trim().toUpperCase(), invoice.getCompany().getId())
                .orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado ou inválido: " + voucherCode));

        if (voucher.getStatus() != StoreVoucherStatus.ACTIVE && voucher.getStatus() != StoreVoucherStatus.PARTIALLY_USED) {
            throw new BusinessRuleException("O vale de compras " + voucherCode + " não está disponível para utilização (Estado: " + voucher.getStatus() + ").");
        }

        if (voucher.getExpiresAt().isBefore(java.time.LocalDate.now())) {
            voucher.setStatus(StoreVoucherStatus.EXPIRED);
            storeVoucherRepository.save(voucher);
            throw new BusinessRuleException("O vale de compras " + voucherCode + " expirou em " + voucher.getExpiresAt());
        }

        if (voucher.getRemainingAmount().compareTo(amount) < 0) {
            throw new BusinessRuleException(String.format(
                    "Saldo insuficiente no vale de compras. Saldo disponível: %s MT, valor a abater: %s MT.",
                    voucher.getRemainingAmount(), amount));
        }

        BigDecimal newRemaining = voucher.getRemainingAmount().subtract(amount).setScale(2, RoundingMode.HALF_UP);
        voucher.setRemainingAmount(newRemaining);
        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            voucher.setStatus(StoreVoucherStatus.FULLY_REDEEMED);
        } else {
            voucher.setStatus(StoreVoucherStatus.PARTIALLY_USED);
        }
        storeVoucherRepository.save(voucher);
        auditLogService.logCurrent("STORE_VOUCHER_REDEEM",
                "Abatido " + amount + " MT do vale " + voucher.getCode() + " na venda " + invoice.getInvoiceNumber()
                        + ". Saldo restante: " + newRemaining + " MT.");
    }

    @Transactional
    public StoreVoucherDTO getVoucher(String code, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("Código do vale não pode ser vazio.");
        }
        StoreVoucher voucher = storeVoucherRepository.findByCodeAndCompanyId(code.trim().toUpperCase(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado: " + code));

        if (voucher.getExpiresAt().isBefore(java.time.LocalDate.now()) && voucher.getStatus() == StoreVoucherStatus.ACTIVE) {
            voucher.setStatus(StoreVoucherStatus.EXPIRED);
            voucher = storeVoucherRepository.save(voucher);
        }
        return toDTO(voucher);
    }

    public StoreVoucherDTO toDTO(StoreVoucher voucher) {
        if (voucher == null) return null;
        return new StoreVoucherDTO(
                voucher.getId(),
                voucher.getCode(),
                voucher.getInitialAmount(),
                voucher.getRemainingAmount(),
                voucher.getCompany() != null ? voucher.getCompany().getId() : null,
                voucher.getClient() != null ? voucher.getClient().getId() : null,
                voucher.getClientName(),
                voucher.getCreditNote() != null ? voucher.getCreditNote().getId() : null,
                voucher.getCreditNote() != null ? voucher.getCreditNote().getNoteNumber() : null,
                voucher.getIssuedAt(),
                voucher.getExpiresAt(),
                voucher.getStatus() != null ? voucher.getStatus().name() : null,
                voucher.getCreatedBy()
        );
    }

    /**
     * Saída de stock (SALE) de todas as linhas da venda, no armazém do posto. Espelha o
     * {@code deductStockForInvoice} da faturação: o stock só se move depois de a venda estar
     * autorizada (sessão aberta, pagamentos válidos e limite de crédito respeitado).
     */
    private void deductStockForSale(Invoice invoice, Warehouse warehouse, Client client, String walkInLabel) {
        String clientLabel = walkInLabel != null ? client.getName() + " — " + walkInLabel : client.getName();
        String desc = String.format("Venda POS %s - Cliente %s", invoice.getInvoiceNumber(), clientLabel);
        for (InvoiceLine line : invoice.getLines()) {
            inventoryService.registerMovement(
                    line.getProduct(),
                    warehouse,
                    line.getQuantity().negate(),
                    "SALE",
                    line.getBatchNumber(),
                    line.getSerialNumber(),
                    desc
            );
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
        invoice.setStatus(invoice.deriveStatusFromPayments());
        return invoiceRepository.save(invoice);
    }

    public TillSessionDTO toDTO(TillSession s) {
        return new TillSessionDTO(
                s.getId(),
                s.getOperator(),
                s.getCurrentOperator(),
                s.getCompany() != null ? s.getCompany().getId() : null,
                s.getOpeningBalance(),
                s.getClosingBalanceExpected(),
                s.getClosingBalanceReal(),
                s.getDifference(),
                s.getOpenDate(),
                s.getCloseDate(),
                s.getStatus(),
                s.getClosingNotes(),
                s.getCashBreakdownJson()
        );
    }

    // ── Passagem de turno (Shift Handover) ──────────────────────────────────

    /**
     * Realiza a passagem de turno entre operadores dentro da mesma sessão.
     * Regista uma reconciliação parcial (contagem da gaveta no momento do handover)
     * e transfere a posse da sessão para o operador de entrada.
     */
    @Transactional
    public ShiftReconciliationDTO performShiftHandover(ShiftHandoverRequest request) {
        TillSession session = tillSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Impossível realizar passagem de turno numa sessão fechada.");
        }
        CurrentUserContext.requireCompany(session.getCompany().getId());

        // Validar que o operador de saída é o actual
        String effectiveOperator = session.getCurrentOperator() != null
                ? session.getCurrentOperator() : session.getOperator();
        if (!effectiveOperator.equalsIgnoreCase(request.outgoingOperator())) {
            throw new BusinessRuleException(
                    "O operador de saída ('" + request.outgoingOperator()
                            + "') não corresponde ao operador actual da sessão ('" + effectiveOperator + "').");
        }

        if (request.incomingOperator() == null || request.incomingOperator().isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o operador de entrada.");
        }
        if (request.outgoingOperator().equalsIgnoreCase(request.incomingOperator())) {
            throw new BusinessRuleException("O operador de entrada não pode ser o mesmo que o de saída.");
        }
        if (request.countedCash() == null) {
            throw new BusinessRuleException("É obrigatório indicar o valor contado na gaveta.");
        }

        BigDecimal expected = computeExpectedCash(session.getId(), session.getOpeningBalance());
        BigDecimal difference = request.countedCash().subtract(expected);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            PermissionGuard.requireManagerOrAdmin("passagem de turno com diferença");
        }

        // Registar reconciliação
        ShiftReconciliation recon = new ShiftReconciliation();
        recon.setTillSession(session);
        recon.setOutgoingOperator(request.outgoingOperator());
        recon.setIncomingOperator(request.incomingOperator());
        recon.setReconciledAt(LocalDateTime.now());
        recon.setExpectedCash(expected);
        recon.setCountedCash(request.countedCash());
        recon.setDifference(difference);
        recon.setCashBreakdownJson(request.cashBreakdownJson() != null && !request.cashBreakdownJson().isBlank()
                ? request.cashBreakdownJson().trim() : null);
        recon.setNotes(request.notes() != null && !request.notes().isBlank()
                ? request.notes().trim() : null);
        recon.setCreatedBy(request.outgoingOperator());
        recon.setCompany(session.getCompany());
        recon = shiftReconciliationRepository.save(recon);

        // Transferir posse da sessão
        session.setCurrentOperator(request.incomingOperator());
        tillSessionRepository.save(session);

        auditLogService.logCurrent("POS_SHIFT_HANDOVER",
                "Passagem de turno na sessão " + session.getId()
                        + ": " + request.outgoingOperator() + " -> " + request.incomingOperator()
                        + ". Esperado: " + expected + " MT; contado: " + request.countedCash()
                        + " MT; diferença: " + difference + " MT."
                        + (recon.getNotes() != null ? " Obs: " + recon.getNotes() : ""));

        return toDTO(recon);
    }

    @Transactional(readOnly = true)
    public List<ShiftReconciliationDTO> getShiftReconciliations(Long sessionId) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));
        CurrentUserContext.requireCompany(session.getCompany().getId());
        return shiftReconciliationRepository.findByTillSessionIdOrderByReconciledAtAsc(sessionId)
                .stream().map(this::toDTO).toList();
    }

    public ShiftReconciliationDTO toDTO(ShiftReconciliation r) {
        return new ShiftReconciliationDTO(
                r.getId(),
                r.getTillSession() != null ? r.getTillSession().getId() : null,
                r.getOutgoingOperator(),
                r.getIncomingOperator(),
                r.getReconciledAt(),
                r.getExpectedCash(),
                r.getCountedCash(),
                r.getDifference(),
                r.getCashBreakdownJson(),
                r.getNotes()
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
