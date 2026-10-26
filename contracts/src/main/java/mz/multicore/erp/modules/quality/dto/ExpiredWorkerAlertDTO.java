package mz.multicore.erp.modules.quality.dto;

import java.time.LocalDate;
import java.util.List;

public record ExpiredWorkerAlertDTO(
        Long employeeId,
        String employeeName,
        String employeeDepartment,
        String employeeRole,
        int totalExpiredItems,
        int totalExpiringSoonItems,
        List<ExpiredEquipmentItemDTO> items
) {
    public record ExpiredEquipmentItemDTO(
            Long assignmentId,
            Long equipmentId,
            String equipmentCode,
            String equipmentName,
            LocalDate assignedDate,
            LocalDate expiryDate,
            long daysOverdueOrRemaining,
            AssignmentStatus status,
            String statusLabel
    ) {}
}
