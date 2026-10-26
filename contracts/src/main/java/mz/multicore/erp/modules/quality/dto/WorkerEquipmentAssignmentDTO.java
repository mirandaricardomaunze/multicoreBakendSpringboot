package mz.multicore.erp.modules.quality.dto;

import java.time.LocalDate;

public record WorkerEquipmentAssignmentDTO(
        Long id,
        Long companyId,
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        EquipmentCategory equipmentCategory,
        String equipmentCategoryLabel,
        Long employeeId,
        String employeeName,
        String employeeDepartment,
        String employeeRole,
        LocalDate assignedDate,
        LocalDate expiryDate,
        long daysUntilExpiry,
        int quantity,
        String sizeOrSpec,
        String serialNumber,
        AssignmentStatus status,
        String statusLabel,
        String notes
) {
    public WorkerEquipmentAssignmentDTO(
            Long id,
            Long companyId,
            Long equipmentId,
            String equipmentCode,
            String equipmentName,
            Long employeeId,
            String employeeName,
            LocalDate assignedDate,
            LocalDate expiryDate,
            long daysUntilExpiry,
            AssignmentStatus status
    ) {
        this(
                id,
                companyId,
                equipmentId,
                equipmentCode,
                equipmentName,
                EquipmentCategory.PPE_PROTECTION,
                EquipmentCategory.PPE_PROTECTION.getLabel(),
                employeeId,
                employeeName,
                "-",
                "-",
                assignedDate,
                expiryDate,
                daysUntilExpiry,
                1,
                "-",
                null,
                status,
                status != null ? status.getLabel() : "-",
                null
        );
    }
}
