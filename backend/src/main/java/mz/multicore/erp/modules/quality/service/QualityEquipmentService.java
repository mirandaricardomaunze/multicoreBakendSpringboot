package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.quality.dto.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QualityEquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final WorkerEquipmentAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyService companyService;

    public QualityEquipmentService(
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

    // ─── Equipamentos ────────────────────────────────────────────────────────

    @Transactional
    public EquipmentDTO createEquipment(EquipmentCreateRequest req) {
        if (req == null || req.companyId() == null) {
            throw new BusinessRuleException("Empresa obrigatória para cadastrar equipamento.");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new BusinessRuleException("O nome do equipamento é obrigatório.");
        }
        if (req.code() == null || req.code().isBlank()) {
            throw new BusinessRuleException("O código do equipamento é obrigatório.");
        }
        if (req.periodValue() <= 0) {
            throw new BusinessRuleException("O prazo de validade do equipamento deve ser superior a zero.");
        }
        Long companyId = resolveCompanyId(req.companyId());

        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        if (equipmentRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, req.code().trim())) {
            throw new BusinessRuleException("Já existe um equipamento cadastrado com o código: " + req.code());
        }

        Equipment eq = new Equipment();
        eq.setCompany(company);
        eq.setCode(req.code().trim().toUpperCase());
        eq.setName(req.name().trim());
        eq.setCategory(req.category() != null ? req.category() : EquipmentCategory.PPE_PROTECTION);
        eq.setPeriodValue(req.periodValue());
        eq.setPeriodUnit(req.periodUnit() != null ? req.periodUnit() : PeriodUnit.MONTHS);
        eq.setWarningDaysBefore(req.warningDaysBefore() > 0 ? req.warningDaysBefore() : 15);
        eq.setActive(req.active());
        eq.setDescription(req.description());

        Equipment saved = equipmentRepository.save(eq);
        return mapToEquipmentDTO(saved);
    }

    @Transactional
    public EquipmentDTO updateEquipment(Long id, EquipmentCreateRequest req) {
        if (id == null || req == null || req.companyId() == null) {
            throw new BusinessRuleException("Identificador e empresa obrigatórios.");
        }
        if (req.name() == null || req.name().isBlank() || req.code() == null || req.code().isBlank()) {
            throw new BusinessRuleException("O código e o nome do equipamento são obrigatórios.");
        }
        if (req.periodValue() <= 0) {
            throw new BusinessRuleException("O prazo de validade do equipamento deve ser superior a zero.");
        }
        Long companyId = resolveCompanyId(req.companyId());
        Equipment eq = equipmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Equipamento não encontrado."));

        if (!eq.getCode().equalsIgnoreCase(req.code().trim())
                && equipmentRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, req.code().trim())) {
            throw new BusinessRuleException("Já existe um equipamento com o código: " + req.code());
        }

        eq.setCode(req.code().trim().toUpperCase());
        eq.setName(req.name().trim());
        eq.setCategory(req.category() != null ? req.category() : EquipmentCategory.PPE_PROTECTION);
        eq.setPeriodValue(req.periodValue());
        eq.setPeriodUnit(req.periodUnit() != null ? req.periodUnit() : PeriodUnit.MONTHS);
        eq.setWarningDaysBefore(req.warningDaysBefore() > 0 ? req.warningDaysBefore() : 15);
        eq.setActive(req.active());
        eq.setDescription(req.description());

        Equipment saved = equipmentRepository.save(eq);
        return mapToEquipmentDTO(saved);
    }

    @Transactional(readOnly = true)
    public EquipmentDTO getEquipment(Long id, Long companyId) {
        companyId = resolveCompanyId(companyId);
        return equipmentRepository.findByIdAndCompanyId(id, companyId)
                .map(this::mapToEquipmentDTO)
                .orElseThrow(() -> new BusinessRuleException("Equipamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<EquipmentDTO> listActiveEquipments(Long companyId) {
        companyId = resolveCompanyId(companyId);
        return equipmentRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId)
                .stream()
                .map(this::mapToEquipmentDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentDTO> searchEquipments(Long companyId, String search, int page, int size) {
        companyId = resolveCompanyId(companyId);
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 25 : Math.min(200, size);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());

        String cleanSearch = search != null && !search.isBlank() ? search.trim() : null;
        Page<Equipment> result = equipmentRepository.searchEquipments(companyId, cleanSearch, pageable);

        List<EquipmentDTO> dtoList = result.getContent().stream()
                .map(this::mapToEquipmentDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtoList,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ─── Atribuição a Trabalhadores ──────────────────────────────────────────

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
        Long companyId = resolveCompanyId(req.companyId());

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
        companyId = resolveCompanyId(companyId);
        WorkerEquipmentAssignment current = assignmentRepository.findByIdAndCompanyId(assignmentId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Atribuição não encontrada."));

        current.setStatus(AssignmentStatus.RENEWED);
        assignmentRepository.save(current);

        LocalDate deliveryDate = newAssignedDate != null ? newAssignedDate : LocalDate.now();
        EquipmentAssignmentRequest newReq = new EquipmentAssignmentRequest(
                companyId,
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
        companyId = resolveCompanyId(companyId);
        WorkerEquipmentAssignment current = assignmentRepository.findByIdAndCompanyId(assignmentId, companyId)
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
        companyId = resolveCompanyId(companyId);
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 25 : Math.min(200, size);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("assignedDate").descending());

        String cleanSearch = search != null && !search.isBlank() ? search.trim() : null;
        Page<WorkerEquipmentAssignment> result = assignmentRepository.searchAssignments(
                companyId, status, employeeId, cleanSearch, pageable
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

    // ─── Radar de Colaboradores Expirados e Alertas ──────────────────────────

    @Transactional(readOnly = true)
    public List<ExpiredWorkerAlertDTO> getExpiredWorkerAlerts(Long companyId) {
        companyId = resolveCompanyId(companyId);
        LocalDate today = LocalDate.now();
        List<WorkerEquipmentAssignment> expiredList = assignmentRepository.findExpiredActiveAssignments(companyId, today);
        List<WorkerEquipmentAssignment> expiringList = assignmentRepository.findExpiringSoonAssignments(
                companyId, today, today.plusDays(30)
        );

        Map<Long, List<WorkerEquipmentAssignment>> byEmployee = new LinkedHashMap<>();
        for (WorkerEquipmentAssignment a : expiredList) {
            byEmployee.computeIfAbsent(a.getEmployee().getId(), k -> new ArrayList<>()).add(a);
        }
        for (WorkerEquipmentAssignment a : expiringList) {
            byEmployee.computeIfAbsent(a.getEmployee().getId(), k -> new ArrayList<>()).add(a);
        }

        List<ExpiredWorkerAlertDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<WorkerEquipmentAssignment>> entry : byEmployee.entrySet()) {
            List<WorkerEquipmentAssignment> list = entry.getValue();
            if (list.isEmpty()) continue;

            Employee emp = list.get(0).getEmployee();
            int expiredCount = 0;
            int expiringSoonCount = 0;
            List<ExpiredWorkerAlertDTO.ExpiredEquipmentItemDTO> items = new ArrayList<>();

            for (WorkerEquipmentAssignment a : list) {
                AssignmentStatus dynamicStatus = a.computeCurrentStatus(today);
                long days = a.calculateDaysUntilExpiry(today);
                if (dynamicStatus == AssignmentStatus.EXPIRED) {
                    expiredCount++;
                } else if (dynamicStatus == AssignmentStatus.EXPIRING_SOON) {
                    expiringSoonCount++;
                }
                items.add(new ExpiredWorkerAlertDTO.ExpiredEquipmentItemDTO(
                        a.getId(),
                        a.getEquipment().getId(),
                        a.getEquipment().getCode(),
                        a.getEquipment().getName(),
                        a.getAssignedDate(),
                        a.getExpiryDate(),
                        days,
                        dynamicStatus,
                        dynamicStatus.getLabel()
                ));
            }

            result.add(new ExpiredWorkerAlertDTO(
                    emp.getId(),
                    emp.getName(),
                    emp.getDepartment() != null ? emp.getDepartment() : "Geral",
                    "—",
                    expiredCount,
                    expiringSoonCount,
                    items
            ));
        }

        result.sort(Comparator
                .comparingInt(ExpiredWorkerAlertDTO::totalExpiredItems).reversed()
                .thenComparing(ExpiredWorkerAlertDTO::employeeName));
        return result;
    }

    @Transactional(readOnly = true)
    public QualityKpiDTO getQualityKpis(Long companyId) {
        companyId = resolveCompanyId(companyId);
        LocalDate today = LocalDate.now();
        long activeCount = assignmentRepository.countActiveAssignments(companyId);
        long expiredWorkers = assignmentRepository.countDistinctEmployeesWithExpiredEquipment(companyId, today);
        List<WorkerEquipmentAssignment> expiredItems = assignmentRepository.findExpiredActiveAssignments(companyId, today);
        List<WorkerEquipmentAssignment> expiringItems = assignmentRepository.findExpiringSoonAssignments(
                companyId, today, today.plusDays(30)
        );
        long totalEquipments = equipmentRepository.countByCompanyId(companyId);

        return new QualityKpiDTO(
                activeCount,
                expiringItems.size(),
                expiredItems.size(),
                expiredWorkers,
                totalEquipments
        );
    }

    // ─── Helpers de Mapeamento ───────────────────────────────────────────────

    private EquipmentDTO mapToEquipmentDTO(Equipment e) {
        return new EquipmentDTO(
                e.getId(),
                e.getCompany().getId(),
                e.getCode(),
                e.getName(),
                e.getCategory(),
                e.getCategory() != null ? e.getCategory().getLabel() : "-",
                e.getPeriodValue(),
                e.getPeriodUnit(),
                e.getPeriodUnit() != null ? e.getPeriodUnit().getLabel() : "-",
                e.getWarningDaysBefore(),
                e.isActive(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }

    private WorkerEquipmentAssignmentDTO mapToAssignmentDTO(WorkerEquipmentAssignment a, LocalDate today) {
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

    private Long resolveCompanyId(Long requestedCompanyId) {
        if (requestedCompanyId == null) {
            throw new BusinessRuleException("Empresa obrigatória para continuar.");
        }
        Long authenticatedCompanyId = CurrentUserContext.findCurrentCompanyId();
        if (authenticatedCompanyId != null && !authenticatedCompanyId.equals(requestedCompanyId)) {
            throw new BusinessRuleException("Operação recusada: os dados pertencem a outra empresa.");
        }
        return authenticatedCompanyId != null ? authenticatedCompanyId : requestedCompanyId;
    }
}
