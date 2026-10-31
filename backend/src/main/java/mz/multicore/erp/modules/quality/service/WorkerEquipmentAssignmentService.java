package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.quality.dto.AssignmentStatus;
import mz.multicore.erp.modules.quality.dto.EquipmentAssignmentRequest;
import mz.multicore.erp.modules.quality.dto.WorkerEquipmentAssignmentDTO;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestão de atribuições de equipamentos e EPIs a colaboradores, renovações e devoluções.
 */
@Service
public class WorkerEquipmentAssignmentService {

    private final EquipmentRepository equipmentRepository;
    private final WorkerEquipmentAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyService companyService;

    public WorkerEquipmentAssignmentService(
            EquipmentRepository equipmentRepository,
            WorkerEquipmentAssignmentRepository assignmentRepository,
            EmployeeRepository employeeRepository,
            CompanyService companyService
    ) {
        this.equipmentRepository = equipmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.companyService = companyService;
    }

    @Transactional
    public WorkerEquipmentAssignmentDTO assignEquipment(EquipmentAssignmentRequest req) {
        if (req == null || req.companyId() == null) {
            throw new BusinessRuleException("Empresa obrigatória para atribuir equipamento.");
        }
        if (req.equipmentId() == null) {
            throw new BusinessRuleException("Selecione o equipamento a atribuir.");
        }
        if (req.employeeId() == null) {
            throw new BusinessRuleException("Selecione o colaborador que receberá o equipamento.");
        }
        if (req.quantity() <= 0) {
            throw new BusinessRuleException("A quantidade deve ser superior a zero.");
        }
        Long companyId = QualityEquipmentService.resolveCompanyId(req.companyId());

        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        Equipment equipment = equipmentRepository.findByIdAndCompanyId(req.equipmentId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Equipamento não encontrado."));

        Employee employee = employeeRepository.findByIdAndCompanyId(req.employeeId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado nesta empresa."));

        LocalDate assignedDate = req.assignedDate() != null ? req.assignedDate() : LocalDate.now();
        LocalDate expiryDate = WorkerEquipmentAssignment.calculateExpiryDate(
                assignedDate, equipment.getPeriodValue(), equipment.getPeriodUnit()
        );

        WorkerEquipmentAssignment assignment = new WorkerEquipmentAssignment();
        assignment.setCompany(company);
        assignment.setEquipment(equipment);
        assignment.setEmployee(employee);
        assignment.setAssignedDate(assignedDate);
        assignment.setExpiryDate(expiryDate);
        assignment.setQuantity(req.quantity());
        assignment.setSizeOrSpec(req.sizeOrSpec());
        assignment.setSerialNumber(req.serialNumber());
        assignment.setNotes(req.notes());
        assignment.setStatus(assignment.computeCurrentStatus(LocalDate.now()));

        WorkerEquipmentAssignment saved = assignmentRepository.save(assignment);
        return mapToAssignmentDTO(saved, LocalDate.now());
    }

    @Transactional
    public WorkerEquipmentAssignmentDTO renewAssignment(Long assignmentId, Long companyId, LocalDate newAssignedDate, String notes) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        WorkerEquipmentAssignment current = assignmentRepository.findByIdAndCompanyId(assignmentId, resolvedCompanyId)
                .orElseThrow(() -> new BusinessRuleException("Atribuição não encontrada."));

        current.setStatus(AssignmentStatus.RENEWED);
        assignmentRepository.save(current);

        LocalDate deliveryDate = newAssignedDate != null ? newAssignedDate : LocalDate.now();
        EquipmentAssignmentRequest newReq = new EquipmentAssignmentRequest(
                resolvedCompanyId,
                current.getEquipment().getId(),
                current.getEmployee().getId(),
                deliveryDate,
                current.getQuantity(),
                current.getSizeOrSpec(),
                current.getSerialNumber(),
                notes != null && !notes.isBlank() ? notes : "Renovação periódica do equipamento #" + current.getId()
        );
        return assignEquipment(newReq);
    }

    @Transactional
    public void returnAssignment(Long assignmentId, Long companyId, String notes) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        WorkerEquipmentAssignment current = assignmentRepository.findByIdAndCompanyId(assignmentId, resolvedCompanyId)
                .orElseThrow(() -> new BusinessRuleException("Atribuição não encontrada."));

        current.setStatus(AssignmentStatus.RETURNED);
        if (notes != null && !notes.isBlank()) {
            current.setNotes((current.getNotes() != null ? current.getNotes() + " | " : "") + notes);
        }
        assignmentRepository.save(current);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkerEquipmentAssignmentDTO> searchAssignments(
            Long companyId,
            AssignmentStatus status,
            Long employeeId,
            String search,
            int page,
            int size
    ) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 25 : Math.min(200, size);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("assignedDate").descending());

        String cleanSearch = search != null && !search.isBlank() ? search.trim() : null;
        Page<WorkerEquipmentAssignment> result = assignmentRepository.searchAssignments(
                resolvedCompanyId, status, employeeId, cleanSearch, pageable
        );

        LocalDate today = LocalDate.now();
        List<WorkerEquipmentAssignmentDTO> dtoList = result.getContent().stream()
                .map(a -> mapToAssignmentDTO(a, today))
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtoList,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public WorkerEquipmentAssignmentDTO mapToAssignmentDTO(WorkerEquipmentAssignment a, LocalDate today) {
        AssignmentStatus dynamicStatus = a.computeCurrentStatus(today);
        long days = a.calculateDaysUntilExpiry(today);
        return new WorkerEquipmentAssignmentDTO(
                a.getId(),
                a.getCompany().getId(),
                a.getEquipment().getId(),
                a.getEquipment().getCode(),
                a.getEquipment().getName(),
                a.getEquipment().getCategory(),
                a.getEquipment().getCategory() != null ? a.getEquipment().getCategory().getLabel() : "-",
                a.getEmployee().getId(),
                a.getEmployee().getName(),
                a.getEmployee().getDepartment() != null ? a.getEmployee().getDepartment() : "-",
                "-",
                a.getAssignedDate(),
                a.getExpiryDate(),
                days,
                a.getQuantity(),
                a.getSizeOrSpec() != null ? a.getSizeOrSpec() : "-",
                a.getSerialNumber(),
                dynamicStatus,
                dynamicStatus.getLabel(),
                a.getNotes()
        );
    }
}
