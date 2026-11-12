package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceCommandService;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteCommandService;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.comercial.service.OrderCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.ReceivablesService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnResultDTO;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;
import mz.multicore.erp.modules.pos.dto.TillMovementDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import mz.multicore.erp.modules.pos.model.ShiftReconciliation;
import mz.multicore.erp.modules.pos.model.StoreVoucher;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.ShiftReconciliationRepository;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class POSService {

    private final TillSessionRepository tillSessionRepository;
    private final CompanyLookup companyLookup;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final PosSessionAndShiftService sessionAndShiftService;
    private final PosZReportCalculator zReportCalculator;
    private final PosCheckoutAndPaymentService checkoutAndPaymentService;
    private final PosReturnAndVoucherService returnAndVoucherService;

    @Autowired
    public POSService(
            TillSessionRepository tillSessionRepository, CompanyLookup companyLookup,
            FinanceService financeService, AuditLogService auditLogService,
            PosSessionAndShiftService sessionAndShiftService, PosZReportCalculator zReportCalculator,
            PosCheckoutAndPaymentService checkoutAndPaymentService, PosReturnAndVoucherService returnAndVoucherService) {
        this.tillSessionRepository = tillSessionRepository;
        this.companyLookup = companyLookup;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.sessionAndShiftService = sessionAndShiftService;
        this.zReportCalculator = zReportCalculator;
        this.checkoutAndPaymentService = checkoutAndPaymentService;
        this.returnAndVoucherService = returnAndVoucherService;
    }

    public POSService(
            TillSessionRepository tillSessionRepository, TillMovementRepository tillMovementRepository,
            InvoiceLookup invoiceLookup, InvoiceCommandService invoiceCommands, ClientLookup clientLookup,
            ProductLookup productLookup, WarehouseLookup warehouseLookup,
            CompanyLookup companyLookup, TreasuryAccountRepository treasuryAccountRepository,
            InventoryService inventoryService, FinanceService financeService,
            PaymentEntryRepository paymentEntryRepository, WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            CreditNoteService creditNoteService, ReceivablesService receivablesService,
            ApplicationEventPublisher eventPublisher, StoreVoucherRepository storeVoucherRepository,
            InvoiceNoteLookup noteLookup, InvoiceNoteCommandService noteCommands) {
        this(tillSessionRepository, tillMovementRepository, invoiceLookup, invoiceCommands, clientLookup,
                productLookup, warehouseLookup, companyLookup, treasuryAccountRepository,
                inventoryService, financeService, paymentEntryRepository, walkInClientProvider,
                documentNumberService, auditLogService, creditNoteService, receivablesService,
                eventPublisher, storeVoucherRepository, noteLookup, noteCommands, null, null);
    }

    public POSService(
            TillSessionRepository tillSessionRepository, TillMovementRepository tillMovementRepository,
            InvoiceLookup invoiceLookup, InvoiceCommandService invoiceCommands, ClientLookup clientLookup,
            ProductLookup productLookup, WarehouseLookup warehouseLookup,
            CompanyLookup companyLookup, TreasuryAccountRepository treasuryAccountRepository,
            InventoryService inventoryService, FinanceService financeService,
            PaymentEntryRepository paymentEntryRepository, WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            CreditNoteService creditNoteService, ReceivablesService receivablesService,
            ApplicationEventPublisher eventPublisher, StoreVoucherRepository storeVoucherRepository,
            InvoiceNoteLookup noteLookup, InvoiceNoteCommandService noteCommands, OrderCommandService orderCommands) {
        this(tillSessionRepository, tillMovementRepository, invoiceLookup, invoiceCommands, clientLookup,
                productLookup, warehouseLookup, companyLookup, treasuryAccountRepository,
                inventoryService, financeService, paymentEntryRepository, walkInClientProvider,
                documentNumberService, auditLogService, creditNoteService, receivablesService,
                eventPublisher, storeVoucherRepository, noteLookup, noteCommands, orderCommands, null);
    }

    public POSService(
            TillSessionRepository tillSessionRepository, TillMovementRepository tillMovementRepository,
            InvoiceLookup invoiceLookup, InvoiceCommandService invoiceCommands, ClientLookup clientLookup,
            ProductLookup productLookup, WarehouseLookup warehouseLookup,
            CompanyLookup companyLookup, TreasuryAccountRepository treasuryAccountRepository,
            InventoryService inventoryService, FinanceService financeService,
            PaymentEntryRepository paymentEntryRepository, WalkInClientProvider walkInClientProvider,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            CreditNoteService creditNoteService, ReceivablesService receivablesService,
            ApplicationEventPublisher eventPublisher, StoreVoucherRepository storeVoucherRepository,
            InvoiceNoteLookup noteLookup, InvoiceNoteCommandService noteCommands,
            OrderCommandService orderCommands, ShiftReconciliationRepository shiftReconciliationRepository) {
        PosSessionAndShiftService sss = new PosSessionAndShiftService(
                tillSessionRepository, tillMovementRepository, shiftReconciliationRepository, auditLogService);
        PosZReportCalculator zrc = new PosZReportCalculator(
                tillSessionRepository, tillMovementRepository, paymentEntryRepository, shiftReconciliationRepository, sss);
        PosReturnAndVoucherService rvs = new PosReturnAndVoucherService(
                invoiceCommands, noteLookup, noteCommands, creditNoteService, storeVoucherRepository,
                tillMovementRepository, financeService, auditLogService, sss);
        PosCheckoutAndPaymentService cps = new PosCheckoutAndPaymentService(
                invoiceLookup, invoiceCommands, clientLookup, productLookup, warehouseLookup,
                companyLookup, inventoryService, financeService, paymentEntryRepository,
                tillMovementRepository, walkInClientProvider, documentNumberService, auditLogService,
                receivablesService, eventPublisher, orderCommands, sss, rvs, null, null);
        this.tillSessionRepository = tillSessionRepository;
        this.companyLookup = companyLookup;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.sessionAndShiftService = sss;
        this.zReportCalculator = zrc;
        this.checkoutAndPaymentService = cps;
        this.returnAndVoucherService = rvs;
    }

    @Transactional(readOnly = true)
    public Optional<TillSession> getActiveSession(String operator, Long companyId) {
        return sessionAndShiftService.getActiveSession(operator, companyId);
    }

    @Transactional(readOnly = true)
    public List<TillSession> getSessionsByCompany(Long companyId) { return sessionAndShiftService.getSessionsByCompany(companyId); }

    @Transactional(readOnly = true)
    public List<TillMovement> getMovementsBySession(Long sessionId) { return sessionAndShiftService.getMovementsBySession(sessionId); }

    @Transactional
    public TillSession openSession(String operator, BigDecimal openingBalance, Long companyId) {
        PermissionGuard.requireSeller("abrir sessão de caixa");
        CurrentUserContext.requireCompany(companyId);
        sessionAndShiftService.requireOwnOperatorForSeller(operator);
        Company company = companyLookup.lockForUpdate(companyId);
        Optional<TillSession> active = tillSessionRepository.findActiveSessionForOperator(operator, "OPEN", companyId);
        if (active.isPresent()) {
            throw new BusinessRuleException("Já possui uma sessão de caixa aberta para este operador.");
        }
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

    @Transactional
    public TillSession closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId, String notes, String cashBreakdownJson) {
        PermissionGuard.requireSeller("fechar sessão de caixa");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        TillSession session = tillSessionRepository.lockByIdAndCompanyId(sessionId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));
        sessionAndShiftService.requireOwnOperatorForSeller(session.getCurrentOperator() != null
                ? session.getCurrentOperator() : session.getOperator());

        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("Sessão de caixa já se encontra fechada.");
        }

        BigDecimal expected = sessionAndShiftService.computeExpectedCash(sessionId, session.getOpeningBalance());
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

        if (depositAccountId != null) {
            BigDecimal netCash = expected.subtract(session.getOpeningBalance()).subtract(sessionAndShiftService.counterReceiptsNet(sessionId));
            if (netCash.compareTo(BigDecimal.ZERO) > 0) {
                financeService.registerTransaction(depositAccountId, "DEBIT", netCash,
                        "Depósito de fecho de caixa — sessão " + sessionId + " (operador " + session.getOperator() + ")");
            }
        }
        auditLogService.logCurrent("POS_CLOSE_SESSION",
                "Sessão " + sessionId + " fechada por " + session.getOperator()
                        + ". Esperado: " + expected + " MT; contado: " + closingBalanceReal
                        + " MT; diferença: " + session.getDifference() + " MT."
                        + (session.getClosingNotes() != null ? " Justificação: " + session.getClosingNotes() : ""));
        return session;
    }

    @Transactional(readOnly = true)
    public PosZReportDTO buildZReport(Long sessionId) { return zReportCalculator.buildZReport(sessionId); }

    @Transactional(readOnly = true)
    public List<PosSessionSummaryDTO> getSessionsHistory(Long companyId, LocalDateTime from, LocalDateTime to) {
        return zReportCalculator.getSessionsHistory(companyId, from, to);
    }

    @Transactional
    public TillMovement addCashMovement(Long sessionId, String type, BigDecimal amount, String description) {
        return sessionAndShiftService.addCashMovement(sessionId, type, amount, description);
    }

    @Transactional
    public Invoice checkout(POSCheckoutRequest request) { return checkoutAndPaymentService.checkout(request); }

    @Transactional
    public POSReturnResultDTO returnSale(POSReturnRequest request) { return returnAndVoucherService.returnSale(request); }

    @Transactional
    public StoreVoucherDTO getVoucher(String code, Long companyId) { return returnAndVoucherService.getVoucher(code, companyId); }

    public StoreVoucherDTO toDTO(StoreVoucher voucher) { return returnAndVoucherService.toDTO(voucher); }

    @Transactional
    public Invoice registerLatePayment(Long invoiceId, PosPaymentRequest req) {
        return checkoutAndPaymentService.registerLatePayment(invoiceId, req);
    }

    public TillSessionDTO toDTO(TillSession s) { return sessionAndShiftService.toDTO(s); }

    @Transactional
    public ShiftReconciliationDTO performShiftHandover(ShiftHandoverRequest request) {
        return sessionAndShiftService.performShiftHandover(request);
    }

    @Transactional(readOnly = true)
    public List<ShiftReconciliationDTO> getShiftReconciliations(Long sessionId) {
        return sessionAndShiftService.getShiftReconciliations(sessionId);
    }

    public ShiftReconciliationDTO toDTO(ShiftReconciliation r) { return sessionAndShiftService.toDTO(r); }

    public TillMovementDTO toDTO(TillMovement m) { return sessionAndShiftService.toDTO(m); }
}
