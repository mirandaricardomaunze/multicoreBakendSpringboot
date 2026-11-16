package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceInstallment;
import mz.multicore.erp.modules.comercial.repository.InvoiceInstallmentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class InstallmentPaymentAmortizer {

    private final InvoiceInstallmentRepository installmentRepository;
    private final InstallmentPlanGenerator planGenerator;

    public InstallmentPaymentAmortizer(InvoiceInstallmentRepository installmentRepository,
                                       InstallmentPlanGenerator planGenerator) {
        this.installmentRepository = installmentRepository;
        this.planGenerator = planGenerator;
    }

    public List<InvoiceInstallmentDTO> getInstallments(Long companyId, Long invoiceId) {
        if (companyId == null) {
            companyId = CurrentUserContext.getCurrentCompanyId();
        }
        return installmentRepository.findByInvoiceIdAndCompanyIdOrderByInstallmentNumberAsc(invoiceId, companyId)
                .stream()
                .map(planGenerator::toDTO)
                .toList();
    }

    /** Re-spreads what the invoice has paid over its instalments, in order (after a receipt or its cancellation). */
    public void syncWithInvoicePayments(Long companyId, Long invoiceId, BigDecimal invoicePaid) {
        BigDecimal remaining = invoicePaid == null ? BigDecimal.ZERO : invoicePaid;
        for (InvoiceInstallment inst : installmentRepository.findByInvoiceIdAndCompanyIdOrderByInstallmentNumberAsc(invoiceId, companyId)) {
            inst.resetPayments();
            remaining = inst.applyPayment(remaining);
            installmentRepository.save(inst);
        }
    }

    public List<InvoiceInstallmentDTO> applyPaymentToInvoiceInstallments(Long companyId, Long invoiceId, BigDecimal paymentAmount) {
        if (companyId == null) {
            companyId = CurrentUserContext.getCurrentCompanyId();
        }
        List<InvoiceInstallment> installments = installmentRepository
                .findByInvoiceIdAndCompanyIdOrderByInstallmentNumberAsc(invoiceId, companyId);

        BigDecimal remainingPayment = paymentAmount;
        for (InvoiceInstallment inst : installments) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;
            if (inst.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                remainingPayment = inst.applyPayment(remainingPayment);
                installmentRepository.save(inst);
            }
        }

        return installments.stream().map(planGenerator::toDTO).toList();
    }
}
