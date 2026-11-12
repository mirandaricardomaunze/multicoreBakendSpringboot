package mz.multicore.erp.modules.advances.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.advances.dto.ApplyAdvanceRequest;
import mz.multicore.erp.modules.advances.dto.CustomerAdvanceDTO;
import mz.multicore.erp.modules.advances.dto.ReceiveAdvanceRequest;
import mz.multicore.erp.modules.advances.model.CustomerAdvance;
import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;
import mz.multicore.erp.modules.advances.repository.CustomerAdvanceRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNoteReason;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.InvoiceCommandService;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer advances (SA-C10, {@code docs/ADIANTAMENTOS_SPEC.md}). Every step goes through the existing fiscal
 * services, never through SQL:
 * <ol>
 *   <li>receive: advance invoice with VAT (system product "Adiantamento") plus an ordinary receipt (cash in);</li>
 *   <li>apply: approved credit note on the advance invoice, then an offset receipt that moves the amount already
 *       paid onto the final invoice. The offset touches neither treasury nor the ledger, because the final
 *       invoice and the credit note already settle account 2101.</li>
 * </ol>
 */
@Service
public class CustomerAdvanceService {

    private final CustomerAdvanceRepository advanceRepository;
    private final AdvanceProductProvisioner productProvisioner;
    private final ComercialService comercialService;
    private final CreditNoteService creditNoteService;
    private final ClientLookup clientLookup;
    private final CompanyLookup companyLookup;
    private final OrderRepository orderRepository;
    private final InvoiceLookup invoiceLookup;
    private final InvoiceCommandService invoiceCommands;
    private final ReceiptRepository receiptRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public CustomerAdvanceService(CustomerAdvanceRepository advanceRepository, AdvanceProductProvisioner productProvisioner,
                                  ComercialService comercialService, CreditNoteService creditNoteService,
                                  ClientLookup clientLookup, CompanyLookup companyLookup,
                                  OrderRepository orderRepository, InvoiceLookup invoiceLookup,
                                  InvoiceCommandService invoiceCommands, ReceiptRepository receiptRepository,
                                  CreditNoteRepository creditNoteRepository,
                                  DocumentNumberService documentNumberService, AuditLogService auditLogService) {
        this.advanceRepository = advanceRepository;
        this.productProvisioner = productProvisioner;
        this.comercialService = comercialService;
        this.creditNoteService = creditNoteService;
        this.clientLookup = clientLookup;
        this.companyLookup = companyLookup;
        this.orderRepository = orderRepository;
        this.invoiceLookup = invoiceLookup;
        this.invoiceCommands = invoiceCommands;
        this.receiptRepository = receiptRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CustomerAdvanceDTO> list(Long companyId, CustomerAdvanceStatus status, Long clientId) {
        CurrentUserContext.requireCompany(companyId);
        return advanceRepository.findByCompany(companyId, status, clientId).stream().map(CustomerAdvanceService::toDto).toList();
    }

    @Transactional
    public CustomerAdvanceDTO receive(ReceiveAdvanceRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("receber adiantamento");
        var existing = advanceRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());
        if (AdvancePayments.isOffsetPayment(request.paymentMethod())) {
            throw new BusinessRuleException("Escolha a forma como o cliente pagou o sinal.");
        }
        Client client = clientLookup.requireForCompany(request.clientId(), request.companyId());
        if (WalkInClientProvider.WALK_IN_TAX_ID.equals(client.getTaxId())) {
            throw new BusinessRuleException("O adiantamento é de um cliente identificado, não do Consumidor Final.");
        }
        Company company = companyLookup.require(request.companyId());
        Order order = request.orderId() == null ? null : requireClientOrder(request, client);

        // The amount typed is what the client pays, VAT included; the invoice line carries the net price.
        Product product = productProvisioner.forCompany(company);
        BigDecimal rate = product.effectiveTaxRate();
        BigDecimal netPrice = request.amount().divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
        var line = new CreateInvoiceLineRequest(product.getId(), BigDecimal.ONE, rate, BigDecimal.ZERO, null, null, netPrice);
        var invoiceDto = comercialService.createInvoice(new CreateInvoiceRequest(
                client.getId(), request.companyId(), request.warehouseId(), List.of(line), LocalDate.now()));
        Invoice invoice = invoiceLookup.find(invoiceDto.id())
                .orElseThrow(() -> new BusinessRuleException("Fatura do adiantamento não encontrada."));
        if (invoice.getStatus() != InvoiceStatus.APPROVED) {
            throw new BusinessRuleException("A fatura do adiantamento ficou pendente de aprovação e não foi recebida.");
        }
        var receiptDto = comercialService.createReceipt(invoice.getId(), request.treasuryAccountId(),
                request.paymentMethod(), invoice.getTotalAmount());

        CustomerAdvance advance = new CustomerAdvance();
        advance.setCompany(company);
        advance.setClient(client);
        advance.setOrder(order);
        advance.setAmount(invoice.getTotalAmount());
        advance.setAdvanceInvoice(invoice);
        advance.setReceipt(receiptRepository.findById(receiptDto.id()).orElseThrow());
        advance.setNotes(blankToNull(request.notes()));
        advance.setRequestKey(request.requestKey());
        advance.setCreatedBy(CurrentUserContext.getUsername());
        advance = advanceRepository.save(advance);
        auditLogService.logCurrent("CUSTOMER_ADVANCE_RECEIVE", "Adiantamento de " + DisplayNumbers.money(advance.getAmount())
                + " MT recebido de " + client.getName() + " (fatura " + invoice.getInvoiceNumber()
                + ", recibo " + receiptDto.receiptNumber() + ").");
        return toDto(advance);
    }

    @Transactional
    public CustomerAdvanceDTO apply(Long advanceId, ApplyAdvanceRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("aplicar adiantamento");
        CustomerAdvance advance = advanceRepository.lockByIdAndCompanyId(advanceId, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Adiantamento não encontrado."));
        if (advance.getStatus() != CustomerAdvanceStatus.OPEN) {
            throw new BusinessRuleException("Este adiantamento já foi aplicado.");
        }
        Invoice target = invoiceCommands.lockForCompany(request.invoiceId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Fatura final não encontrada."));
        Invoice advanceInvoice = advance.getAdvanceInvoice();
        BigDecimal amount = advance.getAmount();
        requireTarget(advance, target, amount);

        CreditNoteDTO note = regularise(advanceInvoice, target);
        if (note.totalAmount() == null || note.totalAmount().compareTo(amount) != 0) {
            throw new BusinessRuleException("A nota de crédito do adiantamento não corresponde ao valor recebido.");
        }

        // The cash stays recorded once: it leaves the advance invoice and settles part of the final invoice.
        Invoice source = invoiceCommands.lockForCompany(advanceInvoice.getId(), request.companyId()).orElseThrow();
        source.setAmountPaid(source.getAmountPaid().subtract(amount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        source.setStatus(source.deriveStatusFromPayments());
        invoiceCommands.save(source);
        target.setAmountPaid(target.getAmountPaid().add(amount).setScale(2, RoundingMode.HALF_UP));
        target.setStatus(target.deriveStatusFromPayments());
        invoiceCommands.save(target);

        Receipt offset = new Receipt();
        offset.setInvoice(target);
        offset.setCompany(target.getCompany());
        offset.setTreasuryAccount(advance.getReceipt().getTreasuryAccount());
        offset.setAmountPaid(amount);
        offset.setPaymentMethod(AdvancePayments.OFFSET_PAYMENT_METHOD);
        offset.setReceiptDate(LocalDateTime.now());
        offset.setStatus("COMPLETED");
        offset.setReceiptNumber(documentNumberService.next(DocumentSeries.RECEIPT));
        offset.setCreatedBy(CurrentUserContext.getUsername());
        offset = receiptRepository.save(offset);

        advance.setStatus(CustomerAdvanceStatus.APPLIED);
        advance.setAppliedInvoice(target);
        advance.setCreditNote(creditNoteRepository.findById(note.id()).orElseThrow());
        advance.setOffsetReceipt(offset);
        advance.setAppliedAt(LocalDateTime.now());
        advance.setAppliedBy(CurrentUserContext.getUsername());
        advance = advanceRepository.save(advance);
        auditLogService.logCurrent("CUSTOMER_ADVANCE_APPLY", "Adiantamento " + advanceInvoice.getInvoiceNumber() + " de "
                + DisplayNumbers.money(amount) + " MT aplicado na fatura " + target.getInvoiceNumber()
                + " (nota de crédito " + note.noteNumber() + ", recibo de compensação " + offset.getReceiptNumber() + ").");
        return toDto(advance);
    }

    /**
     * The client gave up: an approved credit note cancels the advance invoice (base and VAT) and the advance receipt is
     * cancelled, which takes the money out of the till and reverses its ledger entry. Everything is undone with the
     * ordinary documents; no amount stays with the client or the company. Pending legal opinion (question 6 of
     * {@code docs/PEDIDO_PARECER_FATURA_ADIANTAMENTO.md}).
     */
    @Transactional
    public CustomerAdvanceDTO refund(Long advanceId, mz.multicore.erp.modules.advances.dto.RefundAdvanceRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("devolver adiantamento");
        CustomerAdvance advance = advanceRepository.lockByIdAndCompanyId(advanceId, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Adiantamento não encontrado."));
        if (advance.getStatus() != CustomerAdvanceStatus.OPEN) {
            throw new BusinessRuleException("Só se devolve um adiantamento em aberto; este já foi aplicado ou devolvido.");
        }
        Invoice advanceInvoice = advance.getAdvanceInvoice();
        InvoiceLine line = advanceInvoice.getLines().get(0);
        CreditNoteDTO created = creditNoteService.create(new CreateCreditNoteRequest(advanceInvoice.getId(),
                CreditNoteReason.CANCELLATION.name(), null, "Devolução do sinal ao cliente: " + request.reason().trim(),
                List.of(new CreateCreditNoteLineRequest(line.getId(), line.getQuantity()))));
        CreditNoteDTO note = creditNoteService.approve(created.id());
        comercialService.cancelReceipt(advance.getReceipt().getId(), "Devolução do sinal ao cliente: " + request.reason().trim());
        advance.setStatus(CustomerAdvanceStatus.REFUNDED);
        advance.setCreditNote(creditNoteRepository.findById(note.id()).orElseThrow());
        advance.setAppliedAt(LocalDateTime.now());
        advance.setAppliedBy(CurrentUserContext.getUsername());
        String notes = (advance.getNotes() == null ? "" : advance.getNotes() + " · ") + "Devolvido: " + request.reason().trim();
        advance.setNotes(notes.length() > 300 ? notes.substring(0, 300) : notes);
        advance = advanceRepository.save(advance);
        auditLogService.logCurrent("CUSTOMER_ADVANCE_REFUND", "Sinal " + advanceInvoice.getInvoiceNumber() + " de "
                + DisplayNumbers.money(advance.getAmount()) + " MT devolvido a " + advance.getClient().getName()
                + " (nota de crédito " + note.noteNumber() + "): " + request.reason().trim());
        return toDto(advance);
    }

    private void requireTarget(CustomerAdvance advance, Invoice target, BigDecimal amount) {
        if (target.getClient() == null || !target.getClient().getId().equals(advance.getClient().getId())) {
            throw new BusinessRuleException("O adiantamento só se aplica a uma fatura do mesmo cliente.");
        }
        if (target.getId().equals(advance.getAdvanceInvoice().getId()) || advanceRepository.existsByAdvanceInvoiceId(target.getId())) {
            throw new BusinessRuleException("Escolha a fatura final da encomenda, não uma fatura de adiantamento.");
        }
        if (target.getStatus() == null || !target.getStatus().isCollectable()) {
            throw new BusinessRuleException("A fatura " + target.getInvoiceNumber() + " não está por cobrar.");
        }
        if (target.outstandingAmount().compareTo(amount) < 0) {
            throw new BusinessRuleException("A fatura " + target.getInvoiceNumber() + " só tem "
                    + DisplayNumbers.money(target.outstandingAmount()) + " MT em dívida; o adiantamento é de "
                    + DisplayNumbers.money(amount) + " MT.");
        }
    }

    /** Credit note over the whole advance invoice, approved through the ordinary credit-note service. */
    private CreditNoteDTO regularise(Invoice advanceInvoice, Invoice target) {
        InvoiceLine line = advanceInvoice.getLines().get(0);
        CreditNoteDTO created = creditNoteService.create(new CreateCreditNoteRequest(advanceInvoice.getId(),
                CreditNoteReason.CANCELLATION.name(), null,
                "Regularização de adiantamento aplicado na fatura " + target.getInvoiceNumber(),
                List.of(new CreateCreditNoteLineRequest(line.getId(), line.getQuantity()))));
        return creditNoteService.approve(created.id());
    }

    private Order requireClientOrder(ReceiveAdvanceRequest request, Client client) {
        Order order = orderRepository.findById(request.orderId())
                .filter(o -> o.getCompany() != null && request.companyId().equals(o.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada nesta empresa."));
        if (order.getClient() == null || !order.getClient().getId().equals(client.getId())) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " é de outro cliente.");
        }
        return order;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static CustomerAdvanceDTO toDto(CustomerAdvance a) {
        return new CustomerAdvanceDTO(a.getId(), a.getClient().getId(), a.getClient().getName(),
                a.getOrder() == null ? null : a.getOrder().getId(),
                a.getOrder() == null ? null : a.getOrder().getOrderNumber(),
                a.getAmount(), a.getStatus(),
                a.getAdvanceInvoice().getId(), a.getAdvanceInvoice().getInvoiceNumber(),
                a.getReceipt().getId(), a.getReceipt().getReceiptNumber(), a.getReceipt().getPaymentMethod(),
                a.getAppliedInvoice() == null ? null : a.getAppliedInvoice().getId(),
                a.getAppliedInvoice() == null ? null : a.getAppliedInvoice().getInvoiceNumber(),
                a.getCreditNote() == null ? null : a.getCreditNote().getId(),
                a.getCreditNote() == null ? null : a.getCreditNote().getNoteNumber(),
                a.getOffsetReceipt() == null ? null : a.getOffsetReceipt().getReceiptNumber(),
                a.getNotes(), a.getCreatedBy(), a.getCreatedAt(), a.getAppliedAt());
    }
}
