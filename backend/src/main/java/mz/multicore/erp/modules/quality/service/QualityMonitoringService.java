package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.quality.dto.AssignmentStatus;
import mz.multicore.erp.modules.quality.dto.ExpiredWorkerAlertDTO;
import mz.multicore.erp.modules.quality.dto.QualityKpiDTO;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Radar de vigilância de conformidade, alertas de EPIs/equipamentos vencidos e apuração de KPIs de qualidade.
 */
@Service
public class QualityMonitoringService {

    private final EquipmentRepository equipmentRepository;
    private final WorkerEquipmentAssignmentRepository assignmentRepository;

    public QualityMonitoringService(
            EquipmentRepository equipmentRepository,
            WorkerEquipmentAssignmentRepository assignmentRepository
    ) {
        this.equipmentRepository = equipmentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpiredWorkerAlertDTO> getExpiredWorkerAlerts(Long companyId) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        LocalDate today = LocalDate.now();
        List<WorkerEquipmentAssignment> expiredList = assignmentRepository.findExpiredActiveAssignments(resolvedCompanyId, today);
        List<WorkerEquipmentAssignment> expiringList = assignmentRepository.findExpiringSoonAssignments(
                resolvedCompanyId, today, today.plusDays(30)
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
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        LocalDate today = LocalDate.now();
        long activeCount = assignmentRepository.countActiveAssignments(resolvedCompanyId);
        long expiredWorkers = assignmentRepository.countDistinctEmployeesWithExpiredEquipment(resolvedCompanyId, today);
        List<WorkerEquipmentAssignment> expiredItems = assignmentRepository.findExpiredActiveAssignments(resolvedCompanyId, today);
        List<WorkerEquipmentAssignment> expiringItems = assignmentRepository.findExpiringSoonAssignments(
                resolvedCompanyId, today, today.plusDays(30)
        );
        long totalEquipments = equipmentRepository.countByCompanyId(resolvedCompanyId);

        return new QualityKpiDTO(
                activeCount,
                expiringItems.size(),
                expiredItems.size(),
                expiredWorkers,
                totalEquipments
        );
    }
}
