package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o apuramento mensal do IVA — vendas (IVA liquidado) menos compras
 * (IVA deduzido) — para a declaração mensal à Autoridade Tributária.
 * Apenas faturas com estado APPROVED ou PAID são consideradas e apenas
 * compras não-CANCELADAS.
 */
@Service
public class FiscalSummaryService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseRepository purchaseRepository;
    private final mz.multicore.erp.modules.comercial.repository.CreditNoteRepository creditNoteRepository;
    private final mz.multicore.erp.modules.comercial.repository.DebitNoteRepository debitNoteRepository;

    public FiscalSummaryService(InvoiceRepository invoiceRepository, PurchaseRepository purchaseRepository) {
        this(invoiceRepository, purchaseRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FiscalSummaryService(InvoiceRepository invoiceRepository, PurchaseRepository purchaseRepository,
                                mz.multicore.erp.modules.comercial.repository.CreditNoteRepository creditNoteRepository,
                                mz.multicore.erp.modules.comercial.repository.DebitNoteRepository debitNoteRepository) {
        this.invoiceRepository = invoiceRepository;
        this.purchaseRepository = purchaseRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
    }

    @Transactional(readOnly = true)
    public IvaSummaryDTO computeMonth(Long companyId, int year, int month) {
        return computeMonth(companyId, year, month, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public IvaSummaryDTO computeMonth(Long companyId, int year, int month, BigDecimal previousCredit) {
        CurrentUserContext.requireCompany(companyId);
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        List<IvaSummaryDTO.IvaLineDTO> sales = new ArrayList<>();
        BigDecimal salesBase = BigDecimal.ZERO;
        BigDecimal outputTax = BigDecimal.ZERO;

        for (Invoice inv : invoiceRepository.findByCompanyId(companyId)) {
            // VAT is due when the invoice is issued, paid or not: a partially paid invoice counts in full.
            if (inv.getStatus() == null || !inv.getStatus().isRealisedSale()) continue;
            LocalDateTime created = inv.getCreatedAt();
            if (created == null || created.isBefore(start) || created.isAfter(end)) continue;

            BigDecimal base = nz(inv.getTotalBeforeTax());
            BigDecimal tax = nz(inv.getTaxAmount());
            BigDecimal total = nz(inv.getTotalAmount());
            salesBase = salesBase.add(base);
            outputTax = outputTax.add(tax);
            sales.add(new IvaSummaryDTO.IvaLineDTO(
                    inv.getInvoiceNumber(),
                    inv.getClient() != null ? inv.getClient().getName() : "—",
                    base, tax, total));
        }

        // Approved credit notes reduce the output tax of the month they are issued in; debit notes increase it.
        if (creditNoteRepository != null) {
            for (var note : creditNoteRepository.findByCompanyId(companyId)) {
                if (note.getStatus() != mz.multicore.erp.modules.comercial.model.NoteStatus.APPROVED || !inPeriod(note.getIssueDate(), start, end)) continue;
                salesBase = salesBase.subtract(nz(note.getTotalBeforeTax()));
                outputTax = outputTax.subtract(nz(note.getTaxAmount()));
                sales.add(new IvaSummaryDTO.IvaLineDTO(note.getNoteNumber(), note.getClient() != null ? note.getClient().getName() : "—",
                        nz(note.getTotalBeforeTax()).negate(), nz(note.getTaxAmount()).negate(), nz(note.getTotalAmount()).negate()));
            }
        }
        if (debitNoteRepository != null) {
            for (var note : debitNoteRepository.findByCompanyId(companyId)) {
                if (note.getStatus() != mz.multicore.erp.modules.comercial.model.NoteStatus.APPROVED || !inPeriod(note.getIssueDate(), start, end)) continue;
                salesBase = salesBase.add(nz(note.getTotalBeforeTax()));
                outputTax = outputTax.add(nz(note.getTaxAmount()));
                sales.add(new IvaSummaryDTO.IvaLineDTO(note.getNoteNumber(), note.getClient() != null ? note.getClient().getName() : "—",
                        nz(note.getTotalBeforeTax()), nz(note.getTaxAmount()), nz(note.getTotalAmount())));
            }
        }

        List<IvaSummaryDTO.IvaLineDTO> purchases = new ArrayList<>();
        BigDecimal purchasesBase = BigDecimal.ZERO;
        BigDecimal inputTax = BigDecimal.ZERO;

        for (Purchase p : purchaseRepository.findByCompanyId(companyId)) {
            if ("CANCELLED".equals(p.getStatus())) continue;
            LocalDateTime created = p.getPurchaseDate();
            if (created == null || created.isBefore(start) || created.isAfter(end)) continue;

            BigDecimal tax = nz(p.getTaxAmount());
            BigDecimal total = nz(p.getTotalAmount());
            BigDecimal base = total.subtract(tax);
            purchasesBase = purchasesBase.add(base);
            inputTax = inputTax.add(tax);
            purchases.add(new IvaSummaryDTO.IvaLineDTO(
                    p.getPurchaseNumber(),
                    p.getSupplier() != null ? p.getSupplier().getName() : "—",
                    base, tax, total));
        }

        BigDecimal prevCredit = previousCredit != null && previousCredit.compareTo(BigDecimal.ZERO) > 0
                ? previousCredit : BigDecimal.ZERO;
        BigDecimal rawPeriodDiff = outputTax.subtract(inputTax);
        BigDecimal netDue = rawPeriodDiff.subtract(prevCredit);
        BigDecimal payableAmount = netDue.compareTo(BigDecimal.ZERO) > 0 ? netDue : BigDecimal.ZERO;
        BigDecimal creditToCarry = netDue.compareTo(BigDecimal.ZERO) < 0 ? netDue.abs() : BigDecimal.ZERO;
        String fiscalStatus = payableAmount.compareTo(BigDecimal.ZERO) > 0 ? "A_PAGAR" : "CREDITO_A_TRANSPORTAR";

        return new IvaSummaryDTO(year, month, companyId,
                salesBase, outputTax, purchasesBase, inputTax, netDue,
                prevCredit, payableAmount, creditToCarry, fiscalStatus,
                sales, purchases);
    }

    private static boolean inPeriod(LocalDateTime date, LocalDateTime start, LocalDateTime end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Convenience for callers using {@link LocalDate}. */
    public IvaSummaryDTO computeMonth(Long companyId, LocalDate anyDayInMonth) {
        return computeMonth(companyId, anyDayInMonth.getYear(), anyDayInMonth.getMonthValue(), BigDecimal.ZERO);
    }
}
