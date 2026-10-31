package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.model.ContractStatus;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gestor de consultas analíticas, alertas de vencimento e projeção DTO de contratos de trabalho.
 */
@Service
public class ContractQueryService {

    private final EmploymentContractRepository contractRepository;

    @Autowired
    public ContractQueryService(EmploymentContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<EmploymentContractDTO> getAllContracts() {
        return contractRepository.findAllWithEmployeeByCompanyId(currentCompanyId()).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EmploymentContractDTO> getContractsOfEmployee(Long employeeId) {
        return contractRepository.findByEmployeeId(employeeId, currentCompanyId()).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmploymentContract> findContractInPeriod(Long employeeId, LocalDate from, LocalDate to) {
        return contractRepository.findOverlapping(employeeId, currentCompanyId(), from, to, -1L)
                .stream().findFirst();
    }

    @Transactional(readOnly = true)
    public ContractAlertsDTO getContractAlerts(int endingDays, int probationDays) {
        LocalDate today = LocalDate.now();
        Long companyId = currentCompanyId();
        List<EmploymentContractDTO> ending = contractRepository.findEndingBetween(
                        companyId, ContractStatus.VIGENTE, today, today.plusDays(endingDays))
                .stream().map(this::toDTO).toList();
        List<EmploymentContractDTO> probation = contractRepository.findProbationEndingBetween(
                        companyId, ContractStatus.VIGENTE, today, today.plusDays(probationDays))
                .stream().map(this::toDTO).toList();
        return new ContractAlertsDTO(ending, probation);
    }

    public EmploymentContractDTO toDTO(EmploymentContract c) {
        if (c == null) return null;
        LocalDate today = LocalDate.now();
        return new EmploymentContractDTO(
                c.getId(), c.getContractNumber(), c.getEmployee().getId(), c.getEmployee().getName(),
                c.getContractType().name(), c.getContractType().getLabel(),
                c.getStatus().name(), c.getStatus().getLabel(),
                c.getStartDate(), c.getEndDate(), c.getProbationEndDate(),
                c.getAgreedSalary(), c.getWeeklyHours(), c.getJobTitle(), c.getWorkLocation(),
                c.getTermReason(),
                c.getRenewedFrom() == null ? null : c.getRenewedFrom().getId(),
                c.getRenewedFrom() == null ? null : c.getRenewedFrom().getContractNumber(),
                c.getTerminationDate(), c.getTerminationReason(),
                c.isExpired(today), c.isInProbation(today), c.daysUntilEnd(today)
        );
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }
}
