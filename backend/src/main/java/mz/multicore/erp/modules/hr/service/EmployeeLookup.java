package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de colaboradores do RH para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Recibos de vencimento e contratos de trabalho, pouco lidos fora do RH, ficam aqui por serem do agregado
 * mais próximo. Escritas alheias passam por {@link EmployeeCommandService}. As entidades devolvidas servem
 * só para relações JPA dentro do backend e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeLookup {

    static final String NOT_FOUND = "Colaborador não encontrado.";
    static final String NOT_FOUND_IN_COMPANY = "Colaborador não encontrado nesta empresa.";
    static final String CONTRACT_NOT_FOUND = "Contrato não encontrado.";

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final EmploymentContractRepository contractRepository;

    public EmployeeLookup(EmployeeRepository employeeRepository,
                          PayslipRepository payslipRepository,
                          EmploymentContractRepository contractRepository) {
        this.employeeRepository = employeeRepository;
        this.payslipRepository = payslipRepository;
        this.contractRepository = contractRepository;
    }

    /** Colaborador pelo id; {@code BusinessRuleException("Colaborador não encontrado.")} se não existir. */
    public Employee require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Employee> find(Long id) {
        return id == null ? Optional.empty() : employeeRepository.findById(id);
    }

    /** Colaborador da empresa; {@code BusinessRuleException("Colaborador não encontrado nesta empresa.")}. */
    public Employee requireForCompany(Long id, Long companyId) {
        return findForCompany(id, companyId).orElseThrow(() -> new BusinessRuleException(NOT_FOUND_IN_COMPANY));
    }

    public Optional<Employee> findForCompany(Long id, Long companyId) {
        return id == null ? Optional.empty() : employeeRepository.findByIdAndCompanyId(id, companyId);
    }

    /** Colaboradores da empresa, ordenados pelo nome. */
    public List<Employee> listForCompany(Long companyId) {
        return employeeRepository.findByCompanyIdOrderByName(companyId);
    }

    /** Colaboradores da empresa no estado indicado (ex.: {@code "ACTIVE"}). */
    public List<Employee> listByStatusForCompany(Long companyId, String status) {
        return employeeRepository.findByCompanyIdAndStatus(companyId, status);
    }

    /** Recibo de vencimento do colaborador no período (ano/mês), se já emitido. */
    public Optional<Payslip> findPayslip(Long employeeId, int year, int month) {
        return payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month);
    }

    /** Contrato de trabalho da empresa com o colaborador carregado; "Contrato não encontrado." se não existir. */
    public EmploymentContract requireContractForCompany(Long contractId, Long companyId) {
        return contractRepository.findByIdWithEmployeeAndCompanyId(contractId, companyId)
                .orElseThrow(() -> new BusinessRuleException(CONTRACT_NOT_FOUND));
    }
}
