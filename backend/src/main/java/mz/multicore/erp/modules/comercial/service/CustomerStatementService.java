package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serviço de geração de extrato de conta corrente de clientes com saldo progressivo.
 */
@Service
public class CustomerStatementService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;

    public CustomerStatementService(
            ClientRepository clientRepository,
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository
    ) {
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
    }

    @Transactional(readOnly = true)
    public CustomerStatementDTO getStatement(Long clientId, LocalDate startDate, LocalDate endDate) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (clientId == null) {
            throw new BusinessRuleException("Identificador de cliente é obrigatório.");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado: " + clientId));

        LocalDate from = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate to = endDate != null ? endDate : LocalDate.now();

        List<RawMovement> events = new ArrayList<>();

        // 1. Facturas
        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId);
        BigDecimal overdue = BigDecimal.ZERO;
        for (Invoice inv : invoices) {
            if (inv.getClient() == null || !clientId.equals(inv.getClient().getId())) {
                continue;
            }
            if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                continue;
            }
            LocalDate invDate = inv.issueDate() != null ? inv.issueDate() : (inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate() : LocalDate.now());
            LocalDateTime ts = inv.getCreatedAt() != null ? inv.getCreatedAt() : invDate.atStartOfDay();
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            events.add(new RawMovement(
                    invDate,
                    "FT",
                    inv.getInvoiceNumber(),
                    "Factura a Crédito",
                    inv.getInvoiceNumber(),
                    amount,
                    BigDecimal.ZERO,
                    ts
            ));

            if (inv.getDueDate() != null && inv.getDueDate().isBefore(to)) {
                BigDecimal balDue = inv.outstandingAmount();
                if (balDue.signum() > 0) {
                    overdue = overdue.add(balDue);
                }
            }
        }

        // 2. Notas de Débito
        List<DebitNote> debitNotes = debitNoteRepository.findByCompanyId(companyId);
        for (DebitNote dn : debitNotes) {
            if (dn.getClient() == null || !clientId.equals(dn.getClient().getId())) {
                continue;
            }
            if (dn.getStatus() == NoteStatus.CANCELLED || dn.getStatus() == NoteStatus.REJECTED) {
                continue;
            }
            LocalDate dnDate = dn.getIssueDate().toLocalDate();
            LocalDateTime ts = dn.getCreatedAt() != null ? dn.getCreatedAt() : dn.getIssueDate();
            BigDecimal amount = dn.getTotalAmount() != null ? dn.getTotalAmount() : BigDecimal.ZERO;
            String desc = "Nota de Débito" + (dn.getDescription() != null && !dn.getDescription().isBlank() ? " — " + dn.getDescription() : "");
            String ref = dn.getInvoice() != null ? dn.getInvoice().getInvoiceNumber() : "-";
            events.add(new RawMovement(
                    dnDate,
                    "ND",
                    dn.getNoteNumber(),
                    desc,
                    ref,
                    amount,
                    BigDecimal.ZERO,
                    ts
            ));
        }

        // 3. Recibos / Pagamentos
        List<Receipt> receipts = receiptRepository.findByCompanyId(companyId);
        for (Receipt rc : receipts) {
            if (rc.getInvoice() == null || rc.getInvoice().getClient() == null || !clientId.equals(rc.getInvoice().getClient().getId())) {
                continue;
            }
            if ("CANCELLED".equalsIgnoreCase(rc.getStatus())) {
                continue;
            }
            LocalDate rcDate = rc.getReceiptDate().toLocalDate();
            LocalDateTime ts = rc.getCreatedAt() != null ? rc.getCreatedAt() : rc.getReceiptDate();
            BigDecimal amount = rc.getAmountPaid() != null ? rc.getAmountPaid() : BigDecimal.ZERO;
            String method = rc.getPaymentMethod() != null ? rc.getPaymentMethod() : "Liquidada";
            String desc = "Recibo (" + method + ")";
            String ref = rc.getInvoice().getInvoiceNumber();
            events.add(new RawMovement(
                    rcDate,
                    "RC",
                    rc.getReceiptNumber(),
                    desc,
                    ref,
                    BigDecimal.ZERO,
                    amount,
                    ts
            ));
        }

        // 4. Notas de Crédito
        List<CreditNote> creditNotes = creditNoteRepository.findByCompanyId(companyId);
        for (CreditNote cn : creditNotes) {
            if (cn.getClient() == null || !clientId.equals(cn.getClient().getId())) {
                continue;
            }
            if (cn.getStatus() == NoteStatus.CANCELLED || cn.getStatus() == NoteStatus.REJECTED) {
                continue;
            }
            LocalDate cnDate = cn.getIssueDate().toLocalDate();
            LocalDateTime ts = cn.getCreatedAt() != null ? cn.getCreatedAt() : cn.getIssueDate();
            BigDecimal amount = cn.getTotalAmount() != null ? cn.getTotalAmount() : BigDecimal.ZERO;
            String reason = cn.getReason() != null ? cn.getReason().name() : "Regularização";
            String desc = "Nota de Crédito (" + reason + ")";
            String ref = cn.getInvoice() != null ? cn.getInvoice().getInvoiceNumber() : "-";
            events.add(new RawMovement(
                    cnDate,
                    "NC",
                    cn.getNoteNumber(),
                    desc,
                    ref,
                    BigDecimal.ZERO,
                    amount,
                    ts
            ));
        }

        // Ordenação cronológica estrita
        events.sort(Comparator.comparing(RawMovement::date)
                .thenComparing(RawMovement::timestamp)
                .thenComparing(RawMovement::documentNumber));

        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<CustomerStatementLineDTO> lines = new ArrayList<>();

        for (RawMovement mov : events) {
            if (mov.date().isBefore(from)) {
                openingBalance = openingBalance.add(mov.debit()).subtract(mov.credit());
            }
        }

        BigDecimal running = openingBalance;
        for (RawMovement mov : events) {
            if (!mov.date().isBefore(from) && !mov.date().isAfter(to)) {
                running = running.add(mov.debit()).subtract(mov.credit());
                totalDebits = totalDebits.add(mov.debit());
                totalCredits = totalCredits.add(mov.credit());

                lines.add(new CustomerStatementLineDTO(
                        mov.date(),
                        mov.type(),
                        mov.documentNumber(),
                        mov.description(),
                        mov.reference(),
                        mov.debit(),
                        mov.credit(),
                        running
                ));
            }
        }

        BigDecimal closingBalance = openingBalance.add(totalDebits).subtract(totalCredits);

        return new CustomerStatementDTO(
                client.getId(),
                client.getName(),
                client.getTaxId(),
                client.getAddress(),
                client.getEmail(),
                null,
                from,
                to,
                openingBalance,
                totalDebits,
                totalCredits,
                closingBalance,
                overdue,
                lines
        );
    }

    private record RawMovement(
            LocalDate date,
            String type,
            String documentNumber,
            String description,
            String reference,
            BigDecimal debit,
            BigDecimal credit,
            LocalDateTime timestamp
    ) {}
}
