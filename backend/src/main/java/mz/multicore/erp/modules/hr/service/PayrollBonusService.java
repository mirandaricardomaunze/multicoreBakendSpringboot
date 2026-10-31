package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.ThirteenthMonthDTO;
import mz.multicore.erp.modules.hr.dto.VacationAllowanceDTO;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayrollBonusRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apuramento e pagamento de subsídios legais da folha moçambicana — 13.º mês (subsídio de Natal)
 * proporcional ao tempo de serviço no ano e subsídio de férias por pedido aprovado.
 */
@Service
public class PayrollBonusService {

    private final PayrollBonusCalculationService calculationService;
    private final PayrollBonusPaymentService paymentService;

    @Autowired
    public PayrollBonusService(PayrollBonusCalculationService calculationService,
                               PayrollBonusPaymentService paymentService) {
        this.calculationService = calculationService;
        this.paymentService = paymentService;
    }

    public PayrollBonusService(
            EmployeeRepository employeeRepository,
            VacationRepository vacationRepository,
            PayrollBonusRepository bonusRepository,
            AuditLogService auditLogService,
            @Lazy FinanceService financeService
    ) {
        PayrollBonusCalculationService calc = new PayrollBonusCalculationService(
                employeeRepository, vacationRepository);
        this.calculationService = calc;
        this.paymentService = new PayrollBonusPaymentService(
                employeeRepository, bonusRepository, auditLogService, financeService, calc);
    }

    @Transactional(readOnly = true)
    public ThirteenthMonthDTO thirteenthMonth(int year) {
        return calculationService.thirteenthMonth(year);
    }

    @Transactional(readOnly = true)
    public VacationAllowanceDTO vacationAllowance(Long vacationId) {
        return calculationService.vacationAllowance(vacationId);
    }

    @Transactional
    public ThirteenthMonthDTO payThirteenthMonth(int year) {
        PermissionGuard.requireManagerOrAdmin("pagar subsídios");
        return paymentService.payThirteenthMonth(year);
    }

    @Transactional
    public VacationAllowanceDTO payVacationAllowance(Long vacationId) {
        PermissionGuard.requireManagerOrAdmin("pagar subsídios");
        return paymentService.payVacationAllowance(vacationId);
    }
}
