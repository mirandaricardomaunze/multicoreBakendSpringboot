package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceInstallment;
import mz.multicore.erp.modules.comercial.repository.InvoiceInstallmentRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceInstallmentService {

    private final InstallmentPlanGenerator planGenerator;
    private final InstallmentPaymentAmortizer paymentAmortizer;

    @Autowired
    public InvoiceInstallmentService(InstallmentPlanGenerator planGenerator,
                                     InstallmentPaymentAmortizer paymentAmortizer) {
        this.planGenerator = planGenerator;
        this.paymentAmortizer = paymentAmortizer;
    }

    public InvoiceInstallmentService(InvoiceInstallmentRepository installmentRepository,
                                     InvoiceRepository invoiceRepository) {
        InstallmentPlanGenerator gen = new InstallmentPlanGenerator(installmentRepository, invoiceRepository);
        this.planGenerator = gen;
        this.paymentAmortizer = new InstallmentPaymentAmortizer(installmentRepository, gen);
    }

    @Transactional
    public List<InvoiceInstallmentDTO> createInstallmentPlan(Long companyId, CreateInstallmentPlanRequest request) {
        return planGenerator.createInstallmentPlan(companyId, request);
    }

    @Transactional(readOnly = true)
    public List<InvoiceInstallmentDTO> getInstallments(Long companyId, Long invoiceId) {
        return paymentAmortizer.getInstallments(companyId, invoiceId);
    }

    @Transactional
    public List<InvoiceInstallmentDTO> applyPaymentToInvoiceInstallments(Long companyId, Long invoiceId, BigDecimal paymentAmount) {
        return paymentAmortizer.applyPaymentToInvoiceInstallments(companyId, invoiceId, paymentAmount);
    }

    public InvoiceInstallmentDTO toDTO(InvoiceInstallment i) {
        return planGenerator.toDTO(i);
    }
}
