package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceInstallment;
import mz.multicore.erp.modules.comercial.repository.InvoiceInstallmentRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstallmentPlanGenerator {

    private final InvoiceInstallmentRepository installmentRepository;
    private final InvoiceRepository invoiceRepository;

    public InstallmentPlanGenerator(InvoiceInstallmentRepository installmentRepository,
                                    InvoiceRepository invoiceRepository) {
        this.installmentRepository = installmentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<InvoiceInstallmentDTO> createInstallmentPlan(Long companyId, CreateInstallmentPlanRequest request) {
        if (companyId == null) {
            companyId = CurrentUserContext.getCurrentCompanyId();
        }
        if (companyId == null) {
            throw new BusinessRuleException("A empresa activa é obrigatória.");
        }
        if (request.invoiceId() == null) {
            throw new BusinessRuleException("A fatura é obrigatória para o plano de prestações.");
        }
        if (request.numberOfInstallments() <= 0) {
            throw new BusinessRuleException("O número de prestações deve ser pelo menos 1.");
        }

        final Long currentCompId = companyId;
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada: " + request.invoiceId()));

        if (!invoice.getCompany().getId().equals(currentCompId)) {
            throw new BusinessRuleException("A fatura não pertence à empresa activa.");
        }

        BigDecimal invoiceTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        if (invoiceTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("A fatura com valor zero não permite criação de prestações.");
        }

        installmentRepository.deleteByInvoiceIdAndCompanyId(invoice.getId(), companyId);

        int count = request.numberOfInstallments();
        int intervalDays = request.intervalDays() > 0 ? request.intervalDays() : 30;
        LocalDate firstDueDate = request.firstDueDate() != null ? request.firstDueDate() : LocalDate.now().plusDays(intervalDays);
        List<BigDecimal> amounts = resolveAmounts(request, count, invoiceTotal);

        List<InvoiceInstallment> savedList = new ArrayList<>();
        BigDecimal paidRemaining = invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            InvoiceInstallment inst = new InvoiceInstallment();
            inst.setInvoice(invoice);
            inst.setCompany(invoice.getCompany());
            inst.setInstallmentNumber(i + 1);
            inst.setTotalInstallments(count);
            inst.setDueDate(firstDueDate.plusDays((long) i * intervalDays));
            inst.setAmount(amounts.get(i));
            inst.setRemainingBalance(amounts.get(i));
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setNotes("Prestação " + (i + 1) + " de " + count);

            if (paidRemaining.compareTo(BigDecimal.ZERO) > 0) {
                paidRemaining = inst.applyPayment(paidRemaining);
            } else {
                inst.updateStatusBasedOnBalance();
            }

            savedList.add(installmentRepository.save(inst));
        }

        return savedList.stream().map(this::toDTO).toList();
    }

    private List<BigDecimal> resolveAmounts(CreateInstallmentPlanRequest request, int count, BigDecimal invoiceTotal) {
        List<BigDecimal> amounts = new ArrayList<>();
        if (request.customAmounts() != null && request.customAmounts().size() == count) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal amt : request.customAmounts()) {
                if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Todas as prestações personalizadas devem ter valor superior a zero.");
                }
                sum = sum.add(amt.setScale(2, RoundingMode.HALF_UP));
                amounts.add(amt.setScale(2, RoundingMode.HALF_UP));
            }
            if (sum.compareTo(invoiceTotal.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new BusinessRuleException("A soma das prestações (" + sum + " MT) não corresponde ao total da fatura (" + invoiceTotal + " MT).");
            }
        } else {
            BigDecimal base = invoiceTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.FLOOR);
            BigDecimal remainder = invoiceTotal.subtract(base.multiply(BigDecimal.valueOf(count)));
            for (int i = 0; i < count; i++) {
                amounts.add((i == count - 1 ? base.add(remainder) : base).setScale(2, RoundingMode.HALF_UP));
            }
        }
        return amounts;
    }

    public InvoiceInstallmentDTO toDTO(InvoiceInstallment i) {
        return new InvoiceInstallmentDTO(
                i.getId(),
                i.getInvoice() != null ? i.getInvoice().getId() : null,
                i.getInstallmentNumber(),
                i.getTotalInstallments(),
                i.getDueDate(),
                i.getAmount(),
                i.getPaidAmount(),
                i.getRemainingBalance(),
                i.getStatus(),
                i.getNotes()
        );
    }
}
