package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.PayslipDeductionLineDTO;
import mz.multicore.erp.modules.hr.model.*;
import mz.multicore.erp.modules.hr.repository.PayrollDeductionRepository;
import mz.multicore.erp.modules.hr.repository.PayslipDeductionLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Aplica e liberta compromissos nas linhas de um recibo. */
@Service
public class PayrollDeductionApplicationService {
    private final PayrollDeductionRepository deductionRepository;
    private final PayslipDeductionLineRepository lineRepository;

    public PayrollDeductionApplicationService(PayrollDeductionRepository deductionRepository,
                                              PayslipDeductionLineRepository lineRepository) {
        this.deductionRepository = deductionRepository;
        this.lineRepository = lineRepository;
    }

    @Transactional
    public BigDecimal applyTo(Payslip payslip, BigDecimal availableAmount) {
        BigDecimal room = availableAmount == null ? BigDecimal.ZERO : availableAmount;
        BigDecimal total = BigDecimal.ZERO;
        for (PayrollDeduction deduction : deductionRepository.findApplicable(
                payslip.getEmployee().getId(), CurrentUserContext.requireCurrentCompanyId(), periodEnd(payslip))) {
            if (room.signum() <= 0) break;
            BigDecimal applied = applied(deduction);
            if (deduction.isSettled(applied)) continue;
            BigDecimal amount = deduction.getInstallmentAmount().min(deduction.outstanding(applied)).min(room);
            if (amount.signum() <= 0) continue;
            PayslipDeductionLine line = new PayslipDeductionLine();
            line.setPayslip(payslip);
            line.setDeduction(deduction);
            line.setDescription(deduction.getDescription());
            line.setAmount(amount);
            lineRepository.save(line);
            total = total.add(amount);
            room = room.subtract(amount);
        }
        return total;
    }

    @Transactional public void releaseFromPayslip(Long payslipId) { lineRepository.deleteByPayslipId(payslipId); }

    @Transactional(readOnly = true)
    public List<PayslipDeductionLineDTO> linesOf(Long payslipId) {
        return lineRepository.findByPayslipId(payslipId).stream().map(line -> new PayslipDeductionLineDTO(
                line.getId(), line.getDeduction().getId(), line.getDeduction().getKind().name(),
                line.getDeduction().getKind().getLabel(), line.getDescription(), line.getAmount())).toList();
    }

    BigDecimal applied(PayrollDeduction deduction) {
        BigDecimal sum = lineRepository.sumApplied(deduction.getId());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private LocalDate periodEnd(Payslip payslip) {
        LocalDate first = LocalDate.of(payslip.getYear(), payslip.getMonth(), 1);
        return first.withDayOfMonth(first.lengthOfMonth());
    }
}
