package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.model.PayrollLiability;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityStatus;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayrollLiabilityRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Consultas de retenções da folha, alertas de prazo e apuramento do custo total do trabalhador. */
@Component
public class PayrollLiabilityQueryService {

    private final PayrollLiabilityRepository liabilityRepository;
    private final PayslipRepository payslipRepository;

    public PayrollLiabilityQueryService(PayrollLiabilityRepository liabilityRepository,
                                        PayslipRepository payslipRepository) {
        this.liabilityRepository = liabilityRepository;
        this.payslipRepository = payslipRepository;
    }

    public List<PayrollLiabilityDTO> list(Long companyId) {
        return liabilityRepository.findAllByCompany(companyId).stream()
                .map(PayrollLiabilityQueryService::toDTO)
                .toList();
    }

    public List<PayrollLiabilityDTO> dueAlerts(Long companyId, int dueAlertDays) {
        LocalDate today = LocalDate.now();
        return liabilityRepository.findByStatus(companyId, PayrollLiabilityStatus.POR_ENTREGAR)
                .stream()
                .filter(l -> l.getAmount().signum() > 0)
                .filter(l -> {
                    Long days = l.daysUntilDue(today);
                    return days == null || days <= dueAlertDays;
                })
                .map(PayrollLiabilityQueryService::toDTO)
                .toList();
    }

    public PayrollCostDTO monthlyCost(Long companyId, int year, int month) {
        List<Payslip> payslips = payslipRepository
                .findByCompanyIdAndYearAndMonth(companyId, year, month).stream()
                .filter(p -> !"CANCELLED".equals(p.getStatus()))
                .toList();

        List<PayrollCostDTO.PayrollCostLineDTO> lines = payslips.stream().map(p -> {
            BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
            return new PayrollCostDTO.PayrollCostLineDTO(
                    p.getEmployee().getId(), p.getEmployee().getEmployeeNumber(),
                    p.getEmployee().getName(), p.getBaseSalary(), p.getAllowances(), p.getOvertime(),
                    gross, p.getEmployerInss(), gross.add(p.getEmployerInss()), p.getNetPay());
        }).toList();

        BigDecimal gross = lines.stream().map(PayrollCostDTO.PayrollCostLineDTO::grossPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal employerInss = lines.stream().map(PayrollCostDTO.PayrollCostLineDTO::employerInss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = lines.stream().map(PayrollCostDTO.PayrollCostLineDTO::netPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayrollCostDTO(year, month, gross, employerInss, gross.add(employerInss), net, lines);
    }

    public static PayrollLiabilityDTO toDTO(PayrollLiability l) {
        LocalDate today = LocalDate.now();
        return new PayrollLiabilityDTO(
                l.getId(), l.getYear(), l.getMonth(),
                l.getLiabilityType().name(), l.getLiabilityType().getLabel(),
                l.getAmount(), l.getDueDate(), l.daysUntilDue(today), l.isOverdue(today),
                l.getStatus().name(), l.getStatus().getLabel(),
                l.getPaymentDate(), l.getPaymentReference(), l.getDeliveredBy());
    }
}
