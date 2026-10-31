package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coletor de eventos e movimentos financeiros da conta corrente de clientes (Facturas,
 * Notas de Débito, Recibos, Liquidações directas e Notas de Crédito).
 * Ver {@code docs/CUSTOMER_STATEMENT_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Component
public class CustomerStatementCollector {

    public record CollectedCustomerMovements(
            List<CustomerStatementMovement> movements,
            BigDecimal overdueAmount
    ) {}

    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;

    public CustomerStatementCollector(
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
    }

    public CollectedCustomerMovements collect(Long companyId, Long clientId, LocalDate to) {
        List<CustomerStatementMovement> events = new ArrayList<>();
        BigDecimal overdue = BigDecimal.ZERO;
        List<Invoice> clientInvoices = new ArrayList<>();

        // 1. Facturas de Venda Efectiva
        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId);
        for (Invoice inv : invoices) {
            if (inv.getClient() == null || !clientId.equals(inv.getClient().getId())) {
                continue;
            }
            if (inv.getStatus() == null || !inv.getStatus().isRealisedSale()) {
                continue;
            }
            clientInvoices.add(inv);
            LocalDate invDate = inv.issueDate() != null ? inv.issueDate()
                    : (inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate() : LocalDate.now());
            LocalDateTime ts = inv.getCreatedAt() != null ? inv.getCreatedAt() : invDate.atStartOfDay();
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            Long buId = inv.getBusinessUnit() != null ? inv.getBusinessUnit().getId() : null;
            String buName = inv.getBusinessUnit() != null ? inv.getBusinessUnit().getName() : null;
            events.add(new CustomerStatementMovement(
                    invDate, "FT",
                    inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "-",
                    "Factura a Crédito",
                    inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "-",
                    amount, BigDecimal.ZERO, ts, buId, buName
            ));

            LocalDate due = inv.effectiveDueDate();
            if (due != null && due.isBefore(to)) {
                BigDecimal balDue = inv.outstandingAmount();
                if (balDue.signum() > 0) {
                    overdue = overdue.add(balDue);
                }
            }
        }

        // 2. Notas de Débito
        List<DebitNote> debitNotes = debitNoteRepository.findByCompanyId(companyId);
        for (DebitNote dn : debitNotes) {
            if (dn.getClient() == null || !clientId.equals(dn.getClient().getId()) || dn.getStatus() != NoteStatus.APPROVED) {
                continue;
            }
            LocalDate dnDate = dn.getIssueDate() != null ? dn.getIssueDate().toLocalDate()
                    : (dn.getCreatedAt() != null ? dn.getCreatedAt().toLocalDate() : LocalDate.now());
            LocalDateTime ts = dn.getCreatedAt() != null ? dn.getCreatedAt()
                    : (dn.getIssueDate() != null ? dn.getIssueDate() : dnDate.atStartOfDay());
            BigDecimal amount = dn.getTotalAmount() != null ? dn.getTotalAmount() : BigDecimal.ZERO;
            String desc = "Nota de Débito" + (dn.getDescription() != null && !dn.getDescription().isBlank() ? " — " + dn.getDescription() : "");
            String ref = dn.getInvoice() != null && dn.getInvoice().getInvoiceNumber() != null ? dn.getInvoice().getInvoiceNumber() : "-";
            Long buId = dn.getInvoice() != null && dn.getInvoice().getBusinessUnit() != null ? dn.getInvoice().getBusinessUnit().getId() : null;
            String buName = dn.getInvoice() != null && dn.getInvoice().getBusinessUnit() != null ? dn.getInvoice().getBusinessUnit().getName() : null;
            events.add(new CustomerStatementMovement(
                    dnDate, "ND",
                    dn.getNoteNumber() != null ? dn.getNoteNumber() : "-",
                    desc, ref, amount, BigDecimal.ZERO, ts, buId, buName
            ));
        }

        // 3. Recibos / Pagamentos
        List<Receipt> receipts = receiptRepository.findByCompanyId(companyId);
        Map<Long, BigDecimal> receiptsPerInvoice = new HashMap<>();
        for (Receipt rc : receipts) {
            if (rc.getInvoice() == null || rc.getInvoice().getClient() == null
                    || !clientId.equals(rc.getInvoice().getClient().getId()) || "CANCELLED".equalsIgnoreCase(rc.getStatus())) {
                continue;
            }
            LocalDate rcDate = rc.getReceiptDate() != null ? rc.getReceiptDate().toLocalDate()
                    : (rc.getCreatedAt() != null ? rc.getCreatedAt().toLocalDate() : LocalDate.now());
            LocalDateTime ts = rc.getCreatedAt() != null ? rc.getCreatedAt()
                    : (rc.getReceiptDate() != null ? rc.getReceiptDate() : rcDate.atStartOfDay());
            BigDecimal amount = rc.getAmountPaid() != null ? rc.getAmountPaid() : BigDecimal.ZERO;
            if (rc.getInvoice().getId() != null) {
                receiptsPerInvoice.merge(rc.getInvoice().getId(), amount, BigDecimal::add);
            }
            String method = rc.getPaymentMethod() != null ? rc.getPaymentMethod() : "Liquidada";
            String ref = rc.getInvoice().getInvoiceNumber() != null ? rc.getInvoice().getInvoiceNumber() : "-";
            Long buId = rc.getInvoice().getBusinessUnit() != null ? rc.getInvoice().getBusinessUnit().getId() : null;
            String buName = rc.getInvoice().getBusinessUnit() != null ? rc.getInvoice().getBusinessUnit().getName() : null;
            events.add(new CustomerStatementMovement(
                    rcDate, "RC",
                    rc.getReceiptNumber() != null ? rc.getReceiptNumber() : "-",
                    "Recibo (" + method + ")", ref, BigDecimal.ZERO, amount, ts, buId, buName
            ));
        }

        // 3.1 Pagamentos a pronto / balcão registados directamente na fatura sem recibo autónomo
        for (Invoice inv : clientInvoices) {
            BigDecimal totalPaid = inv.getAmountPaid() != null ? inv.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal receiptSum = inv.getId() != null ? receiptsPerInvoice.getOrDefault(inv.getId(), BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal directSettlement = totalPaid.subtract(receiptSum);
            if (directSettlement.signum() > 0) {
                LocalDate invDate = inv.issueDate() != null ? inv.issueDate()
                        : (inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate() : LocalDate.now());
                LocalDateTime ts = (inv.getCreatedAt() != null ? inv.getCreatedAt() : invDate.atStartOfDay()).plusSeconds(1);
                Long buId = inv.getBusinessUnit() != null ? inv.getBusinessUnit().getId() : null;
                String buName = inv.getBusinessUnit() != null ? inv.getBusinessUnit().getName() : null;
                String docNum = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : ("INV-" + inv.getId());
                events.add(new CustomerStatementMovement(
                        invDate, "RC", "PG/" + docNum,
                        "Pagamento no Acto da Factura", docNum,
                        BigDecimal.ZERO, directSettlement, ts, buId, buName
                ));
            }
        }

        // 4. Notas de Crédito
        List<CreditNote> creditNotes = creditNoteRepository.findByCompanyId(companyId);
        for (CreditNote cn : creditNotes) {
            if (cn.getClient() == null || !clientId.equals(cn.getClient().getId()) || cn.getStatus() != NoteStatus.APPROVED) {
                continue;
            }
            LocalDate cnDate = cn.getIssueDate() != null ? cn.getIssueDate().toLocalDate()
                    : (cn.getCreatedAt() != null ? cn.getCreatedAt().toLocalDate() : LocalDate.now());
            LocalDateTime ts = cn.getCreatedAt() != null ? cn.getCreatedAt()
                    : (cn.getIssueDate() != null ? cn.getIssueDate() : cnDate.atStartOfDay());
            BigDecimal amount = cn.getTotalAmount() != null ? cn.getTotalAmount() : BigDecimal.ZERO;
            String reason = cn.getReason() != null ? cn.getReason().name() : "Regularização";
            String ref = cn.getInvoice() != null && cn.getInvoice().getInvoiceNumber() != null ? cn.getInvoice().getInvoiceNumber() : "-";
            Long buId = cn.getInvoice() != null && cn.getInvoice().getBusinessUnit() != null ? cn.getInvoice().getBusinessUnit().getId() : null;
            String buName = cn.getInvoice() != null && cn.getInvoice().getBusinessUnit() != null ? cn.getInvoice().getBusinessUnit().getName() : null;
            events.add(new CustomerStatementMovement(
                    cnDate, "NC",
                    cn.getNoteNumber() != null ? cn.getNoteNumber() : "-",
                    "Nota de Crédito (" + reason + ")", ref,
                    BigDecimal.ZERO, amount, ts, buId, buName
            ));
        }

        return new CollectedCustomerMovements(events, overdue);
    }
}
