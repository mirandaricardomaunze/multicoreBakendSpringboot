package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.model.PayrollDeduction;
import mz.multicore.erp.modules.hr.repository.PayrollDeductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Consultas e projecção dos compromissos salariais. */
@Service
public class PayrollDeductionQueryService {
    private final PayrollDeductionRepository repository;
    private final PayrollDeductionApplicationService applicationService;

    public PayrollDeductionQueryService(PayrollDeductionRepository repository,
                                        PayrollDeductionApplicationService applicationService) {
        this.repository = repository;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public List<PayrollDeductionDTO> list() {
        return repository.findAllByCompany(companyId()).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollDeductionDTO> listForEmployee(Long employeeId) {
        return repository.findByEmployee(employeeId, companyId()).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollDeductionDTO> outstandingFor(Long employeeId) {
        return listForEmployee(employeeId).stream()
                .filter(item -> !item.settled() && item.outstandingAmount().signum() > 0)
                .filter(PayrollDeductionDTO::paidOut).toList();
    }

    PayrollDeductionDTO toDTO(PayrollDeduction deduction) {
        BigDecimal applied = applicationService.applied(deduction);
        return new PayrollDeductionDTO(deduction.getId(), deduction.getEmployee().getId(),
                deduction.getEmployee().getName(), deduction.getKind().name(), deduction.getKind().getLabel(),
                deduction.getDescription(), deduction.getPrincipalAmount(), deduction.getInstallmentAmount(),
                deduction.getInstallments(), deduction.getStartDate(), deduction.getEndDate(), applied,
                deduction.outstanding(applied), deduction.isSettled(applied), deduction.isPaidOut(),
                deduction.isActive(), deduction.getNotes());
    }

    private Long companyId() { return CurrentUserContext.requireCurrentCompanyId(); }
}
