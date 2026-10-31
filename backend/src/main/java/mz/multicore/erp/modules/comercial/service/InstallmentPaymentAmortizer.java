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
