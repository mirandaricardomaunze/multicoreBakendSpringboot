package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.InvoiceCommandService;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteCommandService;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnResultDTO;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;
import mz.multicore.erp.modules.pos.model.PaymentMethod;
import mz.multicore.erp.modules.pos.model.StoreVoucher;
import mz.multicore.erp.modules.pos.model.StoreVoucherStatus;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillMovementType;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class PosReturnAndVoucherService {

    private final InvoiceCommandService invoiceCommands;
    private final InvoiceNoteLookup noteLookup;
    private final InvoiceNoteCommandService noteCommands;
    private final CreditNoteService creditNoteService;
    private final StoreVoucherRepository storeVoucherRepository;
    private final TillMovementRepository tillMovementRepository;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final PosSessionAndShiftService sessionAndShiftService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public PosReturnAndVoucherService(InvoiceCommandService invoiceCommands,
                                      InvoiceNoteLookup noteLookup,
                                      InvoiceNoteCommandService noteCommands,
                                      CreditNoteService creditNoteService,
                                      StoreVoucherRepository storeVoucherRepository,
                                      TillMovementRepository tillMovementRepository,
                                      FinanceService financeService,
                                      AuditLogService auditLogService,
                                      PosSessionAndShiftService sessionAndShiftService,
                                      org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.invoiceCommands = invoiceCommands;
        this.noteLookup = noteLookup;
        this.noteCommands = noteCommands;
        this.creditNoteService = creditNoteService;
        this.storeVoucherRepository = storeVoucherRepository;
        this.tillMovementRepository = tillMovementRepository;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.sessionAndShiftService = sessionAndShiftService;
    }

    public POSReturnResultDTO returnSale(POSReturnRequest request) {
        PermissionGuard.requireManagerOrAdmin("registar devolução no POS");
        CurrentUserContext.requireCompany(request.companyId());

        Invoice invoice = invoiceCommands.lockForCompany(request.invoiceId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        String key = request.idempotencyKey();
        String fingerprint = key == null ? null : posReturnRequestFingerprint(request);
        if (key != null) {
            Optional<CreditNote> previous =
                    noteLookup.findCreditNoteByPosReturnKey(request.invoiceId(), key);
            if (previous.isPresent()) {
                CreditNote note = previous.get();
                if (!fingerprint.equals(note.getPosReturnRequestFingerprint())) {
                    throw new BusinessRuleException(
                            "A chave de idempotência desta devolução já foi usada com dados diferentes.");
                }
                StoreVoucher voucher = storeVoucherRepository.findByCreditNoteId(note.getId()).orElse(null);
                return new POSReturnResultDTO(
                        creditNoteService.findById(note.getId()), voucher == null ? null : toDTO(voucher));
            }
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Não é possível devolver uma fatura anulada.");
        }
        if (invoice.getSalesChannel() != SalesChannel.POS) {
            throw new BusinessRuleException("Use o fluxo comercial para devoluções de faturas que não são POS.");
        }

        CreditNoteDTO created = creditNoteService.create(new CreateCreditNoteRequest(
                invoice.getId(), "RETURN", request.warehouseId(), request.reason(), request.lines()
        ));
        if (key != null) {
            noteCommands.markCreditNotePosReturn(created.id(), key, fingerprint);
        }
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
        String description = "Reembolso POS " + note.noteNumber() + " (Fatura " + invoice.getInvoiceNumber() + ")";

        switch (method) {
            case CASH -> { refundCash(request.operator(), request.companyId(), amount, description); publishRefund(request, note, amount, true); return null; }
            case CARD, BANK_TRANSFER, MPESA, EMOLA -> {
                if (request.treasuryAccountId() == null) {
                    throw new BusinessRuleException("Conta de tesouraria é obrigatória para reembolso por " + method + ".");
                }
                financeService.registerTransaction(request.treasuryAccountId(), "CREDIT", amount, description);
                publishRefund(request, note, amount, false);
                return null;
            }
            case CREDIT -> { return null; }
            case STORE_CREDIT -> { return createStoreVoucherForReturn(invoice, note, amount, request.operator()); }
        }
        return null;
    }

    private void publishRefund(POSReturnRequest request, CreditNoteDTO note, BigDecimal amount, boolean cash) {
        if (eventPublisher != null) eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.CustomerRefundedEvent(
                request.companyId(), note.id(), note.noteNumber(), LocalDate.now(), amount, cash));
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
        voucher.setCreditNote(noteLookup.findCreditNote(note.id()).orElse(null));
        voucher.setIssuedAt(LocalDateTime.now());
        voucher.setExpiresAt(LocalDate.now().plusDays(90));
        voucher.setStatus(StoreVoucherStatus.ACTIVE);
        voucher.setCreatedBy(operator != null ? operator : "SYSTEM");
        voucher = storeVoucherRepository.save(voucher);

        auditLogService.logCurrent("STORE_VOUCHER_ISSUED",
                "Emitido vale de compras " + code + " no valor de " + amount + " MT para " + clientName
                        + " associado à nota de crédito " + note.noteNumber());
        return voucher;
    }

    private String generateUniqueVoucherCode() {
        SecureRandom rng = new SecureRandom();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("VALE-");
            for (int i = 0; i < 4; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
            sb.append("-");
            for (int i = 0; i < 4; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
            code = sb.toString();
            if (++attempts > 50) { code = "VALE-" + System.currentTimeMillis(); break; }
        } while (storeVoucherRepository.existsByCode(code));
        return code;
    }

    private void refundCash(String operator, Long companyId, BigDecimal amount, String description) {
        TillSession session = sessionAndShiftService.getActiveSession(operator, companyId)
                .orElseThrow(() -> new BusinessRuleException("Abra uma sessão de caixa antes de devolver dinheiro ao cliente."));
        BigDecimal expected = sessionAndShiftService.computeExpectedCash(session.getId(), session.getOpeningBalance());
        if (expected.compareTo(amount) < 0) {
            throw new BusinessRuleException("Saldo de caixa insuficiente para reembolso. Disponível: " + DisplayNumbers.money(expected) + " MT");
        }
        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(TillMovementType.REFUND);
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(operator);
        tillMovementRepository.save(movement);
        auditLogService.logCurrent("POS_REFUND_CASH", "Reembolso em numerário na sessão " + session.getId() + ": " + amount + " MT.");
    }

    public void redeemVoucherForPayment(String voucherCode, BigDecimal amount, Invoice invoice) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new BusinessRuleException("Código do vale de compras é obrigatório.");
        }
        StoreVoucher voucher = storeVoucherRepository.findByCodeAndCompanyId(voucherCode.trim().toUpperCase(), invoice.getCompany().getId())
                .orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado ou inválido: " + voucherCode));

        if (voucher.getStatus() != StoreVoucherStatus.ACTIVE && voucher.getStatus() != StoreVoucherStatus.PARTIALLY_USED) {
            throw new BusinessRuleException("O vale de compras " + voucherCode + " não está disponível para utilização (Estado: " + voucher.getStatus() + ").");
        }
        if (voucher.getExpiresAt().isBefore(LocalDate.now())) {
            voucher.setStatus(StoreVoucherStatus.EXPIRED);
            storeVoucherRepository.save(voucher);
            throw new BusinessRuleException("O vale de compras " + voucherCode + " expirou em " + voucher.getExpiresAt());
        }
        if (voucher.getRemainingAmount().compareTo(amount) < 0) {
            throw new BusinessRuleException(String.format(
                    "Saldo insuficiente no vale de compras. Saldo disponível: %s MT, valor a abater: %s MT.",
                    DisplayNumbers.money(voucher.getRemainingAmount()), DisplayNumbers.money(amount)));
        }
        BigDecimal newRemaining = voucher.getRemainingAmount().subtract(amount).setScale(2, RoundingMode.HALF_UP);
        voucher.setRemainingAmount(newRemaining);
        voucher.setStatus(newRemaining.compareTo(BigDecimal.ZERO) == 0 ? StoreVoucherStatus.FULLY_REDEEMED : StoreVoucherStatus.PARTIALLY_USED);
        storeVoucherRepository.save(voucher);
        auditLogService.logCurrent("STORE_VOUCHER_REDEEM",
                "Abatido " + amount + " MT do vale " + voucher.getCode() + " na venda " + invoice.getInvoiceNumber()
                        + ". Saldo restante: " + newRemaining + " MT.");
    }

    public StoreVoucherDTO getVoucher(String code, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("Código do vale não pode ser vazio.");
        }
        StoreVoucher voucher = storeVoucherRepository.findByCodeAndCompanyId(code.trim().toUpperCase(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado: " + code));
        if (voucher.getExpiresAt().isBefore(LocalDate.now()) && voucher.getStatus() == StoreVoucherStatus.ACTIVE) {
            voucher.setStatus(StoreVoucherStatus.EXPIRED);
            voucher = storeVoucherRepository.save(voucher);
        }
        return toDTO(voucher);
    }

    public StoreVoucherDTO toDTO(StoreVoucher voucher) {
        if (voucher == null) return null;
        return new StoreVoucherDTO(
                voucher.getId(), voucher.getCode(), voucher.getInitialAmount(), voucher.getRemainingAmount(),
                voucher.getCompany() != null ? voucher.getCompany().getId() : null,
                voucher.getClient() != null ? voucher.getClient().getId() : null,
                voucher.getClientName(),
                voucher.getCreditNote() != null ? voucher.getCreditNote().getId() : null,
                voucher.getCreditNote() != null ? voucher.getCreditNote().getNoteNumber() : null,
                voucher.getIssuedAt(), voucher.getExpiresAt(),
                voucher.getStatus() != null ? voucher.getStatus().name() : null, voucher.getCreatedBy()
        );
    }

    public static PaymentMethod parsePaymentMethod(String method) {
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

    private static String posReturnRequestFingerprint(POSReturnRequest request) {
        StringBuilder canonical = new StringBuilder(320);
        appendFingerprintPart(canonical, request.operator());
        appendFingerprintPart(canonical, request.companyId());
        appendFingerprintPart(canonical, request.invoiceId());
        appendFingerprintPart(canonical, request.warehouseId());
        appendFingerprintPart(canonical, request.reason());
        appendFingerprintPart(canonical, request.refundMethod());
        appendFingerprintPart(canonical, request.treasuryAccountId());
        request.lines().stream()
                .sorted(Comparator.comparing(CreateCreditNoteLineRequest::invoiceLineId))
                .forEach(line -> {
                    appendFingerprintPart(canonical, line.invoiceLineId());
                    appendFingerprintPart(canonical, canonicalDecimal(line.quantity()));
                });
        return sha256Hex(canonical.toString());
    }

    static String canonicalDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    static void appendFingerprintPart(StringBuilder target, Object value) {
        String text = value == null ? "<null>" : value.toString().trim();
        target.append(text.length()).append(':').append(text).append('|');
    }

    static String sha256Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }
}
