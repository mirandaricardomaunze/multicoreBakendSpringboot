package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.financeira.dto.CashFlowItemDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Recolhe saldos de tesouraria, recebíveis e exigíveis classificados por balde temporal. */
@Component
public class CashFlowDataCollector {

    public record CollectedCashFlowData(
            BigDecimal cashBalance,
            BigDecimal bankBalance,
            BigDecimal availableLiquidity,
            List<CashFlowItemDTO> receivables,
            BigDecimal totalReceivables,
            List<CashFlowItemDTO> payables,
            BigDecimal totalPayables
    ) {}

    private final TreasuryAccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final PurchaseRepository purchaseRepository;

    public CashFlowDataCollector(TreasuryAccountRepository accountRepository,
                                 InvoiceRepository invoiceRepository,
                                 PurchaseRepository purchaseRepository) {
        this.accountRepository = accountRepository;
        this.invoiceRepository = invoiceRepository;
        this.purchaseRepository = purchaseRepository;
    }

    public CollectedCashFlowData collect(Long companyId, LocalDate today) {
        List<TreasuryAccount> accounts = companyId != null
                ? accountRepository.findByCompanyIdOrderByName(companyId)
                : accountRepository.findAll();

        BigDecimal cashBalance = BigDecimal.ZERO;
        BigDecimal bankBalance = BigDecimal.ZERO;
        for (TreasuryAccount acc : accounts) {
            BigDecimal b = acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO;
            if (acc.getAccountType() == TreasuryAccountType.CASH) {
                cashBalance = cashBalance.add(b);
            } else {
                bankBalance = bankBalance.add(b);
            }
        }
        BigDecimal availableLiquidity = cashBalance.add(bankBalance);

        List<Invoice> invoices = companyId != null
                ? invoiceRepository.findByCompanyId(companyId)
                : invoiceRepository.findAll();
        List<CashFlowItemDTO> allReceivables = new ArrayList<>();
        BigDecimal totalReceivables = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getStatus() == InvoiceStatus.APPROVED && inv.outstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal outstanding = inv.outstandingAmount();
                LocalDate dueDate = inv.effectiveDueDate() != null ? inv.effectiveDueDate() : inv.issueDate();
                if (dueDate == null) dueDate = today;

                String bucket = determineBucket(dueDate, today);
                String clientName = inv.getClient() != null
                        ? inv.getClient().getName()
                        : (inv.getCustomerName() != null ? inv.getCustomerName() : "Cliente");

                allReceivables.add(new CashFlowItemDTO(
                        inv.getId(), "RECEIVABLE",
                        inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "FT-" + inv.getId(),
                        clientName, dueDate, outstanding, bucket
                ));
                totalReceivables = totalReceivables.add(outstanding);
            }
        }

        List<Purchase> purchases = companyId != null
                ? purchaseRepository.findByCompanyId(companyId)
                : purchaseRepository.findAll();
        List<CashFlowItemDTO> allPayables = new ArrayList<>();
        BigDecimal totalPayables = BigDecimal.ZERO;

        for (Purchase pur : purchases) {
            if ("COMPLETED".equalsIgnoreCase(pur.getStatus()) && pur.getOutstanding().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal outstanding = pur.getOutstanding();
                LocalDate purchaseDate = pur.getPurchaseDate() != null ? pur.getPurchaseDate().toLocalDate() : today;
                LocalDate dueDate = purchaseDate.plusDays(30);
                String bucket = determineBucket(dueDate, today);
                String supplierName = pur.getSupplier() != null ? pur.getSupplier().getName() : "Fornecedor";

                allPayables.add(new CashFlowItemDTO(
                        pur.getId(), "PAYABLE",
                        pur.getPurchaseNumber() != null ? pur.getPurchaseNumber() : "V/FT-" + pur.getId(),
                        supplierName, dueDate, outstanding, bucket
                ));
                totalPayables = totalPayables.add(outstanding);
            }
        }

        return new CollectedCashFlowData(
                cashBalance, bankBalance, availableLiquidity,
                allReceivables, totalReceivables,
                allPayables, totalPayables
        );
    }

    static String determineBucket(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) return "OVERDUE";
        if (dueDate.isEqual(today)) return "TODAY";
        if (!dueDate.isAfter(today.plusDays(7))) return "DAYS_1_7";
        if (!dueDate.isAfter(today.plusDays(15))) return "DAYS_8_15";
        if (!dueDate.isAfter(today.plusDays(30))) return "DAYS_16_30";
        if (!dueDate.isAfter(today.plusDays(60))) return "DAYS_31_60";
        return "DAYS_PLUS_60";
    }
}
