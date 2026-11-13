package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.service.EmployeeLookup;
import mz.multicore.erp.modules.quality.dto.*;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Orquestrador executivo para gestão de equipamentos, calibrações, EPIs e conformidade de qualidade.
 * Delega para colaboradores especializados respeitando os limites físicos de linhas.
 */
@Service
public class QualityEquipmentService {

    private final EquipmentCatalogService catalogService;
    private final WorkerEquipmentAssignmentService assignmentService;
    private final QualityMonitoringService monitoringService;

    @Autowired
    public QualityEquipmentService(
            EquipmentCatalogService catalogService,
            WorkerEquipmentAssignmentService assignmentService,
            QualityMonitoringService monitoringService
    ) {
        this.catalogService = catalogService;
        this.assignmentService = assignmentService;
        this.monitoringService = monitoringService;
    }

    public QualityEquipmentService(
            EquipmentRepository equipmentRepository,
            WorkerEquipmentAssignmentRepository assignmentRepository,
            EmployeeLookup employeeLookup,
            CompanyService companyService
    ) {
        this.catalogService = new EquipmentCatalogService(equipmentRepository, companyService);
        this.assignmentService = new WorkerEquipmentAssignmentService(
                equipmentRepository, assignmentRepository, employeeLookup, companyService);
        this.monitoringService = new QualityMonitoringService(equipmentRepository, assignmentRepository);
    }

    // ─── Equipamentos ────────────────────────────────────────────────────────

    @Transactional
    public EquipmentDTO createEquipment(EquipmentCreateRequest req) {
        return catalogService.createEquipment(req);
    }

    @Transactional
    public EquipmentDTO updateEquipment(Long id, EquipmentCreateRequest req) {
        return catalogService.updateEquipment(id, req);
    }

    @Transactional(readOnly = true)
    public EquipmentDTO getEquipment(Long id, Long companyId) {
        return catalogService.getEquipment(id, companyId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentDTO> listActiveEquipments(Long companyId) {
        return catalogService.listActiveEquipments(companyId);
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentDTO> searchEquipments(Long companyId, String search, int page, int size) {
        return catalogService.searchEquipments(companyId, search, page, size);
    }

    // ─── Atribuição a Trabalhadores ──────────────────────────────────────────

    @Transactional
    public WorkerEquipmentAssignmentDTO assignEquipment(EquipmentAssignmentRequest req) {
        return assignmentService.assignEquipment(req);
    }

    @Transactional
    public WorkerEquipmentAssignmentDTO renewAssignment(Long assignmentId, Long companyId, LocalDate newAssignedDate, String notes) {
        return assignmentService.renewAssignment(assignmentId, companyId, newAssignedDate, notes);
    }

    @Transactional
    public void returnAssignment(Long assignmentId, Long companyId, String notes) {
        assignmentService.returnAssignment(assignmentId, companyId, notes);
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
        return assignmentService.searchAssignments(companyId, status, employeeId, search, page, size);
    }

    // ─── Radar de Colaboradores Expirados e Alertas ──────────────────────────

    @Transactional(readOnly = true)
    public List<ExpiredWorkerAlertDTO> getExpiredWorkerAlerts(Long companyId) {
        return monitoringService.getExpiredWorkerAlerts(companyId);
    }

    @Transactional(readOnly = true)
    public QualityKpiDTO getQualityKpis(Long companyId) {
        return monitoringService.getQualityKpis(companyId);
    }

    public static Long resolveCompanyId(Long requestedCompanyId) {
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
