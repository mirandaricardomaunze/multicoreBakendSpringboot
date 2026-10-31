package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SettlementStatus;
import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.model.TerminationSettlementLine;
import mz.multicore.erp.modules.hr.repository.TerminationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TerminationQueryService {
    private final TerminationRepository terminationRepository;

    public TerminationQueryService(TerminationRepository terminationRepository) {
        this.terminationRepository = terminationRepository;
    }

    @Transactional(readOnly = true)
    public List<TerminationDTO> list() {
        return terminationRepository.findAllByCompany(CurrentUserContext.requireCurrentCompanyId()).stream()
                .map(t -> toDTO(t, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public Termination loadForPrint(Long id) {
        return terminationRepository.findByIdWithLines(id, CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Acerto final não encontrado."));
    }

    public static TerminationDTO previewDTO(Employee employee,
                                            TerminationSettlementCalculator.Settlement settlement,
                                            CreateTerminationRequest request) {
        BigDecimal earnings = settlement.lines().stream().filter(TerminationSettlementLine::isEarning)
                .map(TerminationSettlementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductions = settlement.lines().stream().filter(l -> !l.isEarning())
                .map(TerminationSettlementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TerminationDTO(null, null, employee.getId(), employee.getName(),
                settlement.contract() == null ? null : settlement.contract().getId(),
                settlement.contract() == null ? null : settlement.contract().getContractNumber(),
                request.terminationDate(), settlement.reason().name(), settlement.reason().getLabel(),
                request.noticeServed(), earnings, deductions, earnings.subtract(deductions),
                SettlementStatus.POR_PAGAR.name(), SettlementStatus.POR_PAGAR.getLabel(), null,
                blankToNull(request.notes()),
                settlement.lines().stream().map(TerminationQueryService::lineDTO).toList(),
                settlement.warnings());
    }

    public static TerminationDTO toDTO(Termination t, List<String> warnings) {
        return new TerminationDTO(t.getId(), t.getSettlementNumber(), t.getEmployee().getId(),
                t.getEmployee().getName(),
                t.getContract() == null ? null : t.getContract().getId(),
                t.getContract() == null ? null : t.getContract().getContractNumber(),
                t.getTerminationDate(), t.getReason().name(), t.getReason().getLabel(),
                t.isNoticeServed(), t.getTotalEarnings(), t.getTotalDeductions(), t.getNetAmount(),
                t.getStatus().name(), t.getStatus().getLabel(), t.getPaymentDate(), t.getNotes(),
                t.getLines().stream().map(TerminationQueryService::lineDTO).toList(), warnings);
    }

    public static TerminationDTO.TerminationLineDTO lineDTO(TerminationSettlementLine line) {
        return new TerminationDTO.TerminationLineDTO(
                line.getDescription(), line.getAmount(), line.isEarning());
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
