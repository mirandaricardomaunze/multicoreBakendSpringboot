package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escrita de colaboradores e recibos pedida por outros módulos (seeders, prémios de metas de vendas), sem
 * expor os repositórios do RH (docs/MODULE_DECOUPLING_SPEC.md §2). Leituras passam por {@link EmployeeLookup}.
 */
@Service
@Transactional
public class EmployeeCommandService {

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;

    public EmployeeCommandService(EmployeeRepository employeeRepository, PayslipRepository payslipRepository) {
        this.employeeRepository = employeeRepository;
        this.payslipRepository = payslipRepository;
    }

    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Payslip savePayslip(Payslip payslip) {
        return payslipRepository.save(payslip);
    }
}
